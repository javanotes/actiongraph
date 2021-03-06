import org.reactiveminds.actiongraph.core.ActionGraph;
import org.reactiveminds.actiongraph.core.Group;
import org.reactiveminds.actiongraph.react.Matchers;
import org.reactiveminds.actiongraph.react.Reaction;

import java.io.*;
import java.util.UUID;
import java.util.logging.Logger;

public class Usage {
    public static void main(String[] args) throws InterruptedException {
        reaction();
        ActionGraph.instance().release();
    }

    static void tester() throws InterruptedException {
        // create root group
        Group ordersTrigger = ActionGraph.instance().getOrCreateRoot("orders");
        // create action handler - the "reaction"
        Reaction myReaction = new Reaction(){
            @Override
            public void destroy() {
                // when the action is deleted
            }

            @Override
            public void accept(String actionPath, String event) {
                // I am at actionPath
                // I have the event
            }
        };

        // create sub groups
        Group servicingTrigger = ordersTrigger.changeGroup("Servicing", true);
        // attach handlers to the actions
        servicingTrigger.getAction("Internal", true).addObserver(myReaction);
        servicingTrigger.getAction("External", true).addObserver(myReaction);
        ordersTrigger.getAction("Transaction", true).addObserver(myReaction);
        ordersTrigger.getAction("Sales", true).addObserver(myReaction);

        // pass event from root, with node filter - or simply, match all
        ordersTrigger.react(UUID.randomUUID().toString(), Matchers.ALL, "some_event_signal");
        // or from a sub-tree
        servicingTrigger.react(UUID.randomUUID().toString(), Matchers.ALL, "some_subevent_signal");
    }
    static void reaction() throws InterruptedException {
        Group audit = ActionGraph.instance().createGroup("/Orders");
        MyReaction myReaction = new MyReaction();
        MyReaction2 myReaction2 = new MyReaction2();
        audit.makeGroup("Inventory", true).makeGroup("Payment", true).makeGroup("Servicing", true);
        Group sub = audit.changeGroup("Inventory", false);
        sub.getAction("DebitLog", true).addObserver(myReaction);
        sub.getAction("CreditLog", true).addObserver(myReaction);
        audit.changeGroup("Payment", false).getAction("TxnLog", true).addObserver(myReaction);
        audit.changeGroup("Servicing", false).getAction("TxnLog", true)
                .addObserver(myReaction).addObserver(myReaction2);
        audit.changeGroup("Servicing", false).getAction("PartyLog", true)
                .addObserver(myReaction).addObserver(myReaction2);
        audit.changeGroup("Servicing", false).changeGroup("Integration", true).getAction("TxnLog", true)
                .addObserver(myReaction).addObserver(myReaction2);
        audit.print(new PrintWriter(new OutputStreamWriter(System.out)));
        for (int i=0; i<1000; i++){
            audit.react(UUID.randomUUID().toString(), Matchers.ALL, "Record_Success__"+i);
            audit.react(UUID.randomUUID().toString(),Matchers.REGEX("/Orders/Servicing/Integration.*"), "Record_Payment_Failure__"+i);

        }
        //System.out.println("/Orders/Servicing".matches("/Orders/Servicing/Integration.*"));
        Thread.sleep(30000);

    }
    public static class MyReaction implements Reaction {
        private static final Logger LOG = Logger.getLogger(MyReaction.class.getName());
        @Override
        public void accept(String context, String serializable) {
            LOG.info(String.format("[%s] reaction executed at %s : %s",
                    Thread.currentThread().getName(), context, serializable));
        }

        @Override
        public void destroy() {

        }
    }
    public static class MyReaction2 implements Reaction{
        private static final Logger LOG = Logger.getLogger(MyReaction2.class.getName());
        @Override
        public void accept(String context, String serializable) {
            LOG.info(String.format("[%s] some other reaction at %s : %s",
                    Thread.currentThread().getName(), context, serializable));
        }

        @Override
        public void destroy() {

        }
    }
}
