FROM maven:3-jdk-8 AS builder

WORKDIR /app
RUN apt-get update && apt-get dist-upgrade -y && apt-get install -y openjfx
COPY pom.xml /app/pom.xml
COPY src /app/src

RUN mvn clean package

FROM openjdk:8-slim

VOLUME /data
WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/ea-2-rdf.jar"]
CMD ["--help"]

RUN apt-get update && apt-get dist-upgrade -y && apt-get install -y openjfx

COPY --from=builder /app/target/EnterpriseArchitectToRDF-*.jar /app/ea-2-rdf.jar
