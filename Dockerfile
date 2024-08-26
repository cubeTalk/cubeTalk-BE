FROM openjdk:17-slim

WORKDIR /app

COPY build/libs/cubetalk-server.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]