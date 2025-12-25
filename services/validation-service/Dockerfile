FROM alpine:edge

# Install OpenJDK 21 and OpenSSL (Alpine Edge has recent OpenSSL 3.x)
RUN apk add --no-cache openjdk21 openssl

WORKDIR /app

COPY build/libs/validation-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
