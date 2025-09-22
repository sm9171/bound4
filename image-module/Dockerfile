FROM openjdk:21-jre-slim

WORKDIR /app

COPY build/libs/image-module-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]