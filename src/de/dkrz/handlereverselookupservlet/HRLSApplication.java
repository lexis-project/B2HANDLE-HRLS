package de.dkrz.handlereverselookupservlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HRLSApplication extends Application {

	private static final Logger LOGGER = LogManager.getLogger(HRLSApplication.class);
	
	private Set<Object> singletons = new HashSet<Object>();

	private Set<Class<?>> emptySet = new HashSet<Class<?>>();

	public HRLSApplication(@Context ServletContext servletContext)
			throws ClassNotFoundException, FileNotFoundException, IOException, InvalidConfigException, SQLException {
		// Get config from context (singleton)
		Map<Object, Object> env = System.getProperties();
		// Search for simple config file in $HANDLE_SVR/handlereverselookupservlet.properties
		String handleHome = System.getenv("HANDLE_SVR");
		if (handleHome == null) {
			throw new InvalidConfigException("This servlet requires the HANDLE_SVR environmental variable." +
				"HANDLE_SRV should point to the Handle server instance's home directory, " +
				"where its configuration files are located (e.g. '/hs/svr_1').");
		} else {
			File configFile = new File(handleHome+"/handlereverselookupservlet.properties");
			if (configFile.exists()) {
				Properties configFileProps = new Properties();
				configFileProps.load(new FileInputStream(configFile));
				env.putAll(configFileProps);
			}
		}
		ReverseLookupConfig hrlsConfig = new ReverseLookupConfig(servletContext, env);
		// Create DataSource instance
		hrlsConfig.createHandleDataSource();
		hrlsConfig.createSolrClient();
		// Set up servlet
		singletons.add(new HandleReverseLookupResource());
	}

	@Override
	public Set<Class<?>> getClasses() {
		return emptySet;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}

}
