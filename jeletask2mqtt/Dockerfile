FROM --platform=$BUILDPLATFORM openjdk:17-jdk-alpine
RUN addgroup -S teletask2mqtt && adduser -S teletask2mqtt -G teletask2mqtt
USER teletask2mqtt:teletask2mqtt
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
