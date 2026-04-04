# Development Dockerfile for Sokybot Backend
FROM eclipse-temurin:21-jdk

# Install debugging, networking, and watching tools
RUN apt-get update && apt-get install -y \
    netcat-openbsd \
    procps \
    curl \
    iproute2 \
    inotify-tools \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Maven cache mount point (compose binds host ~/.m2 here for non-root user)
RUN mkdir -p /var/maven/.m2

# Expose ports:
# 8182: Backend API (RSocket)
# 5005: Java Debug Port
# 8101: Karaf Shell (SSH)
EXPOSE 8182 5005 8101

# The execution is handled via docker-compose by mounting the project root
# and running the soky script in foreground mode with optional watching.
