package org.reactiveminds.actiongraph.react.templates;

import org.reactiveminds.actiongraph.util.ScriptUtil;

class JavascriptTemplating extends AbstractTemplateFunction {

    JavascriptTemplating(String template) {
        super(template);
    }

    @Override
    protected String evaluate(String payload, String exprStr) {
        return String.valueOf(ScriptUtil.evaluate(payload, exprStr));
    }
}
