#!/bin/bash
set -euo pipefail
if [[ -z "${PG_TUNNEL_HOSTNAME:-}" ]]; then
    exec java -jar /app/app.jar
fi
: "${TUNNEL_SERVICE_TOKEN_ID:?Falta el token de Cloudflare Access}"
: "${TUNNEL_SERVICE_TOKEN_SECRET:?Falta el secreto de Cloudflare Access}"
cloudflared access tcp --hostname "$PG_TUNNEL_HOSTNAME" --url 127.0.0.1:15432 &
tunnel_pid=$!
java_pid=''
cleanup() {
    kill "$tunnel_pid" ${java_pid:+"$java_pid"} 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 143' TERM INT
for attempt in {1..100}; do
    if (echo > /dev/tcp/127.0.0.1/15432) 2>/dev/null; then break; fi
    kill -0 "$tunnel_pid"
    sleep 0.1
done
java -jar /app/app.jar &
java_pid=$!
wait -n "$tunnel_pid" "$java_pid"
# Si alguno termina, se detiene el otro para que el contenedor pueda reiniciarse.
exit 1
