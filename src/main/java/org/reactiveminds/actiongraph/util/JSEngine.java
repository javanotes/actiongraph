package org.reactiveminds.actiongraph.util;

import javax.script.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public class JSEngine {
    private JSEngine(){}
    public static final String EXPR_BEGIN = "#{";
    public static final String EXPR_END = "}";
    public static final String DOC_IDENTIFIER = "$";

    private static ScriptEngine SCRIPT_ENGINE;
    private static final String JS_JSON_FUNC = "getJson";
    static {
        //The Nashorn JavaScript script engine, its APIs, and the jjs tool have been removed.
        // The engine, the APIs, and the tool were deprecated for removal in Java 11 with the express intent to remove them in a future release. See JDK-8236933
        SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            SCRIPT_ENGINE.eval("function "+JS_JSON_FUNC+"(jsonStr) { return Java.asJSONCompatible(jsonStr);}");
        } catch (ScriptException e) {
            throw new RuntimeException("json evaluating function compile failed", e);
        }
    }
    public static JsonNode evaluateJson(String doc)  {
        Object json = null;
        try {
            json = SCRIPT_ENGINE.eval(String.format("Java.asJSONCompatible(%s)", doc));
        }  catch (Exception e) {
            throw new UncheckedIOException(new IOException("json parse error", e));
        }
        return JsonNode.parse(json);
    }
    public static String evaluateTemplate(String doc, String template, String expression)  {
        String exprStr = expression.substring(EXPR_BEGIN.length(), expression.length()-1);
        Bindings bindings = SCRIPT_ENGINE.createBindings();
        bindings.put(DOC_IDENTIFIER, doc);
        Object eval = null;
        try {
            eval = SCRIPT_ENGINE.eval(exprStr, bindings);
        } catch (ScriptException e) {
            throw new UncheckedIOException(new IOException("unable to evaluate template", e));
        }
        return template.replace(expression, eval.toString());
    }
}
