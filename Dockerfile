FROM maven:3.9.3-amazoncorretto-11 as build

WORKDIR /app

COPY src src
COPY pom.xml pom.xml

RUN mvn clean install -DskipTests

FROM amazoncorretto:11

WORKDIR /app

COPY --from=build /app/target/app.jar /app/app.jar

COPY run.sh /app/run.sh

ENTRYPOINT ["/bin/bash", "run.sh"]
