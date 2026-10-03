FROM eclipse-temurin:25-alpine
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED -Xms64m -Xmx64m -XX:+UseParallelGC"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
HEALTHCHECK --interval=30s --timeout=10s --start-period=2m --retries=3 CMD ["java","-jar","/app.jar","--healthcheck"]
ENTRYPOINT ["java","-jar","/app.jar"]
