package org.keycloak.exceptions;

public class RetryableTransactionException extends RuntimeException{

    public RetryableTransactionException(Throwable t){
        super(t);
    }
}
