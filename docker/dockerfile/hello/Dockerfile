FROM alpine:latest

LABEL author="alvin" email="quhao317@163.com"

WORKDIR /opt

COPY project .

RUN set -ex; \
	chmod +x ./hello.sh

ENV USER_NAME=alvin

WORKDIR /

ENTRYPOINT ["/opt/hello.sh"]

