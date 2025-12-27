FROM openjdk:21-slim

LABEL maintainer="gov-crypto-team"
LABEL description="RFC 3161 Timestamp Authority mock for development/testing"

# Install OpenSSL for TSA operations
RUN apt-get update && apt-get install -y --no-install-recommends \
    openssl \
    && rm -rf /var/lib/apt/lists/*

# Create directories
RUN mkdir -p /tsa/certs /tsa/data

# Copy configuration and startup script
COPY tsa.cnf /tsa/tsa.cnf
COPY init-tsa.sh /init-tsa.sh
RUN chmod +x /init-tsa.sh

WORKDIR /tsa

# TSA listens on port 8318 (typical TSA port)
EXPOSE 8318

# Initialize and start TSA
ENTRYPOINT ["/init-tsa.sh"]
CMD ["tail", "-f", "/dev/null"]
