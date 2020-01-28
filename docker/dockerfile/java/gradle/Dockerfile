FROM openjdk:8

WORKDIR /build

COPY files .

RUN set -ex;    \
    ./gradlew build --no-daemon
    
WORKDIR /

RUN set -ex;    \
    rm -rf /build
