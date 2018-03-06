package org.keycloak.connections.jpa;

import org.hibernate.ConnectionAcquisitionMode;
import org.hibernate.ConnectionReleaseMode;
import org.hibernate.HibernateException;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.transaction.backend.jdbc.internal.DdlTransactionIsolatorNonJtaImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorBuilderImpl;
import org.hibernate.resource.transaction.backend.jdbc.internal.JdbcResourceLocalTransactionCoordinatorImpl;
import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransactionAccess;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.resource.transaction.TransactionCoordinator;
import org.hibernate.resource.transaction.TransactionCoordinatorBuilder;
import org.hibernate.resource.transaction.spi.TransactionCoordinatorOwner;
import org.hibernate.tool.schema.internal.exec.JdbcContext;


public class CockroachDbTransactionCoordinatorBuilder implements TransactionCoordinatorBuilder {
    public static final String SHORT_NAME = "jdbc";

    /**
     * Singleton access
     */
    public static final CockroachDbTransactionCoordinatorBuilder INSTANCE = new CockroachDbTransactionCoordinatorBuilder();

    @Override
    public TransactionCoordinator buildTransactionCoordinator(TransactionCoordinatorOwner owner, TransactionCoordinatorOptions options) {
        if ( owner instanceof JdbcResourceTransactionAccess) {
            return new CockroachDbTransactionCoordinator( this, owner, (JdbcResourceTransactionAccess) owner );
        }

        throw new HibernateException(
                "Could not determine ResourceLocalTransactionAccess to use in building TransactionCoordinator"
        );
    }

    @Override
    public boolean isJta() {
        return false;
    }

    @Override
    public ConnectionReleaseMode getDefaultConnectionReleaseMode() {
        return ConnectionReleaseMode.AFTER_TRANSACTION;
    }

    @Override
    public ConnectionAcquisitionMode getDefaultConnectionAcquisitionMode() {
        return ConnectionAcquisitionMode.AS_NEEDED;
    }


}
