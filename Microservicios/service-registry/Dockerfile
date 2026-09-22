# Multi-stage build para imagen ligera del Service Registry.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/service-registry-1.0.0.jar app.jar

# El registry es liviano: cap de heap bajo para no robar RAM a los servicios.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=40.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -Xss512k"

EXPOSE 8761
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
