package org.reactiveminds.actiongraph.util;

import org.apache.kafka.common.protocol.types.Field;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.UncheckedIOException;

public class JSEngine {
    private JSEngine(){}
    public static final String DOC_IDENTIFIER = "$";

    private static ScriptEngine SCRIPT_ENGINE;
    private static final String JSON_FUNC = "Java.asJSONCompatible(%s)";
    private static final String JSON_PRETTY = "JSON.stringify(%s, null, 2)";
    static {
        //The Nashorn JavaScript script engine, its APIs, and the jjs tool have been removed.
        // The engine, the APIs, and the tool were deprecated for removal in Java 11 with the express intent to remove them in a future release. See JDK-8236933
        SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
    }
    public static JsonNode evaluateJson(String doc) throws ScriptException {
        Object json = SCRIPT_ENGINE.eval(String.format(JSON_FUNC, doc));
        return JsonNode.parse(json);
    }
    public static String prettyJson(String doc) throws ScriptException {
        Object json = SCRIPT_ENGINE.eval(String.format(JSON_PRETTY, doc));
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
            throw new UncheckedIOException(new IOException("unable to evaluate JavaScript template", e));
        }
    }
}
