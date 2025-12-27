FROM eclipse-temurin:21-jre-jammy

# Create secure directories for CA storage
RUN mkdir -p /secure/ca /secure/mtls && chmod 700 /secure/ca /secure/mtls

WORKDIR /app
COPY build/libs/ca-authority-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
