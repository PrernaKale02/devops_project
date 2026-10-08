FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN groupadd --system spring && useradd --system --gid spring --create-home spring \
    && mkdir -p /app/data \
    && chown -R spring:spring /app
COPY --from=build --chown=spring:spring /workspace/target/child-education-sponsorship-0.1.0.jar /app/app.jar

USER spring:spring
EXPOSE 8001
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--server.address=0.0.0.0"]
