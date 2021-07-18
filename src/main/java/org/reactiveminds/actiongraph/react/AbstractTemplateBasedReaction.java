package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.react.templates.TemplateFunction;

import java.util.Map;

public abstract class AbstractTemplateBasedReaction extends AbstractSerializableReaction{
    protected final TemplateFunction templateFunction;
    public AbstractTemplateBasedReaction(String url, String actionPath, String template, TemplateFunction.Engine engine) {
        super(url, actionPath);
        templateFunction = TemplateFunction.get(engine, template);
    }

    @Override
    protected String content(String event, Map<String, String> headers) throws Exception {
        return templateFunction.apply(event);
    }
}
