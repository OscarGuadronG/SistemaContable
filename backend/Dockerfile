FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN --mount=type=cache,target=/root/.m2 env -u MAVEN_CONFIG ./mvnw -q -DskipTests package

FROM icr.io/appcafe/open-liberty:kernel-slim-java21-openj9-ubi-minimal
COPY --chown=1001:0 src/main/liberty/config/server.xml /config/server.xml
RUN features.sh
COPY --chown=1001:0 --from=build /workspace/target/sistema-contable.war /config/apps/sistema-contable.war
COPY --chown=1001:0 --from=build /workspace/target/liberty-driver/postgresql-*.jar /opt/ol/wlp/usr/shared/resources/
RUN configure.sh

ENV PORT=9080 \
    JVM_ARGS="-Xmx300m -Xshareclasses:cacheDir=/tmp"
EXPOSE 9080
