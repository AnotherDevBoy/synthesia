# Synthesia

In this README, you can find instructions on how to build and run the app.

For more information about the design of the solution, tradeoffs made and more, head to the [Tech Design document](tech-design.md).

## Requirements

- [Maven](https://maven.apache.org/install.html)
- [Coretto 11 JDK](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
- [Docker](https://docs.docker.com/desktop/)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Container images

The app depends on certain version of the containers. You can download them by running:

```
docker pull localstack/localstack:2.2
docker pull redis:6.2-alpine
```

## Build

To build the application (including the container), run: `make build`

## Run

To run the application (inside of Docker), you need to:

- Start the infrastructure (Redis, AWS): `make infra`
- Start the application: `API_KEY=<the provided key> make run`
- Stop all the running containers: `make stop`

## Test locally

Once the application and the infrastructure is app and running, you should see something like:

```
synthesia-synthesia-app-1  | [main] INFO io.javalin.Javalin - Starting Javalin ...
synthesia-synthesia-app-1  | [main] INFO org.eclipse.jetty.server.Server - jetty-11.0.15; built: 2023-04-11T18:37:53.775Z; git: 5bc5e562c8d05c5862505aebe5cf83a61bdbcb96; jvm 11.0.20+8-LTS
synthesia-synthesia-app-1  | [main] INFO org.eclipse.jetty.server.session.DefaultSessionIdManager - Session workerName=node0
synthesia-synthesia-app-1  | [main] INFO org.eclipse.jetty.server.handler.ContextHandler - Started i.j.j.@5866731{/,null,AVAILABLE}
synthesia-synthesia-app-1  | [main] INFO org.eclipse.jetty.server.AbstractConnector - Started ServerConnector@4b7ed03e{HTTP/1.1, (http/1.1)}{0.0.0.0:7070}
synthesia-synthesia-app-1  | [main] INFO org.eclipse.jetty.server.Server - Started Server@2bec068b{STARTING}[11.0.15,sto=0] @1986ms
synthesia-synthesia-app-1  | [main] INFO io.javalin.Javalin -
synthesia-synthesia-app-1  |        __                  ___          ______
synthesia-synthesia-app-1  |       / /___ __   ______ _/ (_)___     / ____/
synthesia-synthesia-app-1  |  __  / / __ `/ | / / __ `/ / / __ \   /___ \
synthesia-synthesia-app-1  | / /_/ / /_/ /| |/ / /_/ / / / / / /  ____/ /
synthesia-synthesia-app-1  | \____/\__,_/ |___/\__,_/_/_/_/ /_/  /_____/
synthesia-synthesia-app-1  |
synthesia-synthesia-app-1  |        https://javalin.io/documentation
synthesia-synthesia-app-1  |
synthesia-synthesia-app-1  |
synthesia-synthesia-app-1  | [main] INFO io.javalin.Javalin - Listening on http://localhost:7070/
synthesia-synthesia-app-1  | [main] INFO io.javalin.Javalin - You are running Javalin 5.6.2 (released July 31, 2023).
synthesia-synthesia-app-1  | [main] INFO io.javalin.Javalin - Javalin started in 183ms \o/
synthesia-synthesia-app-1  | [pool-3-thread-1] INFO io.synthesia.async.MessageSigningConsumer - MessageSigningConsumer started
synthesia-synthesia-app-1  | [pool-3-thread-1] INFO io.synthesia.async.SqsMessageSigningQueue - Receive messages from http://localstack:4566/000000000000/sign-queue
synthesia-synthesia-app-1  | [pool-4-thread-1] INFO io.synthesia.async.MessageSingingProcessor - MessageSingingProcessor started

...
```

After that, simply send a `POST` request to `http://localhost:7070/crypto/sign` with the message and the webhook URL in the body:

```
curl --location 'http://localhost:7070/crypto/sign?message=test&webhookUrl=http%3A%2F%2F127.0.0.1%3A7070%2Fwebhook' --verbose
```

The app exposes an additional `/webhook` endpoint to facilitate local testing.
