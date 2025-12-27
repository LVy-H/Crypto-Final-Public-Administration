FROM alpine:3.19

LABEL maintainer="gov-crypto-team"
LABEL description="SoftHSM2 for development/testing key storage"

# Install SoftHSM2 and tools
RUN apk add --no-cache \
    softhsm \
    opensc \
    bash

# Create directories
RUN mkdir -p /var/lib/softhsm/tokens \
    && mkdir -p /etc/softhsm \
    && mkdir -p /var/log/hsm

# Copy configuration
COPY softhsm2.conf /etc/softhsm/softhsm2.conf
COPY init-hsm.sh /init-hsm.sh
RUN chmod +x /init-hsm.sh

# Set environment
ENV SOFTHSM2_CONF=/etc/softhsm/softhsm2.conf
ENV PKCS11_LIB=/usr/lib/softhsm/libsofthsm2.so

# Expose token directory as volume for persistence
VOLUME ["/var/lib/softhsm/tokens"]

# Initialize HSM on startup
ENTRYPOINT ["/init-hsm.sh"]
