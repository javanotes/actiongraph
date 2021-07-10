package org.reactiveminds.actiongraph.util;

import java.util.Objects;

public class Assert {
    public static void notNull(Object o, String msg){
        if(o == null)
            throw new IllegalArgumentException(msg);
    }
    public static void notNull(Object o){
        notNull(o, "value is null");
    }
    public static void isTrue(boolean expr, String msg){
        if(!expr)
            throw new IllegalArgumentException(msg);
    }
    public static void notEmpty(String s, String msg){
        if(s == null || s.trim().isEmpty())
            throw new IllegalArgumentException(msg);
    }
}
