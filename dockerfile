FROM eclipse-temurin
COPY target/Wordpress-Importer-Service-0.0.1-SNAPSHOT.jar wordpress-importer-service.jar
ENTRYPOINT ["java","-jar","/wordpress-importer-service.jar","--spring.profiles.active=prod"]