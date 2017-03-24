# Introduction

## User Problem
User would like to open up an endpoint to expose a business operation over remote boundaries.

## Description

This Experience showcases a basic mapping of a business operation to a remote endpoint.  By taking this approach, clients leverage the HTTP protocol as a transport mechanism to call upon services.  Application engineers define their APIs using a broad interpretation of REST fundamentals, encouraging freedom in design and quick prototyping.

As an application or service matures, this approach may not scale as desired to properly support clean API design or use cases involving database interactions.  Any operations involving shared, mutable state will have to be integrated with an appropriate backing datastore; all requests here will be scoped only to the container servicing the request, and there is no guarantee that subsequent requests will be served by the same container.

This is recommended as an introduction to the mechanics of opening a service to be called upon by remote processes.

## Concepts & Architectural Patterns

HTTP API, Richardson Maturity Model Level 0

# Prerequisites

To get started with these quickstarts you'll need the following prerequisites:

Name | Description | Version
--- | --- | ---
[java][1] | Java JDK | 8
[maven][2] | Apache Maven | 3.2.x
[oc][3] | OpenShift Client | v3.3.x
[git][4] | Git version management | 2.x

[1]: http://www.oracle.com/technetwork/java/javase/downloads/
[2]: https://maven.apache.org/download.cgi?Preferred=ftp://mirror.reverse.net/pub/apache/
[3]: https://docs.openshift.com/enterprise/3.2/cli_reference/get_started_cli.html
[4]: https://git-scm.com/book/en/v2/Getting-Started-Installing-Git

# Build the Project

The project uses Wildfly Swarm to create and package the service.

Execute the following maven command:

```
mvn clean install
```

# Launch and test

1. Run the following command to start the maven goal of WildFlySwarm:

    ```
    mvn wildfly-swarm:run
    ```

1. If the application launched without error, use the following command to access the REST endpoint exposed using curl or httpie tool:

    ```
    http http://localhost:8080/api/greeting
    curl http://localhost:8080/api/greeting
    ```

1. To pass a parameter for the Greeting Service, use the following HTTP request:

    ```
    http http://localhost:8080/api/greeting?name=Charles
    curl http://localhost:8080/api/greeting?name=Bruno
    ```

# OpenShift Online

## Login and prepare your openshift account

1. Go to [OpenShift Online](https://console.dev-preview-int.openshift.com/console/command-line) to get the token used by the oc client for authentication and project access.

2. On the oc client, execute the following command to replace MYTOKEN with the one from the Web Console:

    ```
    oc login https://api.dev-preview-int.openshift.com --token=MYTOKEN
    ```

## Working with a service that provides a simple HTTP interface

1. Use the Fabric8 Maven Plugin to launch the S2I process on the OpenShift Online machine & start the pod.

    ```
    mvn clean fabric8:deploy -Popenshift -DskipTests
    ```

2. Get the route url.

    ```
    oc get route/wildfly-swarm-rest
    NAME              HOST/PORT                                          PATH      SERVICE                TERMINATION   LABELS
    wildfly-swarm-rest   <HOST_PORT_ADDRESS>             wildfly-swarm-rest:8080
    ```

3. Use the Host or Port address to access the REST endpoint.
    ```
    http http://<HOST_PORT_ADDRESS>/greeting
    http http://<HOST_PORT_ADDRESS>/greeting?name=Bruno

    or

    curl http://<HOST_PORT_ADDRESS>/greeting
    curl http://<HOST_PORT_ADDRESS>/greeting?name=Bruno
    ```

Congratulations! You have just deployed your first HTTP service to openshift.
