FROM maven:3-jdk-8 AS builder

WORKDIR /app
RUN apt-get update && apt-get dist-upgrade -y && apt-get install -y openjfx
COPY pom.xml /app/pom.xml
#RUN mvn dependency:copy-dependencies

COPY src /app/src

RUN mvn package

FROM circleci/openjdk:8-node

WORKDIR /app
CMD ["java", "-jar", "/app/ea-2-rdf.jar", "--help"]

RUN sudo apt-get update && sudo apt-get dist-upgrade -y && sudo apt-get install -y openjfx vim

COPY --from=builder /app/target/EnterpriseArchitectToRDF-*.jar /app/ea-2-rdf.jar
