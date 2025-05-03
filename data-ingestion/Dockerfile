FROM maven:3.9.6-amazoncorretto-21 AS build

WORKDIR /app

# Copy the parent POM
COPY pom.xml /app/
COPY data-ingestion/pom.xml /app/data-ingestion/

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B -f data-ingestion/pom.xml

# Copy source files
COPY data-ingestion/src /app/data-ingestion/src

# Build the application
RUN mvn clean package -DskipTests -f data-ingestion/pom.xml

# Runtime stage
FROM amazoncorretto:21-alpine

WORKDIR /app

# Copy the built application
COPY --from=build /app/data-ingestion/target/app.jar /app/app.jar
COPY data-ingestion/src/main/resources/application.conf /app/application.conf

# Run the application
CMD ["java", "-jar", "/app/app.jar"]
