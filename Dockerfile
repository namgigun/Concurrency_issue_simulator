FROM gradle:jdk-21-and-23-graal-jammy
VOLUME /tmp
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]