package org.reactiveminds.actiongraph.util.err;

import org.reactiveminds.actiongraph.util.Utils;

public class TransientException extends ActionGraphException {
    public TransientException(Throwable cause) {
        super(cause.getMessage(), Utils.primaryCause(cause));
    }

    public TransientException(String s) {
        super(s);
    }

    public TransientException(String message, Throwable cause) {
        super(message, cause);
    }
}
