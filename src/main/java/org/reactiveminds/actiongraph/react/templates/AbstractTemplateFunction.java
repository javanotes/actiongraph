package org.reactiveminds.actiongraph.react.templates;

import org.reactiveminds.actiongraph.core.ActionGraphException;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractTemplateFunction implements TemplateFunction {
    protected final String template;
    private final List<String> expressions;
    AbstractTemplateFunction(String template){
        this.template = template;
        expressions = parseExpressions();
    }
    protected List<String> parseExpressions() {
        List<String> expressions = new ArrayList<>();
        int from = template.indexOf(TemplateFunction.EXPR_BEGIN);
        int till = template.indexOf(TemplateFunction.EXPR_END);
        boolean finish = from == -1;
        while(!finish){
            if((from == -1 && till != -1) || (till == -1 && from != -1) || from >= till)
                throw new ActionGraphException("Parse error. Unbalanced expressions");
            expressions.add( template.substring(from, ++till));
            from = template.indexOf(TemplateFunction.EXPR_BEGIN, till);
            till = template.indexOf(TemplateFunction.EXPR_END, till);
            finish = from == -1;
        }
        return expressions;
    }
    @Override
    public String apply(String payload) {
        String result = template;
        for (String expr : expressions){
            result = evaluate(payload, result, expr);
        }
        return result;
    }

    private String evaluate(String payload, String result, String expr) {
        String exprStr = expr.substring(EXPR_BEGIN.length(), expr.indexOf(EXPR_END));
        String eval = evaluate(payload, exprStr);
        return result.replace(expr, eval);
    }

    /**
     * Evaluate the exprStr against the given payload
     * @param payload
     * @param exprStr
     * @return
     */
    protected abstract String evaluate(String payload, String exprStr);
}
