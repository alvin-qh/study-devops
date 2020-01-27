# Javascript build
FROM node:latest AS js-build

WORKDIR /build/src
COPY project/web/src/front .

WORKDIR /build/src

RUN set -ex;    \
    npm config set registry https://registry.npm.taobao.org;    \
    npm run setup;    \
    npm run package


# Java build
FROM study/gradlew:6.1.1 AS java-build

WORKDIR /build

COPY project .
COPY --from=js-build /build/main /build/web/src/main

RUN set -ex;    \
    ./gradlew bootjar


# Production
FROM openjdk:8 AS product

RUN set -ex;    \
    mkdir -p /logs /data;   \
    chmod 777 /logs /data

WORKDIR /app

COPY --from=java-build /build/web/build/libs/alvin.docker*.jar ./alvin.docker.jar
COPY application.yml .

ENTRYPOINT ["java", "-jar", "alvin.docker.jar", "--spring.config.location=classpath:/application.yml,./application.yml"]

EXPOSE 8080
