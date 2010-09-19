@echo off
set ANT_OPTS=-Dlog4j.configuration=cobertura-log4j.xml
call ant coverage
