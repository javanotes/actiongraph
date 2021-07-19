## Action Graph
A topology to support hierarchical (decision tree) based asynchronous command processing. The framework is built on top of Akka actors. 

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

We should be able to create a decision tree with an _order completion state_ as the root. And, if we pass an command to the root, it should be percolated down the tree (un)conditionally. 

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
    public void accept(String actionPath, Serializable command) {
        // I am at actionPath
        // I have the command
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

// pass command from root, with node filter - or simply, match all
ordersTrigger.react(Predicates.MATCH_ALL, "some_event_signal");
// or from a sub-tree
servicingTrigger.react(Predicates.MATCH_ALL, "some_subevent_signal");
```

## Event Server
ActionGraph can be used as a lightweight and reliable __event mediation server__, deployed as a sidecar component in a microservice container for __configuration based__ command generation. The server assumes the responsibility of reliably posting events to the endpoint, including retrying on error and tracing the state. The mediation process uses Akka actor models to execute asynchronously.

While the default flavour generates event to HTTP (post) action endpoints, the soure code can always be extended to support other type of endpoints (like Kafka), or a different state store provider.

The library footprint is kept low by using JDK only implementation of JSON and HTTP client/server functionalities - no large dependency trees! The only dependencies used are Akka (thus Scala), [MapDB](https://github.com/jankotek/mapdb/releases/tag/mapdb-1.0.9) and [Tape](https://github.com/square/tape). 

__Please note: It uses *nashorn* script engine, which is supported till Java 10 only__

#### Configuration
Group configuration example:

```json
{
  "root":"orders",
  "graph":{
    "txnLogger":"",
    "serviceLogger":{
      "sync": "",
      "async":""
    }
  }
}
```
A corresponding action configuration for a path in the above graph.
```json
{
  "actionPath":"/orders/serviceLogger/async",
  "actionEndpoint": "http://localhost:7070/postLog",
  "actionTemplate": {
    "eventType": "serviceLogger",
    "requestId": "#{$.split(2)[0]}",
    "message": "event log generated at #{$.split(2)[1]}"
  }
}
```
Action graph REST API to pass the event payload

`
POST /actiongraph/fire/orders
`
```json
{
  "pathPattern": "/orders/.*",
  "payload": "urn12345678 22/10/21"
}
```
This would submit an event to the configured action endpoint (POST http://localhost:7070/postLog)
```json
{
    "eventType": "serviceLogger",
    "requestId": "urn12345678",
    "message": "event log generated at 22/10/21"
}
```
Other APIs:

`
GET /actiongraph/journal/{corrId}
`

`
POST /actiongraph/replay/{corrId}
`

`
GET /actiongraph/groups/{root}
`

#### Kafka endpoints
Kafka support is being introduced. The action template configuration changes are to be made in the *actionEndpoint* element
```json
{
  "actionPath":"/orders/serviceLogger/sync",
  "actionEndpoint": "kafka://<destinationTopic>:<keyProperty>",
  "script": "JsonPath",
  "actionTemplate": {
    "eventType": "syncLogger",
    "requestId": "#{$.requestId}",
    "message": "event log generated at #{$.eventTime}"
  }
}
```
Firing the below payload to ActionGraph mediation server
```json
{
	"actionPath": "/orders/serviceLogger/.*",
	"payload": {
		"requestId": "r123", 
		"eventTime": "11:55:22"
	}
}
```
Would publish the corresponding event message to Kafka
```json
{
  "requestId": "r123",
  "eventType": "syncLogger",
  "message": "event log generated at 11:55:22"
}
```
#### Template Engines
By default uses __JavaScript__ as the template expression language. So templating can follow any valid javascript syntax, as well as a _JsonPath_ syntax.
