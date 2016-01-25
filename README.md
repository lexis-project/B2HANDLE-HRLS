# B2HANDLE-HandleReverseLookupServlet

B2HANDLE-HRLS provides a Java servlet that will enable reverse-lookup and searching against local a Handle server installation with SQL storage.

## How to build

This is a Maven-enabled project. To build a .war file, install [Apache Maven](https://maven.apache.org) and then call:
```
$ mvn compile
$ mvn war:war
```
The .war file will be under subdirectory "target". 

## How to deploy (using embedded Handle System v8 Jetty)

To deploy the servlet .war in the embedded jetty, copy it to your-instance-directory/webapps.
The servlet also needs configuration; please refer to the Javadoc main page for more details.

Please make sure you are using a JDBC4 database connector; if you are using a JDBC3 connector, you will have to reconfigure c3p0 (see below).

## Further database connection customization

The servlet uses [c3p0](http://www.mchange.com/projects/c3p0) for SQL connection pooling. C3P0 has quite elaborate configuration options; HRLS sets some default options through its own c3p0.properties file. Please refer to the c3p0 documentation to learn how to override them if required.