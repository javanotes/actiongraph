package org.reactiveminds.actiongraph.react.http;

import org.reactiveminds.actiongraph.core.ActionGraphException;
import org.reactiveminds.actiongraph.react.AbstractSerializableReaction;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.util.JSEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Deprecated
/**
 * Simple json templating for POST-ing to rest endpoints
 */
class JsonTemplatingRestReactionOld extends AbstractSerializableReaction {

    private List<String> expressions = new ArrayList<>();
    private static String evaluate(String doc, String template, String expression) {
        return JSEngine.evaluateTemplate(doc, template, expression);
    }
    private String evaluate(String doc) {
        String result = jsonTemplate;
        for (String expr : expressions){
            result = evaluate(doc, result, expr);
        }
        return result;
    }
    private void getExpressions(){
        int from = jsonTemplate.indexOf(TemplateFunction.EXPR_BEGIN);
        int till = jsonTemplate.indexOf(TemplateFunction.EXPR_END);
        boolean finish = from == -1;
        while(!finish){
            if((from == -1 && till != -1) || (till == -1 && from != -1) || from >= till)
                throw new ActionGraphException("Parse error. Unbalanced expressions");
            expressions.add( jsonTemplate.substring(from, ++till));
            from = jsonTemplate.indexOf(TemplateFunction.EXPR_BEGIN, till);
            till = jsonTemplate.indexOf(TemplateFunction.EXPR_END, till);
            finish = from == -1;
        }
    }
    private String jsonTemplate;

    /**
     *
     * @param url
     * @param actionPath
     * @param jsonTemplate
     */
    public JsonTemplatingRestReactionOld(String url, String actionPath, String jsonTemplate) {
        super(url, actionPath);
        this.jsonTemplate = jsonTemplate;
        getExpressions();
    }

    @Override
    protected String content(String event, Map<String, String> headers) {
        // set additional headers
        return evaluate(event);
    }

    @Override
    public void destroy() {
        // release all
    }
}
