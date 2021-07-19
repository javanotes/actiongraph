package org.reactiveminds.actiongraph.util;

import java.util.Optional;

public class Utils {
    private Utils(){}
    public static boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
    public static Throwable primaryCause(Throwable root){
        Throwable cause = root;
        while (cause.getCause() != null){
            cause = cause.getCause();
        }
        return cause;
    }
    public static boolean isTransientError(Throwable cause){
        Optional<Class<? extends Throwable>> first = SystemProps.TRANSIENT_ERRORS.stream()
                .filter(aClass -> aClass.isAssignableFrom(primaryCause(cause).getClass())).findFirst();
        return first.isPresent();
    }
    public static String escapeJson(String jsString) {
        if(isEmpty(jsString))
            return jsString;
        jsString = jsString.replace("\\", "\\\\");
        jsString = jsString.replace("\"", "\\\"");
        jsString = jsString.replace("\b", "\\b");
        jsString = jsString.replace("\f", "\\f");
        jsString = jsString.replace("\n", "\\n");
        jsString = jsString.replace("\r", "\\r");
        jsString = jsString.replace("\t", "\\t");
        jsString = jsString.replace("/", "\\/");
        return jsString;
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
