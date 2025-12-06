# Dockerfile for Spring Boot microservices
FROM eclipse-temurin:17-jdk-jammy

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom.xml and resolve dependencies (for better layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Expose port (will be overridden by docker-compose)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/reactive-nexo-0.0.1-SNAPSHOT.jar"]