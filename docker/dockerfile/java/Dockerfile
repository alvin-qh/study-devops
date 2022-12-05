# Java build
FROM gradle:jdk17 AS java-build

WORKDIR /build

COPY project .
# COPY --from=js-build /build/main /build/web/src/main

RUN set -ex; \
    gradle bootjar --no-daemon


# Production
FROM openjdk:17 AS product

WORKDIR /app

COPY --from=java-build /build/web/build/libs/alvin.study.docker*.jar ./alvin.study.docker.jar
COPY application.yml .

WORKDIR /

ENTRYPOINT ["java", "-jar", "/app/alvin.study.docker.jar", "--spring.config.location=classpath:/application.yml,/app/application.yml"]

VOLUME [ "/data", "/logs" ]

EXPOSE 8080
