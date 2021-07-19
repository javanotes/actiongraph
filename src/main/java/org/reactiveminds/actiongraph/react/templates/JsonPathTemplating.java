package org.reactiveminds.actiongraph.react.templates;

import org.reactiveminds.actiongraph.util.ScriptUtil;

import java.io.IOException;
import java.io.UncheckedIOException;

class JsonPathTemplating extends AbstractTemplateFunction{
    JsonPathTemplating(String template) {
        super(template);
    }

    @Override
    protected String evaluate(String payload, String exprStr) {
        try {
            return String.valueOf(ScriptUtil.jsonPath(payload, exprStr));
        } catch (Exception e) {
            throw new UncheckedIOException(new IOException("Unable to evaluate json path: "+exprStr, e));
        }
    }
}
