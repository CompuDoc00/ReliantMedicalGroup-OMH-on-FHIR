FROM openjdk:8-jre-alpine
MAINTAINER Emerson Farrugia <emerson@openmhealth.org>

ENV SERVER_PREFIX /opt/omh/shimmer

RUN mkdir -p $SERVER_PREFIX
ADD shimmer.war $SERVER_PREFIX/
EXPOSE 8083

#Added -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 for IntelliJ debugging
#Added -Djavax.net.debug=SSL,handshake,data,trustmanager for network debugging which writes to the Console
CMD /usr/bin/java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar $SERVER_PREFIX/shimmer.war --spring.config.location=file:$SERVER_PREFIX/
#CMD /usr/bin/java -Djavax.net.debug=SSL,handshake,data,trustmanager -jar $SERVER_PREFIX/shimmer.war --spring.config.location=file:$SERVER_PREFIX/
#CMD /usr/bin/java -jar $SERVER_PREFIX/shimmer.war --spring.config.location=file:$SERVER_PREFIX/


RUN apk update && apk add curl
WORKDIR /usr/local/share/ca-certificates
COPY FallonClinicRootCertB64.crt /usr/local/share/ca-certificates/EnterpriseRootCA.crt
RUN /usr/sbin/update-ca-certificates

#RUN keytool -keystore /usr/lib/jvm/java-8-oracle/jre/lib/security/cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias EnterpriseRootCA -file EnterpriseRootCA.crt

USER root
#/usr/lib/jvm/java-1.8-openjdk/jre/lib/security
COPY FallonClinicRootCertB64.crt $JAVA_HOME/lib/security
RUN cd $JAVA_HOME/lib/security && keytool -keystore cacerts -storepass changeit -noprompt -trustcacerts -importcert -alias EnterpriseRootCA -file FallonClinicRootCertB64.crt