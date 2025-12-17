# Etapa 1: Construcción (Build)
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Aprovechar el cache de capas para dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Compilar el JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución (Runtime)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos solo el JAR resultante de la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Configuración para optimizar Java en contenedores (usa tu RAM eficientemente)
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]