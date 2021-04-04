FROM maven:3.5.4-jdk-8 as builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn package

#V4

FROM java:8-jdk
COPY --from=builder /usr/src/app/target/mdataserver-0.0.1-SNAPSHOT.jar /usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar
WORKDIR /usr/src/app
EXPOSE 8080
EXPOSE 443
#CMD ["tail", "-f", "/dev/nulljava"]
#CMD ["java", "-jar", "/usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar"]
#CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-Djavax.net.debug=SSL,handshake,data,trustmanager", "-jar", "/usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar"]
CMD ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005", "-jar", "/usr/src/app/mdataserver-0.0.1-SNAPSHOT.jar"]
#LDG Add -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 above for IntelliJ Debugging
#LDG Added -Djavax.net.debug=SSL,handshake,data,trustmanager which writes networking info to console