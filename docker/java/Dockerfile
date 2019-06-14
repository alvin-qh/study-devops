# Javascript build
FROM node:8.16.0-alpine AS js-build

WORKDIR /build
COPY . .

WORKDIR /build/web

RUN set -ex;    \
    npm config set registry https://registry.npm.taobao.org;    \
    npm install;    \
    npm audit fix;  \
    npm run package


# Java build
FROM study/gradlew:5.2.1 AS java-build

WORKDIR /build
COPY --from=js-build /build/ .

RUN set -ex;    \
    ./gradlew bootjar


# Production
FROM mcr.microsoft.com/java/jdk:8u212-zulu-alpine AS product

RUN set -ex;    \
    mkdir -p /logs /data;   \
    chmod 777 /logs /data

WORKDIR /app

COPY --from=java-build /build/web/build/libs/alvin.docker*.jar ./alvin.docker.jar

ENTRYPOINT ["java", "-jar", "alvin.docker.jar"]

EXPOSE 8080
