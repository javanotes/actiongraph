package org.reactiveminds.actiongraph;

public class ActionGraphException extends IllegalStateException{
    public ActionGraphException(String s) {
        super(s);
    }

    public ActionGraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
