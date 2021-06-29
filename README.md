# IIS-Sandbox dockerized

Requires JDK 8

## IIS-sandbox changes
### Repository
Added as submodules:
- https://github.com/usnistgov/swp-mds-client.git
- https://github.com/usnistgov/vaccination_deduplication.git
- https://github.com/immregistries/VaccineForecastConnector.git
- https://github.com/immregistries/codebase-client.git
- https://github.com/immregistries/mqe-hl7-util.git
- https://github.com/immregistries/smm-tester.git
- https://github.com/immregistries/IIS-Sandbox.git

### Source
1. src/main/resource/hibernate.cfg.xml - change localhost to ${MYSQL_HOST_PORT}
2. src/database/create-database.sql - change @locahost to @%
3. src/database/upgrade v0.4.sql - prepend use iis;
4. src/database/upgrade v0.5BETA.sql - prepend use iis;

### POM
Update dependencies to latest versions for repos that do not provide tagged versions or releases:
1. IIS HL7 Tester and Simple Message Mover 2.30.1 - > 2.30.2 (or latest)

## Build and deploy
### Clone repository
    git clone https://github.com/dvci/IIS-Sandbox.git

### Clone submodules
    git submodule init
    git submodule update

### Aggregator POM
Run following to build and install dependencies from submodule repositories.
    
    mvn -f aggregator_pom.xml install -Dmaven.test.skip=true

### POM
Run following to build and install main repository.

    mvn install -Dmaven.test.skip=true    

### Container
    docker-compose up -d

## Run webapp
http://localhost:8081/iis-sandbox/