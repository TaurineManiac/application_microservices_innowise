FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
#What: Copies only the Maven configuration fole intp the container.
#Why: docker caches layers. The pom.xml rarely changes. By coping it before yours source code, Docker saves the "downloaded deoendencies" layer into a cache.

RUN mvn dependency:go-offline
#What: Downloads all required JARs (Spring Boot, Hibernate, etc.) from the internet into the containers cache.
#Why: If you run mvn pachage later, maven won't have to donwload anything - it jjust compiles your code instantly.

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]


