FROM maven:3.9.6-amazoncorretto-21 AS build
WORKDIR /app

# copy the aggregator pom + module poms
COPY pom.xml ./
COPY data-ingestion/pom.xml data-ingestion/
COPY data-api/pom.xml data-api/
COPY chainlayer/pom.xml chainlayer/
COPY db/pom.xml db/
COPY web3/pom.xml web3/

# fetch all *remote* deps for data-api AND the modules it depends on (db, data-api)
RUN mvn dependency:go-offline -B \
    -pl data-api -am \
    -f pom.xml

# now bring in your source
COPY . .

# build data-api AND its reactor dependencies (db, data-api)
RUN mvn clean package -DskipTests -B \
    -pl chainlayer -am \
    -f pom.xml

# --- runtime stage unchanged ---
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/chainlayer/target/*-SNAPSHOT.jar /app/app.jar
COPY data-api/src/main/resources/application.conf /app/application.conf
CMD [ "/jre/bin/java", "-jar",  "-Duser.timezone=UTC", "/app/app.jar", "-XX:+ExitOnOutOfMemoryError", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:+UseSerialGC"]
