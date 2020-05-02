# Step A
FROM spritsail/alpine-cmake:latest as build

COPY project /build
WORKDIR /build/make

RUN set -ex;    \
    cmake ..;   \
    make


# Step B
FROM alpine:latest as prod

WORKDIR /app/

COPY --from=build /usr/lib/*.so /usr/lib/
COPY --from=build /usr/lib/*.so.* /usr/lib/
COPY --from=build /build/make/docker-cpp .

WORKDIR /

ENTRYPOINT [ "/app/docker-cpp" ]
