# Multi-stage build para imagen ligera y menor consumo de RAM en runtime.
# Etapa 1: build
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# Etapa 2: runtime minimo (JRE alpine)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/facturacion-service-1.0.0.jar app.jar

# Flags JVM para operar bien dentro de un pod que comparte 5GB con
# observabilidad y el sidecar del service mesh. Cap del heap al 50% del
# limite del contenedor y GC de baja huella (SerialGC para cargas pequenas).
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50.0 -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m -Xss512k -XX:+ExitOnOutOfMemoryError"

EXPOSE 8088
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
