FROM eclipse-temurin:25-jdk AS jre
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /build/app.jar
# A runtime with only the JDK modules the application needs
RUN cd /build && jar xf app.jar \
    && jlink --add-modules "$(jdeps --ignore-missing-deps -q --multi-release 25 --print-module-deps --recursive \
        --class-path 'BOOT-INF/lib/*' BOOT-INF/classes)" \
        --strip-debug --no-man-pages --no-header-files --compress=zip-6 --output /jre

FROM gcr.io/distroless/base-debian13:nonroot@sha256:6c591b4ac789b683ab3665327181e7cfb7a39b5834f42dcb73d7becb8d18a5d9
ENV JAVA_HOME=/opt/java
COPY --from=jre /jre ${JAVA_HOME}
ENV JAVA_TOOL_OPTIONS="--enable-native-access=ALL-UNNAMED -Xms64m -Xmx64m -XX:+UseParallelGC"
COPY --from=jre /build/app.jar /app.jar
# No shell in distroless; the check class runs on its own, without opening the Spring Boot jar
COPY --from=jre /build/BOOT-INF/classes/io/github/ridiekel/jeletask/HealthCheck.class /healthcheck/io/github/ridiekel/jeletask/
HEALTHCHECK --interval=30s --timeout=10s --start-period=2m --retries=3 CMD ["/opt/java/bin/java","-cp","/healthcheck","io.github.ridiekel.jeletask.HealthCheck"]
ENTRYPOINT ["/opt/java/bin/java","-jar","/app.jar"]
