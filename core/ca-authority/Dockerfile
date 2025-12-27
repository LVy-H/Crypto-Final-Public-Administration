FROM eclipse-temurin:21-jre-jammy

# Create secure directories for CA storage
RUN mkdir -p /secure/ca /secure/mtls && chmod 755 /secure/ca && chmod 777 /secure/mtls

WORKDIR /app
COPY build/libs/ca-authority-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
