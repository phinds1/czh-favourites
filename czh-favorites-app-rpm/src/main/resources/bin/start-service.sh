#!/bin/bash

app="czh-favorites"

# Change to the working directory
cd $(dirname $0)/..

JAVA_OPTS="-Xms384m -Xmx1024m -Dspring.profiles.active= \
 -Djava.net.preferIPv4Addresses=true -Djava.net.preferIPv4Stack=true \
 -Dlogging.config=/opt/$app/conf/logback-spring.xml \
 -Dserver.address=0.0.0.0 $JAVA_OPTS"

mkdir -p /var/run/czh-favorites
chown appuser01:appgroup /var/run/czh-favorites

## Start the Application
su -s /bin/bash - appuser01 -c "nohup /opt/jdk-25/bin/java $JAVA_OPTS -jar /opt/$app/lib/czh-favorites-app.jar --spring.config.location=/opt/$app/conf/application.yml >/dev/null 2>/dev/null &"
