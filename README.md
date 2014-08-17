Wildfly Database bootstrap extension
=========================================

This wildfly extension gives support for bootstrapping of database under the deployment start up process.
The extension allow a list of scan elements pointing to a specific EAR/JAR file containing the classes
for bootstrapping database. This will be executed before the deployment of EAR/WAR/JAR files and will allow
to bootstrap a database before the application is deployed and started.

The bootstrap is divided in 2 phases, where the first phase is creating the database schema and the second
is running any update scripts if needed. This extension is aimed for Hibernate users, but can be used 
without Hibernate.

Prerequisite
---------------
JDK 7
Maven 3.1.1

Build the module
---------------

For building the module it's require maven 3.x or later.

To build
> mvn clean install

For running Arquillian integration test
> mvn clean verify -P arquillian-wildfly-managed 

To install to existing WildFly run
---------------------------------
> mvn clean install -Pupdate-as -Dwildfly.home=/path/to/as8

The -Dwildfly.home is not necessary if $JBOSS_HOME is already pointing at your WildFly installation.

after install is done you can run WildFly with SDD subsystem by running

> ./standalone.sh -c standalone-db-bootstrap.xml

Configure the module
-------------------
For bootstrapping under the deployment use the following example of a configuration below and add it to the your jboss configuration standalone.xml or domain.xml.

Add multiple deployment you want to bootstrap by adding the "<scan file="myarchive.ear"/>, the attribute "filter-on-name" is optional and can be used
to limit the list of JAR files that is scanned for the @BootstrapDatabase annotation.

	<subsystem xmlns="urn:jboss:domain:db_bootstrap:1.0">
      <bootstrap-deployments>
         <scan filename="/content/myarchive.ear" filter-on-name="org.xyz" />
      </bootstrap-deployments>
    </subsystem>
    
User guide
-------------------
For bootstrapping a database create a class with @BootstrapDatabase pointing to the hibernate cfg, that should be used for creating
a connection to the destination database. if multiple bootstrapping classes exits they can be priorities by adding the parameter priority

Add @BootstrapSchema to a method for creating a database schema, if the signature contains the Hibernate Session as parameter it will pass the session. Otherwise it will just call the method without the session, and it's the method responsibility to create a database connection

Add @UpdateSchema to a method for upgrading the database schema, if the signature contains the Hibernate Session as parameter it will pass the session. Otherwise it will just call the method without the session and it's the method responsibility to create a database connection

    @BootstrapDatabase(hibernateCfg="hibernate.cfg.xml", priority=99)
    public class BootstrapImpl{
        @BootstrapSchema
        private void doBootstrap(Session session) throws SQLException {
            // create database schema
        }
    
        @UpdateSchema
        private void doUpgrade(Session session) throws SQLException {
        // upgrade or update the database
        }
    }

Contributors:
-------------------
- Tomaž Cerar -  Providing a template with https://github.com/ctomc/wildfly-sdd and help. 
- Frank Vissing - Creator of db-bootstrap
- Flemming Harms - Creator of db-bootstrap 
- Nicky Moelholm - Contributer 
