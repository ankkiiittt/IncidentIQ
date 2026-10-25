# syntax=docker/dockerfile:1
# Multi-stage Docker build for IncidentIQ Frontend (React 18 / TypeScript / Vite / Nginx)

# -------------------------------------------------------
# Stage 1: Build Frontend Assets
# -------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies layer
COPY package*.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build

# -------------------------------------------------------
# Stage 2: Production Nginx Server
# -------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

LABEL maintainer="IncidentIQ Engineering Team"
LABEL description="IncidentIQ - AI-Powered Enterprise Incident Intelligence Platform Frontend"

# Remove default nginx html
RUN rm -rf /usr/share/nginx/html/*

# Copy built distribution files from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration with reverse proxy & WebSocket support
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# Health check verifying root HTML is served
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget -q --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
