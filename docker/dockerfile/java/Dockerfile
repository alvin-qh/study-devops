# Java build
FROM gradle:jdk11-jammy AS java-build

WORKDIR /build

COPY project .
# COPY --from=js-build /build/main /build/web/src/main

RUN set -ex; \
    gradle bootjar --no-daemon


# Production
FROM openjdk:11 AS product

WORKDIR /app

COPY --from=java-build /build/web/build/libs/alvin.study.docker*.jar ./alvin.study.docker.jar
COPY application.yml .

WORKDIR /

ENTRYPOINT ["java", "-jar", "/app/alvin.study.docker.jar", "--spring.config.location=classpath:/application.yml,/app/application.yml"]

EXPOSE 8080
