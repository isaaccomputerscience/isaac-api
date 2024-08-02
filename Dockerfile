FROM maven:3.9.6-eclipse-temurin-17 AS base
#Set to "-P etl" for etl build
ARG MVN_PACKAGE_PARAM=""
ARG BUILD_VERSION=""
WORKDIR /isaac-api
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . /isaac-api
RUN mvn package -Dmaven.test.skip=true -Dsegue.version=$BUILD_VERSION $MVN_PACKAGE_PARAM

FROM jetty:12.0.8-jdk17-eclipse-temurin
USER root
RUN mkdir /isaac-logs
RUN chmod 755 /isaac-logs
RUN chown jetty /isaac-logs
ADD resources/school_list_2022.tar.gz /local/data/
COPY --from=base /isaac-api/target/isaac-api.war /var/lib/jetty/webapps/isaac-api.war
RUN chmod 755 /var/lib/jetty/webapps/*
RUN chown jetty /var/lib/jetty/webapps/*

COPY resources/start.ini /var/lib/jetty/

# prepare things so that jetty runs in the docker entrypoint
USER jetty
WORKDIR $JETTY_BASE

# enable jetty modules for ee9
RUN java -jar "$JETTY_HOME/start.jar" --add-modules=ee9-webapp,ee9-deploy,ee9-jsp,ee9-jstl,ee9-websocket-jetty,ee9-websocket-jakarta
