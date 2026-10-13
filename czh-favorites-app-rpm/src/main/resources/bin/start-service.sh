#!/bin/bash

# Change to the working directory
cd $(dirname $0)/..

JAVA_OPTS="-Xms384m -Xmx1024m -Dspring.profiles.active= \
 -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true \
 -Dlogging.config=/opt/czh-favorites/conf/logback-spring.xml \
 -Dserver.address=0.0.0.0 $JAVA_OPTS"

mkdir -p /var/run/czh
chown appuser01:appgroup /var/run/czh

## Start the Application
su -s /bin/bash - appuser01 -c "nohup /opt/jdk-25/bin/java $JAVA_OPTS -jar /opt/czh-favorites/lib/czh-favorites-app.jar --spring.config.location=/opt/czh-favorites/conf/application.yml >/dev/null 2>/dev/null &"
