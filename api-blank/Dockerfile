FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src
RUN ./mvnw --batch-mode -DskipTests package

FROM cloudflare/cloudflared:2026.10.5 AS cloudflared

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/api-blank-0.0.1-SNAPSHOT.jar app.jar
COPY --from=cloudflared /usr/local/bin/cloudflared /usr/local/bin/cloudflared
COPY --chmod=755 scripts/container-entrypoint.sh /app/entrypoint.sh
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
