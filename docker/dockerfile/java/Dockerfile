# Javascript build
FROM node AS js-build

WORKDIR /build/src
COPY project/web/src/front .

WORKDIR /build/src

RUN set -ex; \
    npm config set registry https://registry.npm.taobao.org; \
    npm run setup; \
    npm run package


# Java build
FROM gradle:6.1.1-jdk8 AS java-build

WORKDIR /build

COPY project .
COPY --from=js-build /build/main /build/web/src/main

RUN set -ex; \
    gradle bootjar --no-daemon


# Production
FROM openjdk:8-jre-alpine AS product

WORKDIR /app

COPY --from=java-build /build/web/build/libs/alvin.docker*.jar ./alvin.docker.jar
COPY application.yml .

WORKDIR /

ENTRYPOINT ["java", "-jar", "/app/alvin.docker.jar", "--spring.config.location=classpath:/application.yml,./application.yml"]

EXPOSE 8080
