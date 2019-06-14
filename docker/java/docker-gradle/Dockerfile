FROM mcr.microsoft.com/java/jdk:8u212-zulu-alpine

WORKDIR /build

COPY . .

RUN set -ex;    \
    ./gradlew build

WORKDIR /

RUN set -ex;    \
    rm -rf /build
