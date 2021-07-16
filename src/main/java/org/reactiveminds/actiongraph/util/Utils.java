package org.reactiveminds.actiongraph.util;

public class Utils {
    private Utils(){}
    public static Throwable primaryCause(Throwable root){
        Throwable cause = root;
        while (cause.getCause() != null){
            cause = cause.getCause();
        }
        return cause;
    }
    public static String printPrimaryCause(Throwable root, int traceLevel){
        Throwable cause = primaryCause(root);
        StackTraceElement[] stackTrace = cause.getStackTrace();
        StringBuilder builder = new StringBuilder(cause.getMessage()).append("\n");
        for (int i=0; i<Math.min(traceLevel, stackTrace.length); i++){
            builder.append(stackTrace[i].toString()).append("\n");
        }
        return builder.toString();
    }
}
