# Gebruik Maven + Java 17 image
FROM maven:3.9.3-eclipse-temurin-17 AS build

WORKDIR /app

# Kopieer pom en source
COPY pom.xml .
COPY src ./src

# Build project
RUN mvn clean package -DskipTests

# Productie image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Kopieer jar van build stage
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]


