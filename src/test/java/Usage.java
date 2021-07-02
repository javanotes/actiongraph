import org.reactiveminds.actiongraph.core.Group;
import org.reactiveminds.actiongraph.core.Action;
import org.reactiveminds.actiongraph.ActionGraphs;
import org.reactiveminds.actiongraph.react.Predicates;
import org.reactiveminds.actiongraph.react.Reaction;

import java.io.Serializable;
import java.util.Arrays;
import java.util.logging.Logger;

public class Usage {
    public static void main(String[] args) throws InterruptedException {
        reaction();
        traversal();
        ActionGraphs.instance().release();
    }
    static void traversal(){
        Group app = ActionGraphs.instance().root("opt").changeGroup("app", true);
        app.changeGroup("work", true);
        app.changeGroup("work2", true);
        app.changeGroup("work3", true);
        app.print();
        Group classes = ActionGraphs.instance().createGroup("/opt/app/work/classes");
        classes.getAction("startup.java", true);
        classes.print();
        classes = ActionGraphs.instance().createGroup("/opt/app/work/");
        classes.print();
        final Action appYml = ActionGraphs.instance().createAction("/opt/app/work/resources", "application.yml");
        new Thread("t1"){
            public void run(){
                appYml.write(Thread.currentThread().getName() + " content ");
            }
        }.start();

        new Thread("t2"){
            public void run(){
                appYml.write(Thread.currentThread().getName() + " content ");
            }
        }.start();
        String read = appYml.read();
        System.out.println("read => "+read);
        appYml.write("some content");
        read = appYml.read();
        System.out.println("read => "+read);

        app.makeGroup("work4", true).makeGroup("work5", true);
        app.print();
    }
    static void tester() throws InterruptedException {
        // create root group
        Group ordersTrigger = ActionGraphs.instance().root("orders");
        // create action handler - the "reaction"
        Reaction myReaction = new Reaction(){
            @Override
            public void destroy() {

            }

            @Override
            public void accept(Action action, Serializable serializable) {
                // I am at action.path()
                // I have the signal serializable
            }
        };

        // create sub groups
        Group servicingTrigger = ordersTrigger.changeGroup("Servicing", true);
        // attach handlers to the actions
        servicingTrigger.getAction("Internal", true).addObserver(myReaction);
        servicingTrigger.getAction("External", true).addObserver(myReaction);
        ordersTrigger.getAction("Transaction", true).addObserver(myReaction);
        ordersTrigger.getAction("Sales", true).addObserver(myReaction);

        // pass event, with node filter - or simply, match all
        ordersTrigger.react(Predicates.MATCH_ALL, "some_event_signal");
    }
    static void reaction() throws InterruptedException {
        Group audit = ActionGraphs.instance().createGroup("/opt/app/work/classes");
        MyReaction myReaction = new MyReaction();
        MyReaction2 myReaction2 = new MyReaction2();
        audit.makeGroup("Inventory", true).makeGroup("Payment", true).makeGroup("Servicing", true);
        Group sub = audit.changeGroup("Inventory", false);
        sub.getAction("DebitLog", true).addObserver(myReaction);
        sub.getAction("CreditLog", true).addObserver(myReaction);
        audit.changeGroup("Payment", false).getAction("TxnLog", true).addObserver(myReaction);
        audit.changeGroup("Servicing", false).getAction("TxnLog", true)
                .addObserver(myReaction).addObserver(myReaction2);

        for (int i=0; i<100000; i++){
            audit.react(Predicates.MATCH_ALL, "Record_Success__"+i);
            audit.react(new Predicates.DirectoryOrFileWhitelistFilter(Arrays.asList("Servicing"), Arrays.asList("TxnLog")),
                    "Record_Payment_Failure__"+i);
        }
        Thread.sleep(30000);
        audit.delete();
        //audit.stopActor();
        //Thread.sleep(1000);
    }
    public static class MyReaction implements Reaction {
        private static final Logger LOG = Logger.getLogger(MyReaction.class.getName());
        @Override
        public void accept(Action context, Serializable serializable) {
            LOG.info(String.format("[%s] reaction executed for %s-%s : %s",
                    Thread.currentThread().getName(), context.parent().name(), context.name(), serializable));
        }

        @Override
        public void destroy() {

        }
    }
    public static class MyReaction2 implements Reaction{
        private static final Logger LOG = Logger.getLogger(MyReaction2.class.getName());
        @Override
        public void accept(Action context, Serializable serializable) {
            LOG.info(String.format("[%s] some other reaction for %s-%s : %s",
                    Thread.currentThread().getName(), context.parent().name(), context.name(), serializable));
        }

        @Override
        public void destroy() {

        }
    }
}
