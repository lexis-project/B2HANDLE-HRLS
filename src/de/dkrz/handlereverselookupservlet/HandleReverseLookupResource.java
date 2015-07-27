package de.dkrz.handlereverselookupservlet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class HandleReverseLookupResource {
	
	@GET
	@Path("ping")
	public String ping() {
		return "OK";
	}
	
	@GET
	@Path("handles")
	@Produces("application/json")
	public Response search(@QueryParam("url") String url) throws SQLException {
		return Response.ok(genericSearch(url), MediaType.APPLICATION_JSON).build();
	}
	
	public List<String> genericSearch(@QueryParam("url") String url) throws SQLException {
		ReverseLookupConfig config = ReverseLookupConfig.getInstance();
		DataSource dataSource = config.getHandleDataSource();
		Connection connection = null;
		PreparedStatement statement = null;
		ResultSet resultSet = null;
		try {
			connection = dataSource.getConnection();
			statement = connection.prepareStatement("select handle from handles where type='URL' and data=?");
			statement.setString(1, url);
			resultSet = statement.executeQuery();
			String s = "";
			List<String> results = new LinkedList<String>();
			while (resultSet.next()) {
				results.add(resultSet.getString(1));
			}
			return results;
		}
		finally {
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

}
