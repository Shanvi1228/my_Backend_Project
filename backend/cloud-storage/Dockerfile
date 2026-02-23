FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy parent POM and install it to local repo
COPY pom.xml parent-pom.xml
RUN mvn install -f parent-pom.xml -N -q
# Build the module
WORKDIR /app/cloud-storage
COPY cloud-storage/pom.xml .
COPY cloud-storage/src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/cloud-storage/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
