#!/usr/bin/env bash
set -euo pipefail

HOST="${HOST:-s412939@helios.cs.ifmo.ru}"
PORT="${PORT:-2222}"
REMOTE_DIR="${REMOTE_DIR:-/home/studs/s412939/blps-project}"
JAR="${JAR:-advertising-system-1.0.0.jar}"
LOCAL_JAR="${LOCAL_JAR:-target/$JAR}"
REMOTE_TMP="${REMOTE_TMP:-/tmp/$JAR}"

if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME
  JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

mvn -B clean package

scp -P "$PORT" "$LOCAL_JAR" "$HOST:$REMOTE_TMP"

ssh -p "$PORT" "$HOST" "
  set -e
  cd '$REMOTE_DIR'

  pids=\$(ps -U \"\$USER\" -o pid,command 2>/dev/null | awk '\$2 ~ /java/ && /$JAR/ {print \$1}')
  if [ -n \"\$pids\" ]; then
    kill \$pids 2>/dev/null || true
    sleep 3
  fi

  cp '$REMOTE_TMP' '$JAR'
  rm -f '$REMOTE_TMP'
  sh ./start.sh

  for attempt in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30; do
    code=\$(curl -sS -o /dev/null -w '%{http_code}' http://127.0.0.1:18080/swagger-ui/index.html 2>/dev/null || true)
    if [ \"\$code\" = '200' ]; then
      echo \"Deployed. PID: \$(cat app.pid)\"
      exit 0
    fi
    sleep 2
  done

  echo 'Deploy failed: app did not become ready' >&2
  tail -80 app.log >&2
  exit 1
"
