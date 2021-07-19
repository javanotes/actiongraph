package org.reactiveminds.actiongraph.util.err;

import org.reactiveminds.actiongraph.util.Utils;

public class NonTransientException extends ActionGraphException {
    public NonTransientException(Throwable cause) {
        super(cause.getMessage(), Utils.primaryCause(cause));
    }

    public NonTransientException(String s) {
        super(s);
    }

    public NonTransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
