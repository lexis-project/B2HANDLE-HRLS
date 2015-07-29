package de.dkrz.handlereverselookupservlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient.RemoteSolrException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

@Path("/")
public class HandleReverseLookupResource {

	private static final Logger LOGGER = LogManager.getLogger(HandleReverseLookupResource.class);

	@GET
	@Path("ping")
	public String ping() {
		return "OK\n";
	}

	@GET
	@Path("handles")
	@Produces("application/json")
	public Response search(@Context UriInfo info) {
		MultivaluedMap<String, String> params = info.getQueryParameters();
		ReverseLookupConfig configuration = ReverseLookupConfig.getInstance();
		Integer limit = null;
		boolean enforceSql = false;
		MultivaluedMap<String, String> filteredParams = new MultivaluedHashMap<String, String>(params);
		try {
			if (filteredParams.containsKey("limit")) {
				limit = Integer.parseInt(filteredParams.getFirst("limit"));
				filteredParams.remove("limit");
			}
			if (filteredParams.containsKey("enforcesql")) {
				enforceSql = Boolean.parseBoolean(filteredParams.getFirst("enforcesql"));
				filteredParams.remove("enforcesql");
				if (enforceSql && !configuration.useSql())
					return Response.serverError()
							.entity("You asked to enforce SQL usage for searching, but this service is not configured for SQL.")
							.build();
			}
			List<String> result;
			// If available, search via solr takes precedence over SQL unless
			// enforced otherwise
			if (configuration.useSolr() && !enforceSql) {
				result = genericSolrSearch(filteredParams, limit);
			} else {
				result = genericSqlSearch(filteredParams, limit);
			}
			return Response.ok(result, MediaType.APPLICATION_JSON).build();
		} catch (SQLException exc) {
			LOGGER.error(exc);
			return Response.serverError()
					.entity("\"" + exc.getMessage() + " (SQL error code " + exc.getErrorCode() + ")\"\n").build();
		} catch (IOException exc) {
			LOGGER.error(exc);
			return Response.serverError().entity("\"IOException: " + exc.getMessage() + "\"\n").build();
		} catch (NumberFormatException exc) {
			LOGGER.error(exc);
			return Response.serverError().entity("\"Invalid number: " + exc.getMessage() + "\"\n").build();
		} catch (SolrServerException exc) {
			LOGGER.error(exc);
			return Response.serverError().entity("\"SolrServerException: " + exc.getMessage() + "\"\n").build();
		} catch (RemoteSolrException exc) {
			return Response.status(Response.Status.BAD_REQUEST).entity("\"RemoteSolrException: " + exc.getMessage() + "\"\n").build();
		}
	}

	public List<String> genericSolrSearch(MultivaluedMap<String, String> parameters, Integer limit)
			throws SolrServerException, IOException {
		List<String> results = new LinkedList<String>();
		if (parameters.isEmpty()) {
			return results;
		}
		ReverseLookupConfig configuration = ReverseLookupConfig.getInstance();
		CloudSolrClient solr = configuration.getSolrClient();
		try {
			SolrQuery query = new SolrQuery();
			if (limit == null)
				query.setRows(1000);
			else
				query.setRows(limit);
			for (String key : parameters.keySet()) {
				List<String> values = parameters.get(key);
				for (String v : values)
					query.add("q", key + ":" + escapeSolrQueryChars(v));
			}
			LOGGER.debug("Solr query: " + query);
			QueryResponse queryResponse = solr.query(query);
			SolrDocumentList docs = queryResponse.getResults();
			for (SolrDocument doc : docs) {
				results.add(doc.get("id").toString());
			}
		} finally {
		}
		return results;
	}

	private String escapeSolrQueryChars(String s) {
		// Taken from solrj source. Will do normal filtering except for
		// asterisks.
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// These characters are part of the query syntax and must be escaped
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^'
					|| c == '[' || c == ']' || c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|'
					|| c == '&' || c == ';' || c == '/' || Character.isWhitespace(c)) {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}

	/**
	 * Queries SQL for Handles whose type/data pairs match particular filters.
	 * 
	 * @param parameters
	 * @param limit
	 *            SQL query limit. May be null.
	 * @return
	 * @throws SQLException
	 */
	public List<String> genericSqlSearch(MultivaluedMap<String, String> parameters, Integer limit) throws SQLException {
		List<String> results = new LinkedList<String>();
		if (parameters.isEmpty()) {
			return results;
		}
		ReverseLookupConfig config = ReverseLookupConfig.getInstance();
		DataSource dataSource = config.getHandleDataSource();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = dataSource.getConnection();
			StringBuffer sb = new StringBuffer();
			List<String> stringParams = new LinkedList<String>();
			if (parameters.size() == 1) {
				// Simple query, no joins
				String key = parameters.keySet().iterator().next();
				makeSearchSubquery(key, parameters.get(key), sb, stringParams);
			} else {
				// Search for Handles with several type entries to be checked
				// using multiple inner joins
				sb.append("select table_1.handle from ");
				int tableIndex = 1;
				for (String key : parameters.keySet()) {
					if (tableIndex > 1)
						sb.append(" inner join ");
					sb.append("(");
					makeSearchSubquery(key, parameters.get(key), sb, stringParams);
					sb.append(") table_" + tableIndex);
					if (tableIndex > 1)
						sb.append(" on table_" + (tableIndex - 1) + ".handle=table_" + tableIndex + ".handle");
					tableIndex++;
				}
			}
			if (limit != null)
				sb.append(" limit " + limit);
			// Now fill statement with stringParams
			statement = connection.prepareStatement(sb.toString());
			Iterator<String> paramsIter = stringParams.iterator();
			int index = 1;
			while (paramsIter.hasNext()) {
				statement.setString(index, paramsIter.next());
				index++;
			}
			// Execute statement
			resultSet = statement.executeQuery();
			while (resultSet.next()) {
				results.add(resultSet.getString(1));
			}
			return results;
		} finally {
			if (resultSet != null) {
				try {
					resultSet.close();
				} catch (SQLException e) {
					// swallow
				}
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (SQLException e) {
					// swallow
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// swallow
				}
			}
		}
	}

	private void makeSearchSubquery(String key, List<String> list, StringBuffer sb, List<String> stringParams) {
		sb.append("select handle from handles where type=?");
		stringParams.add(key);
		for (String value : list) {
			String modvalue = value;
			if (modvalue.contains("*")) {
				modvalue = modvalue.replace("*", "%");
				sb.append(" and data like ?");
			} else {
				sb.append(" and data=?");
			}
			stringParams.add(modvalue);
		}
	}

}
