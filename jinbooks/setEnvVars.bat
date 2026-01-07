echo off
echo set env

set JBX_VERSION=1.0.1
set JBX_REPOSITORY=jinbooks

set JAVA_HOME=D:\Environment\jdk-17.0.6
set GRADLE_HOME=D:\Environment\gradle-8.8

call %JAVA_HOME%/bin/java -version
call %GRADLE_HOME%/bin/gradle -version
