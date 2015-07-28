package de.dkrz.handlereverselookupservlet;

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
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
		try {
			List<String> result;
			if (params.containsKey("limit")) {
				MultivaluedMap<String, String> newParams = new MultivaluedHashMap<String, String>(params); 
				int limit = Integer.parseInt(newParams.getFirst("limit"));
				newParams.remove("limit");
				result = genericSearch(newParams, limit);
			}
			else {
				result = genericSearch(params, null);
			}
			return Response.ok(result, MediaType.APPLICATION_JSON).build();
		} catch (SQLException exc) {
			return Response.serverError()
					.entity("\"" + exc.getMessage() + " (SQL error code " + exc.getErrorCode() + ")\"\n").build();
		} catch (NumberFormatException exc) {
			return Response.serverError().entity("\"Invalid number: "+exc.getMessage()+"\"\n").build();
		}
	}

	/**
	 * Queries SQL for Handles whose type/data pairs match particular filters.
	 * 
	 * @param parameters
	 * @param limit SQL query limit. May be null.
	 * @return
	 * @throws SQLException
	 */
	public List<String> genericSearch(MultivaluedMap<String, String> parameters, Integer limit) throws SQLException {
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
				// Search for Handles with several type entries to be checked using multiple inner joins
				sb.append("select table_1.handle from ");
				int tableIndex = 1;
				for (String key : parameters.keySet()) {
					if (tableIndex > 1)
						sb.append(" inner join ");
					sb.append("(");
					makeSearchSubquery(key, parameters.get(key), sb, stringParams);
					sb.append(") table_"+tableIndex);
					if (tableIndex > 1)
						sb.append(" on table_"+(tableIndex-1)+".handle=table_"+tableIndex+".handle");
					tableIndex++;
				}
			}
			if (limit != null)
				sb.append(" limit "+limit);
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
			List<String> results = new LinkedList<String>();
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
		for (String value: list) {
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
