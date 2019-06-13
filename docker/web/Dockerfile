# Build
FROM node:8.16.0-alpine AS build

WORKDIR /build

COPY . .

RUN set -ex;            \
    npm config set registry https://registry.npm.taobao.org;  \
    npm run setup;      \
    npm audit fix;		\
    npm run package

# Product
FROM nginx:1.17.0-alpine as prod

COPY nginx.conf /etc/nginx/nginx.conf
COPY web.conf /etc/nginx/conf.d/default.conf

RUN set -ex;        \
    mkdir /logs;    \
    chmod 777 /logs

WORKDIR /web

COPY --from=build /build/out .

EXPOSE 80
