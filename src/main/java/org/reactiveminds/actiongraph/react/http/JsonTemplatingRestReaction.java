package org.reactiveminds.actiongraph.react.http;

import org.reactiveminds.actiongraph.react.AbstractTemplateBasedReaction;
import org.reactiveminds.actiongraph.react.templates.TemplateFunction;

public class JsonTemplatingRestReaction extends AbstractTemplateBasedReaction {

    public JsonTemplatingRestReaction(String url, String actionPath, String template, TemplateFunction.Engine engine) {
        super(url, actionPath, template, engine);
    }

    @Override
    public void destroy() {

    }
}
