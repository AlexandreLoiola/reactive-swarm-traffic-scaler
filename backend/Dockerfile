FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/backend.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]