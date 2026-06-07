# syntax=docker/dockerfile:1
# Imagem única (1 serviço) para o Railway:
#   builda o React -> embute no Spring Boot -> roda o .jar.
# O backend serve a API, o WebSocket e a própria SPA na mesma origem.

# 1) Frontend (React + Vite) -> /frontend/dist
FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# 2) Backend (Spring Boot) — embute a SPA em /static e empacota o .jar
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY asus-platform/pom.xml ./
RUN mvn -B -ntp dependency:go-offline || true
COPY asus-platform/src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static
RUN mvn -B -ntp -DskipTests clean package

# 3) Runtime enxuto (só JRE + o .jar)
FROM eclipse-temurin:21-jre
WORKDIR /app
# Produção usa PostgreSQL (variáveis DB_* abaixo). Pode sobrescrever no painel.
ENV SPRING_PROFILES_ACTIVE=postgres
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
# MaxRAMPercentage ajuda em containers com pouca memória (free tier).
ENTRYPOINT ["sh","-c","java -XX:MaxRAMPercentage=75.0 -jar app.jar"]
