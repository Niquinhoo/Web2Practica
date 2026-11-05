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
COPY --from=build /build/target/api-blank-0.0.1-SNAPSHOT.jar /tmp/app.jar
RUN java -Djarmode=tools -jar /tmp/app.jar extract --destination /app && rm /tmp/app.jar
# AOT Cache para acortar el arranque en frío. El entrenamiento corre sin base: solo carga el contexto de Spring.
# G1 y -Xmx fijos: el JDK solo guarda objetos de heap con G1 y el cache exige compressed oops también al ejecutar.
RUN java -XX:AOTCacheOutput=/app/app.aot -XX:+UseG1GC -Xmx512m \
    -Dspring.context.exit=onRefresh \
    -Dspring.flyway.enabled=false \
    -Dspring.jpa.hibernate.ddl-auto=none \
    -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
    -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
    -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
    -jar /app/app.jar
COPY --from=cloudflared /usr/local/bin/cloudflared /usr/local/bin/cloudflared
COPY --chmod=755 scripts/container-entrypoint.sh /app/entrypoint.sh
USER 10001:10001
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
