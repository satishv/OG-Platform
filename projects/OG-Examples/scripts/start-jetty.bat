@echo off

REM PLAT-1527
pushd %~dp0\..

if "%JAVA_HOME%" == "" echo Warning: JAVA_HOME is not set
set JAVACMD=%JAVA_HOME%\bin\java.exe

set RUN_MODE=%1
if "%RUN_MODE%" == "" set RUN_MODE=example

"%JAVACMD%" -jar lib\org.eclipse-jetty-jetty-start-7.0.1.v20091125.jar ^
  -Xms1024m -Xmx3072m -XX:MaxPermSize=256M -XX:+UseConcMarkSweepGC ^
  -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing ^
  -Dlogback.configurationFile=jetty-logback.xml ^
  -Dopengamma.platform.runmode=%RUN_MODE% ^
  -Dopengamma.platform.marketdatasource=direct ^
  -Dopengamma.platform.os=win ^
  start.class=com.opengamma.examples.startup.ExampleServer ^
  config\engine-spring.xml ^
  "path=config;og-examples.jar" "lib=lib"
  
REM PLAT-1527
popd