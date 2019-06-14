# 
FROM spritsail/alpine-cmake:3.9 as build

COPY . /build
WORKDIR /build/make

RUN set -ex;    \
    cmake ..;   \
    make


# B
FROM alpine:3.9.4 as prod

WORKDIR /app/

COPY --from=build /usr/lib/*.so /usr/lib/
COPY --from=build /usr/lib/*.so.* /usr/lib/
COPY --from=build /build/make/docker-cpp .

ENTRYPOINT [ "./docker-cpp" ]
