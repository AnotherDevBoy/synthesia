FROM amazoncorretto:11

WORKDIR /app

COPY target/app.jar /app/app.jar

COPY run.sh /app/run.sh

ENTRYPOINT ["/bin/bash", "run.sh"]
