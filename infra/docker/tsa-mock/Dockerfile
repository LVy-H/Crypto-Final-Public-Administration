FROM alpine:3.19

LABEL maintainer="gov-crypto-team"
LABEL description="RFC 3161 Timestamp Authority mock for development/testing"

# Install OpenSSL for TSA operations
RUN apk add --no-cache openssl bash

# Create directories
RUN mkdir -p /tsa/certs /tsa/data /tsa/logs

# Copy configuration and startup script
COPY tsa.cnf /tsa/tsa.cnf
COPY init-tsa.sh /init-tsa.sh
RUN chmod +x /init-tsa.sh

WORKDIR /tsa

# TSA listens on port 8318 (typical TSA port)
EXPOSE 8318

# Initialize and start TSA
ENTRYPOINT ["/init-tsa.sh"]
