package org.keycloak.services.error;

public class RetryableTransactionException extends RuntimeException{

    public RetryableTransactionException(Throwable t){
        super(t);
    }
}
