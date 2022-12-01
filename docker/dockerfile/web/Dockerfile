# Build
FROM node AS build

WORKDIR /build

COPY project .

RUN set -ex; \
    yarn config set registry http://registry.npm.taobao.org/; \
    yarn install; \
    yarn run build

# Product
FROM nginx as prod

COPY project/nginx.conf /etc/nginx/nginx.conf
COPY project/web.conf /etc/nginx/conf.d/default.conf

WORKDIR /web

COPY --from=build /build/dist .

WORKDIR /

EXPOSE 80
