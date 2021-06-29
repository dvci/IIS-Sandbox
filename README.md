# IIS-Sandbox dockerized

Requires JDK 8

## Build and deploy
Clone repository and submodules, build image and deploy containers

    git clone --recursive https://github.com/dvci/IIS-Sandbox.git  
    docker compose up -d

## Run webapp
http://localhost:8081/iis-sandbox/

## IIS-sandbox changes
### Repository
Added as submodules:
- https://github.com/usnistgov/swp-mds-client.git
- https://github.com/usnistgov/vaccination_deduplication.git
- https://github.com/immregistries/VaccineForecastConnector.git
- https://github.com/immregistries/codebase-client.git
- https://github.com/immregistries/mqe-hl7-util.git
- https://github.com/immregistries/smm-tester.git

### Source
Changes to source code:

1. src/main/resource/hibernate.cfg.xml - change localhost to ${MYSQL_HOST_PORT}
2. src/database/create-database.sql - change @locahost to @%
3. src/database/upgrade v0.4.sql - prepend use iis;
4. src/database/upgrade v0.5BETA.sql - prepend use iis;

### POM
Update any submodule dependencies to latest versions for repos that do not provide tagged versions or releases:
1. IIS HL7 Tester and Simple Message Mover 2.30.1 - > 2.30.3 (or latest)