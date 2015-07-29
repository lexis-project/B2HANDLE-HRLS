package de.dkrz.handlereverselookupservlet;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.cpdsadapter.DriverAdapterCPDS;
import org.apache.commons.dbcp2.datasources.SharedPoolDataSource;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

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
	
	private boolean useSolr = false;
	private boolean useSql = false;
	
	private String solrCloudZkHost;
	private String solrCollection;
	
	private CloudSolrClient solrClient;
	
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
	
	private static boolean getBooleanParam(ServletContext sc, Map<Object, Object> additionalProperties, Object key, boolean requiredParam) throws InvalidConfigException {
		if (sc.getInitParameter(key.toString()) == null) {
			Object x = additionalProperties.get(key);
			if (x != null) return Boolean.parseBoolean(x.toString());
			else if (requiredParam) throw new InvalidConfigException("The configuration must specify parameter '"+key+"'!"); 
		}
		return Boolean.parseBoolean(sc.getInitParameter(key.toString()));
	}
	

	public ReverseLookupConfig(ServletContext servletContext, Map<Object, Object> additionalProperties) throws InvalidConfigException  {
		super();
		if (this.instance != null)
			throw new IllegalStateException("Cannot instantiate singleton more than once");
		this.useSql = getBooleanParam(servletContext, additionalProperties, "useSQL", false);
		this.useSolr = getBooleanParam(servletContext, additionalProperties, "useSolr", false);
		if (useSql) {
			this.jdbcDriverClassName = getParam(servletContext, additionalProperties, "jdbcDriverClassName", true);
			this.sqlConnectionString = getParam(servletContext, additionalProperties, "sqlConnectionString", true);
			this.sqlUsername = getParam(servletContext, additionalProperties, "sqlUsername", true);
			this.sqlPassword = getParam(servletContext, additionalProperties, "sqlPassword", true);
		}
		if (useSolr) {
			this.solrCloudZkHost = getParam(servletContext, additionalProperties, "solrCloudZkHost", true);
			this.solrCollection = getParam(servletContext, additionalProperties, "solrCollection", true);
		}
		if (!(this.useSql || this.useSolr)) throw new InvalidConfigException("The configuration must enable at least one of 'useSolr' or 'useSql'!");
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
		if (!useSql) 
			return;
		/*
		 * Load jdbc driver class - this is apparently NOT done automatically
		 * for some combinations of driver, tomcat and Java
		 */
		if ((getJdbcDriverClassName() != null) && (getJdbcDriverClassName().length() > 0))
			Class.forName(getJdbcDriverClassName());
		// Now configure pooled driver adapter and pooled data source
		DriverAdapterCPDS cpds = new DriverAdapterCPDS();
		cpds.setDriver(jdbcDriverClassName);
		cpds.setUrl(sqlConnectionString); 
		cpds.setUser(sqlUsername);
		cpds.setPassword(sqlPassword);
		SharedPoolDataSource sharedDS = new SharedPoolDataSource();
		sharedDS.setConnectionPoolDataSource(cpds);
		handleDataSource = sharedDS;
	}
	
	public void createSolrClient() {
		solrClient = new CloudSolrClient(solrCloudZkHost);
		solrClient.setDefaultCollection(solrCollection);
	}
	
	public CloudSolrClient getSolrClient() {
		return solrClient;
	}

	public boolean useSql() {
		return useSql;
	}
	
	public boolean useSolr() {
		return useSolr;
	}
	

}
