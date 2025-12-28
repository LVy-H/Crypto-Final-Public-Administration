# Custom nginx-ingress-controller with OpenSSL 3.5+ for X25519MLKEM768 PQC TLS
# Base: Alpine 3.22 (ships OpenSSL 3.5.x)

FROM alpine:3.22

# Install nginx and OpenSSL 3.5+ (Alpine 3.22 includes OpenSSL 3.5.x)
RUN apk add --no-cache \
    nginx \
    nginx-mod-http-lua \
    openssl \
    curl \
    ca-certificates \
    libcap \
    tzdata \
    diffutils \
    && rm -rf /var/cache/apk/*

# Create nginx user
RUN adduser -D -u 101 -G nginx -s /sbin/nologin www-data

# Verify OpenSSL version supports X25519MLKEM768
RUN openssl version && \
    echo "Checking X25519MLKEM768 support:" && \
    openssl list -tls1_3-kex-algorithms 2>/dev/null | head -20 || true

# Setup directories
RUN mkdir -p /etc/nginx/ssl /var/log/nginx /var/lib/nginx /run/nginx \
    && chown -R www-data:nginx /var/log/nginx /var/lib/nginx /run/nginx

# Copy nginx configuration (will be mounted)
COPY nginx.conf /etc/nginx/nginx.conf.default

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]
