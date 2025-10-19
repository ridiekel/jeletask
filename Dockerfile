FROM openjdk:25-jdk-slim
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED -Xms64m -Xmx64m -XX:+UseParallelGC"
RUN mkdir /data
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
