package org.reactiveminds.actiongraph.react;

import org.reactiveminds.actiongraph.react.templates.TemplateFunction;
import org.reactiveminds.actiongraph.util.ScriptUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LoggingTemplateBasedReaction extends AbstractTemplateBasedReaction{
    private static final Logger LOGGER = LoggerFactory.getLogger("EVENTLOG");
    public LoggingTemplateBasedReaction(String url, String actionPath, String template, TemplateFunction.Engine engine) {
        super(url, actionPath, template, engine);
    }
    @Override
    public void accept(String actionPath, String event) {
        try {
            final Map<String, String> headers = new HashMap<>();
            String content = content(event, headers);
            LOGGER.info("actionPath: {}, event: {}",actionPath, ScriptUtil.prettyJson(content));
        }
        catch (Exception e) {
            onIOError(event, e);
        }
    }

    @Override
    public void destroy() {

    }
}
