#!/bin/bash

#Install locally dependencies not existing on maven central
#If there is some license problem with this, an existing repo
mvn install:install-file -Dfile=sqljdbc42.jar -Dpackaging=jar -DgroupId=com.microsoft.sqlserver -DartifactId=sqljdbc4 -Dversion=4.2
mvn install:install-file -Dfile=db2jcc4.jar -Dpackaging=jar -DgroupId=com.ibm.db2.jcc -DartifactId=db2jcc -Dversion=4.7.85
mvn install:install-file -Dfile=ojdbc6-11.2.0.1.0.jar -Dpackaging=jar -DgroupId=com.oracle -DartifactId=ojdbc6 -Dversion=11.2.0.1.0

# for maven3
# mvn install:install-file -DlocalRepositoryPath=libs -DcreateChecksum=true -Dpackaging=jar -Dfile=libs/junit/db2jcc4.jar -DgroupId=com.ibm.db2.jcc -DartifactId=db2jcc -Dversion=4.7.85
