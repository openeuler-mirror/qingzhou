FROM apline AS basejdk
RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories
RUN apk update && apk upgrade
RUN apk add --no-cache \
    busybox-extras \
    ca-certificates \
    dumb-init \
    fontconfig \
    ttf-dejavu \
    tzdata
RUN cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone
RUN apk del tzdata
RUN fc-cache -fv

RUN apk add --no-cache openjdk8
RUN rm -rf /var/cache/apk/* && rm -rf /tmp/* /var/tmp/*


FROM basejdk
RUN adduser -u 1000 -D qingzhou
USER qingzhou
COPY --chown=qingzhou:qingzhou target/qingzhou /opt/qingzhou
WORKDIR /opt/qingzhou
EXPOSE 9000
ENTRYPOINT ["sh","./bin/start.sh"]