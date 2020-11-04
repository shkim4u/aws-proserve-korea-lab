#!/bin/sh
# DeviceEventGenerator.sh - Read device event sample logs from files and
# send them to designated syslog server.
#
# author: Sang Hyoun Kim
# 

DIR=`cd "\`dirname "$0"\`" && pwd`

if [ -z "$JAVA_HOME" ] ; then
        JAVA_HOME=`readlink -f \`which java 2>/dev/null\` 2>/dev/null | \
        sed 's/\/bin\/java//'`
fi

TOOLSJAR="$JAVA_HOME/lib/tools.jar"

#if [ ! -f "$TOOLSJAR" ] ; then
#        echo "$JAVA_HOME seems to be no JDK!" >&2
#        exit 1
#fi

# TODO: Uncomment this line when executing with JAR file.
#"$JAVA_HOME"/bin/java $JAVA_OPTS -cp "$DIR/jvmtop.jar:$TOOLSJAR" \
#com.jvmtop.JvmTop "$@"

#java -classpath com.microfocus.security.arcsight.device.event.generator.DeviceEventGenerator
DEPSJARS=/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/bin:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/FakeSenderSyslog.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-cli-1.2.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-configuration-1.10.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-io-2.4.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-lang-2.6.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-lang3-3.8.1.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/commons-logging-1.2.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/jline-1.0.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/log4j-1.2.17.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/slf4j-api-1.7.7.jar:/Users/shkim4u1/Eclipse_Metadata/VER_4.7.0/DeviceEventGenerator/lib/slf4j-log4j12-1.7.7.jar
"$JAVA_HOME"/bin/java $JAVA_OPTS -cp "$DEPSJARS" \
com.microfocus.security.arcsight.device.event.generator.utils.ConsoleUtils "$@"

exit $?
