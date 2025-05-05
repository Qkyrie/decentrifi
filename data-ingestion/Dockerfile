FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app

# copy the aggregator pom + module poms
COPY pom.xml ./
COPY data-ingestion/pom.xml data-ingestion/
COPY data-api/pom.xml data-api/
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
COPY --from=build /app/data-ingestion/target/app.jar /app/app.jar
COPY data-ingestion/src/main/resources/application.conf /app/application.conf
CMD ["java","-jar","/app/app.jar"]
