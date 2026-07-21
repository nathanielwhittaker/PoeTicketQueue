# --- Stage 1: build the Vue frontend ---
FROM node:20-alpine AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# --- Stage 2: build the Spring Boot backend, embedding the built frontend as static resources ---
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /backend
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# --- Stage 3: minimal runtime image ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /backend/target/*.jar app.jar
EXPOSE 8080
# No actuator dependency, so this hits "/" — the SpaWebConfig fallback serves index.html there at 200
# once the app is actually up; wget is Alpine's busybox applet, no extra install needed.
HEALTHCHECK --interval=30s --timeout=3s --start-period=25s --retries=3 \
  CMD wget -q --spider http://localhost:8080/ || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
