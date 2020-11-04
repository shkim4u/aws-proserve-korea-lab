#!/bin/sh
# arc_device_simulator.sh - Read device event sample logs from files and
# send them to designated syslog server.
#
# author: Sang Hyoun Kim
# 

DIR=`cd "\`dirname "$0"\`" && pwd`

if [ -z "$JAVA_HOME" ] ; then
        JAVA_HOME=`readlink -f \`which java 2>/dev/null\` 2>/dev/null | \
        sed 's/\/bin\/java//'`
fi

"$JAVA_HOME"/bin/java $JAVA_OPTS -jar arc_event_generator.jar "$@"

exit $?
