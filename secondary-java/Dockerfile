FROM maven:3.6.3-jdk-11 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:11
COPY --from=build /usr/src/app/target/secondary-java-1.0-SNAPSHOT.jar /usr/app/secondary-java-1.0-SNAPSHOT.jar
ENTRYPOINT ["java","-jar","/usr/app/secondary-java-1.0-SNAPSHOT.jar"]