package org.reactiveminds.actiongraph.store;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ActionData implements Serializable {
    public Set<Props> getProps() {
        return props;
    }

    public void setProps(Set<Props> props) {
        this.props = props;
    }

    private Set<Props> props = new HashSet<>();
    static class Props{
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Props props = (Props) o;
            return Objects.equals(url, props.url);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url);
        }

        String url;
        String jsonTemplate;
    }
}
