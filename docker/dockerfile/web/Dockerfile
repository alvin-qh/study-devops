# Build
FROM node:latest AS build

WORKDIR /build

COPY project .

RUN set -ex;            \
    npm config set registry https://registry.npm.taobao.org;  \
    npm run setup;      \
    npm run package

# Product
FROM nginx:latest as prod

COPY project/nginx.conf /etc/nginx/nginx.conf
COPY project/web.conf /etc/nginx/conf.d/default.conf

RUN set -ex;        \
    mkdir /logs;    \
    chmod 777 /logs

WORKDIR /web

COPY --from=build /build/dist .

EXPOSE 80
