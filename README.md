Keycloak for CockroachDB
========================

Fork of Keycloak 3.4.3 modified to use CockroachDB.
See also the original README of Keycloak for more informations.


Building
--------

Ensure you have JDK 8 (or newer), Maven 3.1.1 (or newer) and Git installed

    java -version
    mvn -version
    git --version

First clone the Keycloak repository:

    git clone https://github.com/cloudtrust/keycloak.git
    cd keycloak

Install CockroachDB (Currently tested with version 2.0.2)

    Download the CockroachDB archive for Linux, and extract the binary:
        `wget -qO- https://binaries.cockroachdb.com/cockroach-v2.0.6.linux-amd64.tgz | tar  xvz`

    Copy the binary into your PATH so it's easy to execute cockroach commands from any shell:
         `cp -i cockroach-v2.0.6.linux-amd64/cockroach /usr/local/bin`

    If you get a permissions error, prefix the command with sudo.

Launch at least one node

   cockroach start --host=localhost --insecure --store=cockroach-node1

Initialize the database and users

    cockroach sql --insecure

    CREATE DATABASE keycloak;
    CREATE USER keycloak WITH PASSWORD 'keycloak';
    GRANT ALL ON DATABASE keycloak TO keycloak;


To build Keycloak run:

    mvn install

This will build all modules and run the testsuite.

To build the distribution run:

    mvn install -Pdistribution -Dpageload.timeout=3600000 -DfailIfNoTests=false -Dkeycloak.connectionsJpa.url=jdbc:postgresql://127.0.0.1:26257/keycloak -Dkeycloak.connectionsJpa.driver=org.postgresql.Driver -Dkeycloak.connectionsJpa.user=keycloak -Dkeycloak.connectionsJpa.password=keycloak

Once completed you will find distribution archives in `distribution`.



Configuring & Starting Keycloak
-------------------------------


Build the distribution as specified above, then extract it:

    tar xfz distribution/server-dist/target/keycloak-<VERSION>.tar.gz
    cd keycloak-<VERSION>

Configure the database (source: https://www.keycloak.org/docs/latest/server_installation/index.html#_database):

    Add the Postgresql driver (CockroachDB uses Postgres driver: http://central.maven.org/maven2/org/postgresql/postgresql/9.4.1212/postgresql-9.4.1212.jar)
        In .../modules/ directory create org/postgresql/main directory structure.
        Copy postgresl JAR in it and create module.xml file with the following content:

            <?xml version="1.0" ?>
            <module xmlns="urn:jboss:module:1.3" name="org.postgresql">

                <resources>
                    <resource-root path="postgresql-9.4.1212.jar"/>
                </resources>

                <dependencies>
                    <module name="javax.api"/>
                    <module name="javax.transaction.api"/>
                </dependencies>
            </module>


    Configure Keycloak to use our database, modify the existing datasource section of standalone.xml with the following content:

        <subsystem xmlns="urn:jboss:domain:datasources:5.0">
            <datasources>
                <datasource jndi-name="java:jboss/datasources/ExampleDS" pool-name="ExampleDS" enabled="true" use-java-context="true">
                    <connection-url>jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE</connection-url>
                    <driver>h2</driver>
                    <security>
                        <user-name>sa</user-name>
                        <password>sa</password>
                    </security>
                </datasource>
                <datasource jndi-name="java:jboss/datasources/KeycloakDS" jta="false" pool-name="KeycloakDS" enabled="true" use-java-context="true">
                    <connection-url>jdbc:postgresql://localhost:26257/keycloak</connection-url>
                    <driver>postgresql</driver>
                    <security>
                        <user-name>keycloak</user-name>
                        <password>keycloak</password>
                    </security>
                    <pool>
                        <min-pool-size>30</min-pool-size>
                        <max-pool-size>150</max-pool-size>
                        <prefill>true</prefill>
                        <use-strict-min>false</use-strict-min>
                        <flush-strategy>FailingConnectionOnly</flush-strategy>
                    </pool>
                </datasource>
                <drivers>
                    <driver name="postgresql" module="org.postgresql">
                        <xa-datasource-class>org.postgresql.Driver</xa-datasource-class>
                    </driver>
                    <driver name="h2" module="com.h2database.h2">
                        <xa-datasource-class>org.h2.jdbcx.JdbcDataSource</xa-datasource-class>
                    </driver>
                </drivers>
            </datasources>
        </subsystem>

Run:

    bin/standalone.sh

To stop the server press `Ctrl + C`.



Technical details
=================

CockroachDB (https://www.cockroachlabs.com/) is a multi-master SQL database designed to run in the cloud and being resilient to failures.
This database has a lot of very interesting properties such serializable isolation, lockless and distributed.

CockroachDB introduces the notion of SAVEPOINT. As this DB is lockless, a transaction may fail due to a concurrent transaction.
In such case, we can rollback to the SAVEPOINT and retry the transaction.
Retrying transactions has the benefit of increasing their priority each time they are retried, increasing their likelihood to succeed.
(More information are available in their very good documentation (https://www.cockroachlabs.com/docs/stable/transactions.html#client-side-transaction-retries, https://www.cockroachlabs.com/blog/how-cockroachdb-distributes-atomic-transactions/, https://www.cockroachlabs.com/blog/serializable-lockless-distributed-isolation-cockroachdb/ ))

So even if CockroachDB uses Postgresql driver to communicate with the DB, this transaction retries mechanism must be added at client side to be able to use CockroachDB.

In this fork, the client-side retry mechanism is added into KeycloakSessionServletFilter.
For each request, we start a transaction which also create a savepoint. All the business logic and execution of statements is performed. Then the transaction is committed by KeycloakTransactionCommitter.
If at some point, the transaction fails due to a concurrent transaction that connot be managed by the database, we receive a Retryable transaction error. This kind of error is transalted into a RetryableTransactionException.
This type of exception is catched by KeycloakSessionServletFilter which rollback to the savepoint and retry to execute the query.

Due to the rollbackOnly implemented in Keycloak and Hibernate, after a rollback a transaction cannot be used anymore.
The retry operation must be performed in the same transaction to increase its priority.
Thus the rollbackOnly mechanism is disabled/bypassed in order to keep the transaction active even after a rollback is issued.
As suggested by CockroachDB, we replace the default Hibernate transaction coordinator class to a custom one (https://github.com/cockroachdb/hibernate-savepoint-fix).
Moreover, we mainly modify JpaKeycloakTransaction so that if the transaction fails to commit due to retryable transaction error, we disable the rollbackOnly mechansim to able to retry the transaction.

CockroachDB does not support addition of some constraints (e.g. primary keys) after table creation.
To circumvent this limitation, we can create a new table, migrate the data, delete the old table, rename the new table with the correct name.
As CockroachDb was not supported by Keycloak until now, we didn’t adapt all existing liquibase scripts. We decided to create a new liquibase script which creates the whole database schema for the current version.
This current limitation is being discussed and will be fixed in future (https://github.com/cockroachdb/cockroach/issues/19141).

Some tests have also been slightly adapted to support SERIALIZABLE isolation, so it can also be a benefit to other DBs that would be configured with such level (i.e. Postgresql)

Notice that the current version of this fork adds the support of CockroachDb at the cost of breaking the support of other types of databases.


License
-------

* [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
