# Step A
FROM spritsail/alpine-cmake as build

COPY project /build
WORKDIR /build/make

RUN set -ex; \
    cmake ..; \
    make


# Step B
FROM alpine as prod

WORKDIR /app/

COPY --from=build /usr/lib/*.so /usr/lib/
COPY --from=build /usr/lib/*.so.* /usr/lib/
COPY --from=build /build/make/docker-cpp .

WORKDIR /

ENTRYPOINT [ "/app/docker-cpp" ]
