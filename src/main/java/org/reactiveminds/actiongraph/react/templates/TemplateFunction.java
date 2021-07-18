package org.reactiveminds.actiongraph.react.templates;

import java.util.function.Function;

public interface TemplateFunction extends Function<String, String> {
    enum Engine {JavaScript, Velocity, JsonPath}
    String EXPR_BEGIN = "#{";
    String EXPR_END = "}";
    static TemplateFunction get(Engine engineEngine, String templateDoc){
        switch (engineEngine){
            case JavaScript:
                return new JavascriptTemplating(templateDoc);
            case Velocity:
                return new VelocityTemplating(templateDoc);
            case JsonPath:
                return new JsonPathTemplating(templateDoc);
        }
        throw new UnsupportedOperationException("Unknown engine: "+engineEngine);
    }
}
