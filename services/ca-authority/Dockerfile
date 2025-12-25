FROM alpine:edge

# Install OpenJDK 21 and OpenSSL (Alpine Edge usually has very recent OpenSSL)
RUN apk add --no-cache openjdk21 openssl

WORKDIR /app

COPY build/libs/ca-authority-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
