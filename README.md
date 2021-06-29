# IIS-Sandbox dockerized

Requires JDK 8

## Repositories
Added as submodules:
- git clone https://github.com/usnistgov/swp-mds-client.git
- git clone https://github.com/usnistgov/vaccination_deduplication.git
- git clone https://github.com/immregistries/VaccineForecastConnector.git
- git clone https://github.com/immregistries/codebase-client.git
- git clone https://github.com/immregistries/mqe-hl7-util.git
- git clone https://github.com/immregistries/smm-tester.git
- git clone https://github.com/immregistries/IIS-Sandbox.git

## IIS-sandbox changes
### source
1. src/main/resource/hibernate.cfg.xml - change localhost to ${MYSQL_HOST_PORT}
2. src/database/create-database.sql - change @locahost to @%
3. src/database/upgrade v0.4.sql - prepend use iis;
4. src/database/upgrade v0.5BETA.sql - prepend use iis;

### POM
Update dependencies to latest versions for repos that do not provide tagged versions or releases:
1. IIS HL7 Tester and Simple Message Mover 2.30.1 - > 2.30.2 (or latest)

## Build
### Aggregator POM
Run following to build and install dependencies from submodule repositories.
    
    mvn -f aggregator_pom.xml install -Dmaven.test.skip=true

### POM
Run following to build and install main repository.

    mvn install -Dmaven.test.skip=true    

### Container
    docker-compose up -d

## Webapp
http://localhost:8081/iis-sandbox/