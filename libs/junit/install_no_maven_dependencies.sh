#!/bin/bash

DIRNAME=$(dirname $0)
#Install locally dependencies not existing on maven central
#If there is some license problem with this, an existing repo
mvn install:install-file -Dfile=${DIRNAME}/sqljdbc42.jar -Dpackaging=jar -DgroupId=com.microsoft.sqlserver -DartifactId=sqljdbc4 -Dversion=4.2
mvn install:install-file -Dfile=${DIRNAME}/db2jcc4.jar -Dpackaging=jar -DgroupId=com.ibm.db2.jcc -DartifactId=db2jcc -Dversion=4.7.85
mvn install:install-file -Dfile=${DIRNAME}/ojdbc7-12.1.0.2.jar -Dpackaging=jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2

# for maven3
# mvn install:install-file -DlocalRepositoryPath=libs -DcreateChecksum=true -Dpackaging=jar -Dfile=libs/junit/db2jcc4.jar -DgroupId=com.ibm.db2.jcc -DartifactId=db2jcc -Dversion=4.7.85
