package de.dkrz.handlereverselookupservlet;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;

/**
 * A singleton.
 * 
 */
public class ReverseLookupConfig {
	
	private static ReverseLookupConfig instance = null;
	
	private String jdbcDriverClassName;
	private String sqlConnectionString;
	private String sqlUsername;
	private String sqlPassword;
	private DataSource handleDataSource;
	
	private static String getParam(ServletContext sc, Map<Object, Object> additionalProperties, Object key, boolean requiredParam) throws InvalidConfigException {
		String s = sc.getInitParameter(key.toString());
		if (s == null) {
			Object x = additionalProperties.get(key);
			if (x != null) s = x.toString();
		}
		if (requiredParam && (s == null)) {
			throw new InvalidConfigException("The configuration must specify parameter '"+key+"'!");
		}
		return s;
	}

	public ReverseLookupConfig(ServletContext servletContext, Map<Object, Object> additionalProperties) throws InvalidConfigException  {
		super();
		if (this.instance != null)
			throw new IllegalStateException("Cannot instantiate singleton more than once");
		this.jdbcDriverClassName = getParam(servletContext, additionalProperties, "jdbcDriverClassName", true);
		this.sqlConnectionString = getParam(servletContext, additionalProperties, "sqlConnectionString", true);
		this.sqlUsername = getParam(servletContext, additionalProperties, "sqlUsername", true);
		this.sqlPassword = getParam(servletContext, additionalProperties, "sqlPassword", true);
		this.instance = this;
	}
	
	public static ReverseLookupConfig getInstance() {
		return instance;
	}

	public String getJdbcDriverClassName() {
		return jdbcDriverClassName;
	}
	
	public DataSource getHandleDataSource() {
		return handleDataSource;
	}
	
	public void setHandleDataSource(DataSource handleDataSource) {
		this.handleDataSource = handleDataSource;
	}

	public void createHandleDataSource() throws ClassNotFoundException {
		DriverAdapterCPDS cpds = new DriverAdapterCPDS();
		cpds.setDriver(jdbcDriverClassName);
		cpds.setUrl(sqlConnectionString); 
		cpds.setUser(sqlUsername);
		cpds.setPassword(sqlPassword);
		SharedPoolDataSource sharedDS = new SharedPoolDataSource();
		sharedDS.setConnectionPoolDataSource(cpds);
		handleDataSource = sharedDS;
	}
	

}
