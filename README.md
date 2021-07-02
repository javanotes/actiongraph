# Action Graph
An topology to support hierarchical (decision tree) asynchronous event processing. The framework is built on top of Akka actors.

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
