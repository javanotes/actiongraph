# Action Graph EMS
An topology to support hierarchical (decision tree) asynchronous event processing. The framework is built on top of Akka actors. This is a prototype for a lightweight, scalable & reliable __event mediation server,__ which can be used as a sidecar component in a microservice container for __configuration based__ event generation.

#### Actions and Groups
An __action__ is the reactive component in the topology, that can have observer(s) attached to it. A __group__ is a collection of actions (or further groups), thus representing an _action tree_. Each _node_ in the tree will have its _path_ which can be used for filtering the traversal.

##### _Illustration:_ 
Say we want to signal the following events, based on the outcome of an order processing: 
- Servicing Internal
- Servicing External
- Transaction
- Sales
 
 OR
 
- Servicing Internal
- Transaction
- Sales

We should be able to create a decision tree with an _order completion state_ as the root. And, if we pass an event to the root, it should be percolated down the tree (un)conditionally. 

`

                            Order
                  +---------- |-----------+
                  |           |           |
                  |           |           |
              Servicing   Transaction   Sales
                  |
              +---+----+
              |        |
           Internal  External
`
#### Usage
```java

// create root group
Group ordersTrigger = ActionGraphs.instance().root("orders");
// create action handler - the "reaction"
Reaction myReaction = new Reaction(){
    @Override
    public void destroy() {
        // when the action is deleted
    }

    @Override
    public void accept(String actionPath, Serializable event) {
        // I am at actionPath
        // I have the event
        // thread safe per action
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
ordersTrigger.react(Predicates.MATCH_ALL, "some_event_signal");
// or from a sub-tree
servicingTrigger.react(Predicates.MATCH_ALL, "some_subevent_signal");
```
### Event Mediation
TBD
