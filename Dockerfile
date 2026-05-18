FROM maven:3.9.6-eclipse-temurin-17 AS base
#Set to "-P etl" for etl build
ARG MVN_PACKAGE_PARAM=""
ARG BUILD_VERSION=""
WORKDIR /isaac-api
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true -Dsegue.version=$BUILD_VERSION $MVN_PACKAGE_PARAM

FROM jetty:12.0.11-jdk17-eclipse-temurin
USER root
RUN mkdir /isaac-logs
RUN chmod 755 /isaac-logs
RUN chown jetty /isaac-logs
ADD resources/schools_list_2026_spring.tar.gz /local/data/
COPY --from=base /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

ADD resources/start.d/ $JETTY_BASE/start.d/

# Set JVM options for URI compliance (allow legacy characters like pipe '|' in question IDs)
ENV JAVA_TOOL_OPTIONS="-Dorg.eclipse.jetty.http.UriCompliance=LEGACY"

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE
