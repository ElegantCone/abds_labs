#!/bin/sh
# Выполняет SQL-скрипты в ClickHouse, развёрнутом в Docker (docker-compose.yml).
# Скрипты выполняются в базе energosbyt. Если базы нет, скрипт её создаёт.
# Использование: ./run_sql.sh path/to/1.sql path/to/2.sql ...

set -u

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE="$PROJECT_DIR/.env.local"

if [ ! -r "$ENV_FILE" ]; then
    echo "Error: не найден $ENV_FILE" >&2
    exit 1
fi

# Загружаем и экспортируем переменные
set -a
. "$ENV_FILE"
set +a

CONTAINER="clickhouse"
CH_USER="${CLICKHOUSE_USER:?CLICKHOUSE_USER is not set}"
CH_PASSWORD="${CLICKHOUSE_PASSWORD:?CLICKHOUSE_PASSWORD is not set}"
CH_DB="${CLICKHOUSE_DATABASE:?CLICKHOUSE_DATABASE is not set}"

if [ $# -eq 0 ]; then
    echo "Usage: $(basename "$0") path/to/1.sql path/to/2.sql ..." >&2
    exit 1
fi

# Проверяем, что все переданные файлы существуют и читаются (до выполнения, чтобы не выполнять частично)
for f in "$@"; do
    if [ ! -r "$f" ]; then
        echo "Error: файл не найден или не читается: $f" >&2
        exit 1
    fi
done

# Проверяем, что контейнер ClickHouse запущен
if [ "$(docker inspect -f '{{.State.Running}}' "$CONTAINER" 2>/dev/null)" != "true" ]; then
    echo "Error: контейнер '$CONTAINER' не запущен. Поднимите его: docker compose -f docker-compose.yml up -d" >&2
    exit 1
fi

# Создаём базу, если её ещё нет
if ! docker exec "$CONTAINER" clickhouse-client --user "$CH_USER" --query "CREATE DATABASE IF NOT EXISTS $CH_DB"; then
    echo "Error: не удалось создать базу $CH_DB" >&2
    exit 1
fi

ok=0
for f in "$@"; do
    echo "==> Выполняю: $f"
    if ! docker exec -i "$CONTAINER" clickhouse-client --user "$CH_USER" --database "$CH_DB" --multiquery < "$f"; then
        echo "Error: не удалось выполнить $f" >&2
        exit 1
    fi
    ok=$((ok + 1))
done

echo "Готово: успешно выполнено скриптов: $ok из $#"