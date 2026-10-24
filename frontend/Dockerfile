# Multi-stage Dockerfile for React/Vite Frontend

# Stage 1: Build React application
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency definitions and install
COPY package*.json ./
RUN npm ci

# Copy source code and build production bundle
COPY . .
RUN npm run build

# Stage 2: Serve static files with Nginx
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
