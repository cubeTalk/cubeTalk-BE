FROM openjdk:17-slim

WORKDIR /app

COPY cubetalk-server.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]