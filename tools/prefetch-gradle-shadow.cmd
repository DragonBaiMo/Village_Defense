@echo off
setlocal
REM Prefetch Shadow plugin marker + implementation into Maven local.

REM 1) plugin marker (from Gradle Plugin Portal Maven repo)
mvn -q dependency:get -U -DremoteRepositories=gradle-plugins::::https://plugins.gradle.org/m2/ -Dartifact=com.github.johnrengelman.shadow:com.github.johnrengelman.shadow.gradle.plugin:8.1.1:pom -Dtransitive=true
if errorlevel 1 exit /b 1

REM 2) actual plugin implementation jar (from Maven Central)
mvn -q dependency:get -U -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=com.github.johnrengelman:shadow:8.1.1 -Dtransitive=true
if errorlevel 1 exit /b 1

REM 3) key transitives (defensive)
mvn -q dependency:get -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=org.ow2.asm:asm:9.4 -Dtransitive=false
mvn -q dependency:get -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=org.ow2.asm:asm-commons:9.4 -Dtransitive=false
mvn -q dependency:get -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=commons-io:commons-io:2.11.0 -Dtransitive=false
mvn -q dependency:get -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=org.apache.ant:ant:1.10.13 -Dtransitive=false
mvn -q dependency:get -DrepoUrl=https://repo.maven.apache.org/maven2/ -Dartifact=org.vafer:jdependency:2.8.0 -Dtransitive=false

exit /b 0
endlocal
