# Docker Hub's official image through Google's mirror, which has no anonymous pull limit.
FROM mirror.gcr.io/library/alpine:3.22
RUN apk add --no-cache ca-certificates tzdata && addgroup -g 10001 web && adduser -D -u 10001 -G web web
WORKDIR /app
COPY vervetwodo-web /app/vervetwodo-web
COPY dist /app/dist
ENV LISTEN_ADDR=0.0.0.0:8080 DATA_DIR=/data ASSET_DIR=/app/dist
USER 10001:10001
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=3s --start-period=30s CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
ENTRYPOINT ["/app/vervetwodo-web"]
