# Comms Router

## Overview

Comms Router implements routing tasks to agents for handling. Routing is based on skills needed for the tasks and capabilities of the agents.

A [demo application](demo-application/README.md) shows how to apply the router to build a call center.

### Concepts

  * Task - work item characterized by its requirements - set of skills needed expressed by the user as key/value pairs.
  * Agent - abstract entity able to handle tasks, characterized by its capabilities - the skills it has expressed by the user as key/value pairs.
  * Queue - collection of tasks waiting for the next available agent. It has a predicate defined by the user that is matched agains the agents' skills to select these able to serve the queue.
  * Plan - defines how a task is handled by the router selecting a queue for it.
  * Router - a container for the router entities allowing different user application to share the same database.

### How it works

User application creates a router and then one or more queues, agents and plans.

Then the application calls the router to creates a task with its requirements and either places it in a queue or assigns a plan to it.

The plan contains list of rules, which the router executes in order. First rule which predicate matches the task requirements determines the queue for this task.

When an agent become available for this task, it is put into state "busy" and the callback URL provided by the user for this task is called with information about the task and the selected agent.

The user's application then informs the agent about the task and tracks the task's completion. When the task is done, the user's application changes the task's state within the router, which makes the agent available for subsequent tasks.

## Installing the router

### Requirements

  * Java - Oracle JDK/JRE 8 (build/runtime)
  * Apache Maven - 3.5 (build)
  * SQL Server - MySQL 5.7 (runtime)
  * Java Servlet Container - Tomcat 8 (runtime)

Although the software may work with different flavors of Java, SQL Server or Web Container, currently it is being tested with these listed above.

### Build

Install Java and Maven, clone the repo and execute:

`mvn install`

The resulting war file should be:

`web/target/comms-router-web.war`

### Install

Create database for the router.

Create DB user to be used by the router and grant this user access to the newly created database.

Configure JNDI data source with name "jdbc/commsRouterDB".
Details on how to do this in Tomcat can be found [here](docs/ConfiguringDatabaseAccess.md).

Deploy comms-router-web.war into Tomcat.
Depending on your Tomcat settings this can be done by simple copying it to the Tomcat's webapps directory.

### Test

List routers:

`curl http://localhost:8080/comms-router-web/api/routers`

## Quick tutorial

Note that the commands listed below are meant to be used with a Unix shell. Some of them need to be modified in order to work on Windows.

For example, the command for creating queues would look like this:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/queues/queue-es -H "Content-Type:application/json" -d "{\"predicate\":\"HAS(#{language},'es')\"}}"
`

For more information take a look at [this](winComms.bat) batch file.

**Resource Identification**

Routers are identified by their Ref ID, which can be provided by the user or generated by the system. Router Ref IDs must be unique within the system.

All other resources are identified by the their Ref ID, again provided by the user or generated by the application and the Ref ID of the router that contains them. Their Ref IDs must be unique within their containing router.

**Create a router**, providing it's Ref ID:

`curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router`

Or request the system to assign the Ref ID:

`curl -X POST http://localhost:8080/comms-router-web/api/routers`

The Ref ID is returned in the response body:

`{"ref":"HaOYogXa8qgX9NlRHJi9Y2"}`

And URL of the created entity is in the header LOCATION of the response:

`Location: http://localhost:8080/comms-router-web/api/routers/HaOYogXa8qgX9NlRHJi9Y2`

**Create some queues.**

For example, let's create one handled by English speaking agents:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/queues/queue-en -H 'Content-Type:application/json' -d$'{"predicate":"HAS(#{language},\'en\')"}}'
`

And one more handled by these speaking Spanish:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/queues/queue-es -H 'Content-Type:application/json' -d$'{"predicate":"HAS(#{language},\'es\')"}}'
`

**Create agents.**

Let's assume we have have three agents - Alice speaking English, Juan speaking Spanish and Maria speaking both English and Spanish.

So for Alice we'll have:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/agents/alice -H 'Content-Type:application/json' -d'{"address":"sip:alice@comms-router.org","capabilities":{"language":["en"]}}'
`

for Juan:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/agents/juan -H 'Content-Type:application/json' -d '{"address":"sip:juan@comms-router.org","capabilities":{"language":["es"]}}'
`

and for Maria:

`
curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/agents/maria -H 'Content-Type:application/json' -d'{"address":"sip:maria@comms-router.org","capabilities":{"language":["en","es"]}}'
`

Note the _address_ field. It does not affect the router logic and is used by the user application to store information that it needs to route tasks to this agent. In our example addresses are SIP URIs.

**List agents and note the queue assignments:**

`curl http://localhost:8080/comms-router-web/api/routers/my-router/agents`

```
[
  {
    "ref": "alice",
    "routerRef": "my-router",
    "capabilities": {
      "language": "en"
    },
    "address": "sip:alice@comms-router.org",
    "state": "offline",
    "queueIds": [
      "queue-en"
    ]
  },
  {
    "ref": "juan",
    "routerRef": "my-router",
    "capabilities": {
      "language": "es"
    },
    "address": "sip:juan@comms-router.org",
    "state": "offline",
    "queueIds": [
      "queue-es"
    ]
  },
  {
    "ref": "maria",
    "routerRef": "my-router",
    "capabilities": {
      "language": [
        "es",
        "en"
      ]
    },
    "address": "sip:maria@comms-router.org",
    "state": "offline",
    "queueIds": [
      "queue-es",
      "queue-en"
    ]
  }
]
```

**Create a plan.**

Let's create a plan with a rule that will route tasks requiring Spanish agent in our Spanish queue. Tasks that don't match this rule we will route to the English queue.

`curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/plans/by-language -H 'Content-Type:application/json' -d$'{"description":"put your plan description", "rules":[{"tag":"spanish", "predicate":"#{language} == \'es\'", "routes":[{"queueRef":"queue-es", "priority":3, "timeout":300}, {"priority":10, "timeout":800}]}], "defaultRoute":{"queueRef":"queue-en"}}'`

**Create tasks.**

`curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/tasks/task-es -H 'Content-Type:application/json' -d$'{"requirements":{"language":"es"},"planRef":"by-language","callbackUrl":"https://requestb.in/1koh4zk1?inspect"}'`

Here the task requires agent speaking Spanish and we assign to it our plan that routes tasks by language.

The "callbackUrl" parameter specifies the user application entry point to be called by the router for activity related with this task. An easy way to test the router is to use a requestb.in to accept the callback, as we are doing in this example.

In addition to using a plan to route tasks, the router accepts direct queue assignment by the user application:

`curl -X PUT http://localhost:8080/comms-router-web/api/routers/my-router/tasks/task-en -H 'Content-Type:application/json' -d$'{"queueRef":"queue-en","callbackUrl":"https://requestb.in/1koh4zk1?inspect"}'`

**Let's list the tasks and see the queues assigned:**

`curl http://localhost:8080/comms-router-web/api/routers/my-router/tasks`

```
[
  {
    "ref": "task-es",
    "routerRef": "my-router",
    "requirements": {
      "language": "es"
    },
    "userContext": null,
    "state": "waiting",
    "planRef": null,
    "queueRef": "queue-es",
    "agentRef": null,
    "callbackUrl": "https://requestb.in/1koh4zk1"
  },
  {
    "ref": "task-en",
    "routerRef": "my-router",
    "requirements": null,
    "userContext": null,
    "state": "waiting",
    "planRef": null,
    "queueRef": "queue-en",
    "agentRef": null,
    "callbackUrl": "https://requestb.in/1koh4zk1?inspect"
  }
]
```

All tasks are in state "waiting" as all our agents are in state "offline".

**Change agent's state.**

`curl -X POST http://localhost:8080/comms-router-web/api/routers/my-router/agents/maria -H 'Content-Type:application/json' -d '{"state":"ready"}'`

Now the router assigns a task this agent and changes its state to "busy". Call to the provided callbackUrl can be observed in requestb.in.

**Complete Task.**

When the user application is done with processing a task it must declare it as done:

`curl -X POST http://localhost:8080/comms-router-web/api/routers/my-router/tasks/task-es -H 'Content-Type:application/json' -d '{"state":"completed"}'`

The router then releases the agent and it is available for other tasks. As in this example the agent "Maria" can serve both queues, it will automatically get the other task we created:

`curl http://localhost:8080/comms-router-web/api/routers/my-router/tasks`

```
[
  {
    "ref": "task-es",
    "routerRef": "my-router",
    "requirements": {
      "language": "es"
    },
    "userContext": null,
    "state": "completed",
    "planId": null,
    "queueRef": "queue-es",
    "agentRef": null,
    "callbackUrl": "https://requestb.in/1koh4zk1"
  },
  {
    "ref": "task-en",
    "routerRef": "my-router",
    "requirements": null,
    "userContext": null,
    "state": "assigned",
    "planRef": null,
    "queueRef": "queue-en",
    "agentRef": "maria",
    "callbackUrl": "https://requestb.in/1koh4zk1"
  }
]
```

We should finish our journey by making this task completed:

`curl -X POST http://localhost:8080/comms-router-web/api/routers/my-router/tasks/task-en -H 'Content-Type:application/json' -d '{"state":"completed"}'`

## Additional resources

  * Browse [docs](docs) directory for additional documentation.
  * See our [demo application](demo-application/README.md) as an example how to integrate the router with Nexmo API]
