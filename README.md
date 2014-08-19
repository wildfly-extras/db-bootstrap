Wildfly Database bootstrap extension
=========================================

This wildfly extension enables support for bootstrapping a database under the deployment startup process.
The extension allow a list of scan elements pointing to a specific EAR/JAR file containing the classes
for bootstrapping database. This will be executed before the deployment of EAR/WAR/JAR files and will allow
to bootstrap a database before the application is deployed and started.

The bootstrap process is divided into two phases. In the first phase the database schema is created. In the second
phase the database schema is updated (by running any update scripts if needed). 

This extension is currently aimed at Hibernate users. But it can also be used without Hibernate (although you won't get automatic database connectivity etc).

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
To bootstrap a database, create a class and annotate it with @BootstrapDatabase. This annotation requires one mandatory attribute:
- hibernateCfg - a String pointing to a Hibernate configuration file in the module classpath

It accepts two optional attributes:
- priority - an integer primitive that can be used to assign a priority to the bootstrap class (might be useful in case of multiple annotated @BootstrapDatabase classes)
- name - a String that gives the bootstrap class a logical name. This name can be used in combination with system properties to define hibernate configuration properties - or to override those already defined in the Hibernate configuration file (referenced by the hibernateCfg attribute).

There is one additional requirement to your bootstrap class: Add a method annotated with @BootstrapSchema or @UpdateSchema. Or add both methods. The @BootstrapSchema and @UpdateSchema annotations gives your methods a certain semantic bootstrap meaning with respect to the bootstrapping process. The bootstrapping process is composed of two phases: 
- Phase 1: All @BootstrapSchema methods are invoked (in prioritized order, according to @BootstrapDatabase.priority)
- Phase 2: All @UpdateSchema methods are invoked (in prioritized order, according to @BootstrapDatabase.priority)

It is in these methods that you should implement functionality to respectively create a new schema or update an existing schema. You could actually create the schema in the @UpdateSchema method - the extension doesn't really care. But lets just say that it isn't what this extension is designed for. 

Declare a Hibernate Session parameter in these methods and the extension will inject a Session object for you - connected to the database using the information you have provided in the Hibernate configuration document. If you don't declare a Hibernate Session parameter, then it is your methods own responsibility to create a database connection.

Example code
-------------------

    @BootstrapDatabase( hibernateCfg="hibernate.cfg.xml", priority=99, name="mybootstrapper" )
    public class BootstrapImpl {

        @BootstrapSchema
        private void doBootstrap(Session session) throws SQLException {
            // Create your database schema using the 'session' parameter
        }
    
        @UpdateSchema
        private void doUpgrade(Session session) throws SQLException {
            // Upgrade or update your database using the 'session' parameter
        }
    }

Firstly assume that this class is part of your enterprise application archive (EAR module). 

The @BootstrapDatabase annotation is what makes this a special class that will have the opportunity to run some code way before the application is fully deployed and started. 
Notice that we declare a reference to a Hibernate configuration document called "hibernate.cfg.xml" - that document contains the information necessary for  connecting to the database. 
It's just an ordinary Hibernate configuration file that is used by the extension to initialize a Hibernate SessionFactory object and Hibernate Session object.

The priority attribute has a value of 99 - so this bootstrapper class will run before classes with a lower priority. 

The bootstrapper is also given a name: mybootstrapper.
Having this name opens up the possibility for overriding hibernate properties in the Hibernate configuration file by using system properties. You can also define new Hibernate properties by means of system properties. For this to work, the system properties must have the following format:
- dbbootstrap.[boostrapper.name].[hibernate.property]

So in this example we could define the following system property to override the "hibernate.connection.url" property from the Hibernate configuration file:
- dbbootstrap.mybootstrapper.hibernate.connection.url=jdbc:postgresql://localhost:5432/importantdb

Just add it to your JAVA_OPTS environment variable or define it in the standalone.xml / domain.xml configuration file. 


Detailed trace
-------------------
Declare a logger with the name "org.jboss.as.extension.db_bootstrap" in standalone.xml or domain.xml. That will give you detailed insight into the extension's actions.

Contributors:
-------------------
- Tomaž Cerar -  Providing a template with https://github.com/ctomc/wildfly-sdd and help. 
- Frank Vissing - Creator of db-bootstrap
- Flemming Harms - Creator of db-bootstrap 
- Nicky Moelholm - Contributer 
