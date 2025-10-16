FROM openjdk:25-jdk-slim
RUN mkdir /data
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
