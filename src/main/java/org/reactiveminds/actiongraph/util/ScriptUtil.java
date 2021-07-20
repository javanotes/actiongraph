package org.reactiveminds.actiongraph.util;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.util.err.NonTransientException;

import javax.script.*;
import java.io.InputStreamReader;

public class ScriptUtil {
    private ScriptUtil(){}
    public static final String DOC_IDENTIFIER = "$";

    private static ScriptEngine SCRIPT_ENGINE;
    private static final String JSON_FUNC = "Java.asJSONCompatible(%s)";
    private static final String JSON_PRETTY = "JSON.stringify(%s, null, 2)";
    private static final String TO_JSON = "JSON.parse('%s')";
    static {
        // The Nashorn JavaScript script engine, its APIs, and the jjs tool have been removed.
        // The engine, the APIs, and the tool were deprecated for removal in Java 11 with the express intent to remove them in a future release. See JDK-8236933
        SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            SCRIPT_ENGINE.eval(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("jsonpath-0.8.0.js")));
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }
    public static JsonNode evaluateJson(String doc)  {
        Object json = null;
        try {
            json = SCRIPT_ENGINE.eval(String.format(JSON_FUNC, doc));
        } catch (ScriptException e) {
            throw new NonTransientException(e.getMessage().substring(0, 50) + " ..(truncated)", e);
        }
        return JsonNode.parse(json);
    }
    public static String prettyJson(String doc)  {
        Object json = null;
        try {
            json = SCRIPT_ENGINE.eval(String.format(JSON_PRETTY, doc));
        } catch (ScriptException e) {
            throw new NonTransientException(e.getMessage().substring(0, 50) + " ..(truncated)", e);
        }
        return "\n" + json;
    }
    public static String evaluateTemplate(String doc, String template, String expression)  {
        String exprStr = expression.substring(TemplateFunction.EXPR_BEGIN.length(), expression.length()-1);
        Object eval = evaluate(doc, exprStr);
        return template.replace(expression, eval.toString());
    }
    public static Object evaluate(String doc, String expression)  {
        Bindings bindings = SCRIPT_ENGINE.createBindings();
        bindings.put(DOC_IDENTIFIER, doc);
        try {
            return SCRIPT_ENGINE.eval(expression, bindings);
        } catch (ScriptException e) {
            throw new NonTransientException("unable to evaluate expression: "+expression, e);
        }
    }
    public static Object jsonPath(String doc, String expression) {
        Invocable function = (Invocable) SCRIPT_ENGINE;
        try {
            Object o = function.invokeFunction("jsonPath", SCRIPT_ENGINE.eval(String.format(TO_JSON, doc)), expression);
            if(o instanceof ScriptObjectMirror){
                ScriptObjectMirror map = (ScriptObjectMirror) o;
                return map.get("0"); //first item in js array. this will be valid iff the json path matches exactly one element
            }
            return o;
        } catch (ScriptException | NoSuchMethodException e) {
            throw new NonTransientException("unable to evaluate json path: "+expression, e);
        }
    }
}
