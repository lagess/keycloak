/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.connections.jpa;

import org.hibernate.exception.LockAcquisitionException;
import org.jboss.logging.Logger;
import org.keycloak.exceptions.RetryableTransactionException;
import org.keycloak.models.KeycloakTransaction;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import java.lang.reflect.Field;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaKeycloakTransaction implements KeycloakTransaction {

    private static final Logger logger = Logger.getLogger(JpaKeycloakTransaction.class);

    protected EntityManager em;

    private boolean ignoreRollbackOnly = false;

    private boolean activeSavePoint = false;



    public JpaKeycloakTransaction(EntityManager em) {
        this.em = em;
    }

    @Override
    public void begin() {
        em.getTransaction().begin();
        em.createNativeQuery("SAVEPOINT cockroach_restart;").executeUpdate();
        activeSavePoint = true;
    }

    @Override
    public void commit() {
        try {
            logger.trace("Committing transaction");
            em.flush();
            em.createNativeQuery("RELEASE SAVEPOINT cockroach_restart;").executeUpdate();
            em.getTransaction().commit();
            activeSavePoint = false;
        } catch (PersistenceException e) {
            if (e.getCause() instanceof LockAcquisitionException){
                cancelRollbakcOnlyFlag(em.getTransaction());
                ignoreRollbackOnly = true;
                throw new RetryableTransactionException(e);
            }
            throw PersistenceExceptionConverter.convert(e.getCause() != null ? e.getCause() : e);
        }
    }

    @Override
    public void rollback() {
        logger.trace("Rollback transaction");
        em.flush();
        em.createNativeQuery("ROLLBACK TO SAVEPOINT cockroach_restart;").executeUpdate();
        em.clear();
    }

    @Override
    public void setRollbackOnly() {
        em.getTransaction().setRollbackOnly();
    }

    @Override
    public boolean getRollbackOnly() {
        if (ignoreRollbackOnly){
            return false;
        }else{
            return  em.getTransaction().getRollbackOnly();
        }
    }

    @Override
    public boolean isActive() {
        return em.getTransaction().isActive();
    }

    @Override
    public void releaseSavePoint() {
        if (!activeSavePoint){
            //savepoint has already been released, nothing to do
            return;
        }

        try {
            logger.trace("Release SavePoint");
            em.createNativeQuery("RELEASE SAVEPOINT cockroach_restart; COMMIT;").executeUpdate();
            activeSavePoint = false;
        }catch(PersistenceException e){
            logger.debug("Exception swallowed during savepoint release", e);
        }
    }

    // For CockorachDB we must be able to perform a query after a rollback.
    // We need to trick the rollbackOnly mechanism to circumvent this Hibernate limitation.
    private void cancelRollbakcOnlyFlag(EntityTransaction transaction){
        try{
            // Get the private field
            final Field field = transaction.getClass().getDeclaredField("rollbackOnly");
            // Allow modification on the field
            field.setAccessible(true);
            field.set(transaction, false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("Failed to change rollbackOnly flag", e);
        }

    }
}
