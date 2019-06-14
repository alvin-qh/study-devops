FROM alpine:3.9.4

LABEL author="alvin" email="quhao317@163.com"

WORKDIR /opt

COPY ./hello.sh .
RUN set -ex;	\
	chmod +x ./hello.sh

ENTRYPOINT ["./hello.sh"]

