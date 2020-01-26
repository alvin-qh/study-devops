FROM openjdk:8

WORKDIR /build

COPY files .

RUN set -ex;    \
    ./gradlew build

WORKDIR /

RUN set -ex;    \
    rm -rf /build
