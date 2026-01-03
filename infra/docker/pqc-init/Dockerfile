FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
# Build the project and copy dependencies
RUN gradle :libs:common-crypto:copyDependencies :libs:common-crypto:jar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy artifact and dependencies
COPY --from=builder /app/libs/common-crypto/build/libs/common-crypto-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --from=builder /app/libs/common-crypto/build/libs/dependencies /app/libs

# Copy entrypoint
COPY infra/docker/pqc-init/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# Entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]
