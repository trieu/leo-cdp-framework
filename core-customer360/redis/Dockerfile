# Redis 8 used as the customer360-api response cache.
FROM redis:8-alpine

COPY redis.conf /usr/local/etc/redis/redis.conf

EXPOSE 6379

# Password is supplied at container start via `--requirepass` (see
# docker-compose.yml `command:`), sourced from REDIS_PASSWORD in .env, so it
# never needs to be baked into the image/conf file.
HEALTHCHECK --interval=10s --timeout=5s --start-period=10s --retries=5 \
    CMD redis-cli -a "$REDIS_PASSWORD" --no-auth-warning ping | grep -q PONG || exit 1

ENTRYPOINT ["redis-server", "/usr/local/etc/redis/redis.conf"]
