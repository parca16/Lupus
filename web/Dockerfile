# Build del cliente React/Vite
FROM node:22-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_WS_URL=
ENV VITE_WS_URL=$VITE_WS_URL
RUN npm run build

# Servidor estatico + proxy WebSocket
FROM nginx:1.27-alpine

ENV LUPUS_SERVER_URL=http://server:8080
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
