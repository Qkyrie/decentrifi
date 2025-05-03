FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml files for dependency resolution
COPY pom.xml ./
COPY data-api/pom.xml ./data-api/

# Download all required dependencies
RUN mvn dependency:go-offline -B -f data-api/pom.xml

# Copy source files
COPY data-api/src ./data-api/src

# Package the application
RUN mvn package -B -f data-api/pom.xml

# Runtime image
FROM eclipse-temurin:21.0.3_7-jre-alpine
WORKDIR /app

# Copy the built application
COPY --from=build /app/data-api/target/app.jar ./app.jar

# Create directory for logs
RUN mkdir -p /app/logs

# Expose the application port
EXPOSE 8081

# Launch the application
CMD ["java", "-jar", "app.jar"]