#!/bin/sh

if [ "`basename $0`" == "start-jetty.sh" ] ; then
  cd `dirname $0`/..
fi

if [ ! -z "$1" ]; then
  RUN_MODE="$1"
else
  RUN_MODE=example
fi

CLASSPATH=config:og-examples.jar
for FILE in `ls -1 lib/*` ; do
  CLASSPATH=$CLASSPATH:$FILE
done

if [ ! -z "$JAVA_HOME" ]; then
  JAVA=$JAVA_HOME/bin/java
elif [ -x /opt/jdk1.6.0_16/bin/java ]; then
  JAVA=/opt/jdk1.6.0_16/bin/java
else
  # No JAVA_HOME, try to find java in the path
  JAVA=`which java 2>/dev/null`
  if [ ! -x "$JAVA" ]; then
    # No java executable in the path either
    echo "Error: Cannot find a JRE or JDK. Please set JAVA_HOME"
    exit 1
  fi 
fi

JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.port=8052 -Dcom.sun.management.jmxremote.ssl=false"
MEM_OPTS="-Xms512m -Xmx2048m -XX:MaxPermSize=256M -XX:+UseConcMarkSweepGC \
  -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing"

$JAVA $JMX_OPTS $MEM_OPTS -jar lib/org.eclipse-jetty-jetty-start-7.0.1.v20091125.jar \
  -DSTOP.PORT=8079 -DSTOP.KEY=OpenGamma \
  -Dopengamma.platform.os=posix -Dopengamma.platform.runmode=$RUN_MODE \
  -Dopengamma.platform.marketdatasource=direct \
  -Dlogback.configurationFile=jetty-logback.xml \
  start.class=com.opengamma.examples.startup.ExampleServer \
  config/engine-spring.xml "path=$CLASSPATH"
