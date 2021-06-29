FROM maven:3.5.2-jdk-8 AS build
COPY src /usr/src/app/src
COPY swp-mds-client /usr/src/app/swp-mds-client
COPY vaccination_deduplication /usr/src/app/vaccination_deduplication
COPY VaccineForecastConnector /usr/src/app/VaccineForecastConnector
COPY codebase-client /usr/src/app/codebase-client
COPY mqe-hl7-util /usr/src/app/mqe-hl7-util
COPY smm-tester /usr/src/app/smm-tester
COPY pom.xml /usr/src/app
COPY aggregator_pom.xml /usr/src/app
RUN mvn -f /usr/src/app/aggregator_pom.xml install -Dmaven.test.skip=true
RUN mvn -f /usr/src/app/pom.xml clean package -Dmaven.test.skip=true

FROM tomcat:jdk8-openjdk
COPY --from=build /usr/src/app/target/iis-sandbox.war $CATALINA_HOME/webapps
EXPOSE 8080