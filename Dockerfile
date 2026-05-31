FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Cache de dependencias Maven (solo se invalida si cambia pom.xml)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# DEPLOY_VERSION invalida el cache del código fuente en cada deploy
ARG DEPLOY_VERSION=1
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
