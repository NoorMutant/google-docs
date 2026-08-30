# Three stages so the final image carries only a JRE and one jar.
# The Angular build is copied into the Spring Boot static folder, which means
# the whole product ships and deploys as a single artifact on one port.

FROM node:22-alpine AS frontend
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
# Dependencies are resolved before the sources are copied so that a code change
# does not invalidate the downloaded Maven cache layer.
COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /build/dist/frontend/browser/ ./src/main/resources/static/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend /build/target/docs-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
