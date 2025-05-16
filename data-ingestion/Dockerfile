FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY data-ingestion/pom.xml data-ingestion/
COPY data-api/pom.xml data-api/
COPY web3/pom.xml web3/
COPY chainlayer/pom.xml chainlayer/
COPY db/pom.xml db/

# fetch all *remote* deps for data-ingestion AND the modules it depends on (db, data-api)
RUN mvn dependency:go-offline -B \
    -pl data-ingestion -am \
    -f pom.xml

# now bring in your source
COPY . .

# build data-ingestion AND its reactor dependencies (db, data-api)
RUN mvn clean package -DskipTests -B \
    -pl data-ingestion -am \
    -f pom.xml

# --- runtime stage unchanged ---
FROM amazoncorretto:21-alpine
WORKDIR /app
# Install dumb-init for proper signal handling
RUN apk add --no-cache dumb-init
COPY --from=build /app/data-ingestion/target/app.jar /app/app.jar
COPY data-ingestion/src/main/resources/application.conf /app/application.conf
# Use dumb-init as the entrypoint to handle signals properly
# This allows kubernetes to override just the CMD part with its own arguments
ENTRYPOINT ["dumb-init", "--", "java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
# Default to auto mode if no arguments are provided
CMD ["--mode=auto"]
