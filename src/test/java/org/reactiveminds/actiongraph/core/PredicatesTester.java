package org.reactiveminds.actiongraph.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.reactiveminds.actiongraph.core.ActionGraph;
import org.reactiveminds.actiongraph.core.Group;
import org.reactiveminds.actiongraph.react.Predicates;
import org.reactiveminds.actiongraph.react.Reaction;

import java.io.*;

@RunWith(BlockJUnit4ClassRunner.class)
public class PredicatesTester {
    static Reaction REACTION = new Reaction() {
        @Override
        public void destroy() {

        }

        @Override
        public void accept(String s, String serializable) {
            System.out.println(String.format("regex '%s' match at path: %s", serializable, s));
        }
    };

    //@Test(expected = ActionGraphException.class)
    public void testNameNotAllowedWithSlash(){
        ActionGraph.instance().getOrCreateRoot("/op");
    }
    //@Test
    public void matchActionsOfSingleGroup() throws InterruptedException {
        Group root = ActionGraph.instance().getOrCreateRoot("op");
        root.makeGroup("auto", true).makeGroup("finance", true).makeGroup("sales", true);
        root.changeGroup("auto",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("finance",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).changeGroup("direct", true).getAction("notify", true).addObserver(REACTION);
        //root.react(Predicates.GroupMatcher("sales"), "sales");
        Thread.sleep(1000);
    }
    @Test
    public void matchActionsByGroupPath() throws InterruptedException {
        String path = "/op/sales.*";
        Group root = ActionGraph.instance().getOrCreateRoot("op");
        root.makeGroup("auto", true).makeGroup("finance", true).makeGroup("sales", true);
        root.changeGroup("auto",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("finance",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).changeGroup("direct", true).getAction("notify", true).addObserver(REACTION);
        root.react(Predicates.PathMatcher(path), path);
        //root.print();
        Thread.sleep(1000);
    }
    @Test
    public void matchActionsByActionPath() throws InterruptedException {
        String path = "/op/sales/direct.*";
        Group root = ActionGraph.instance().getOrCreateRoot("op");
        root.makeGroup("auto", true).makeGroup("finance", true).makeGroup("sales", true);
        root.changeGroup("auto",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("finance",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).getAction("notify", true).addObserver(REACTION);
        root.changeGroup("sales",true).changeGroup("direct", true).getAction("notify", true).addObserver(REACTION);
        root.print(new PrintWriter(new OutputStreamWriter(System.out)));
        root.react(Predicates.PathMatcher(path), path);
        //root.print();
        Thread.sleep(2000);
    }
}
