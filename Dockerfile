FROM eclipse-temurin:25-alpine
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED -Xms64m -Xmx64m -XX:+UseParallelGC"
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
# Same rules as --healthcheck (see HealthCheck.java), but without starting a JVM every check
HEALTHCHECK --interval=30s --timeout=10s --start-period=2m --retries=3 CMD f=/tmp/jeletask2mqtt.health; \
    cat "$f" && [ $(( $(date +%s) - $(stat -c %Y "$f") )) -le 30 ] && grep -qx 'teletask=UP' "$f" && grep -qx 'mqtt=UP' "$f"
ENTRYPOINT ["java","-jar","/app.jar"]
