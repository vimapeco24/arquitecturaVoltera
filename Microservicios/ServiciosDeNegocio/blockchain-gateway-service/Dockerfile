# Multi-stage build para imagen ligera (mismo patron que el resto de servicios).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/blockchain-gateway-service-1.0.0.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8101
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
