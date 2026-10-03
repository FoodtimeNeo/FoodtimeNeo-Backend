FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S foodtime && adduser -S foodtime -G foodtime \
    && mkdir -p /app/logs && chown -R foodtime:foodtime /app
COPY --from=build --chown=foodtime:foodtime /workspace/target/foodtime-neo-backend.jar app.jar
USER foodtime
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
