package org.reactiveminds.actiongraph.util;

public class TransientException extends RuntimeException{
    public TransientException(Throwable cause) {
        super(cause);
    }

    public TransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
