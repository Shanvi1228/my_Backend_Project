FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy parent POM and install it to local repo
COPY pom.xml parent-pom.xml
RUN mvn install -f parent-pom.xml -N -q
# Build the module
WORKDIR /app/collab-editor
COPY collab-editor/pom.xml .
COPY collab-editor/src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/collab-editor/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
