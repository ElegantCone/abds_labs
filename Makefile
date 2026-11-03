.PHONY: run rund services stop clean gen
.DEFAULT_GOAL = run

# Runs all services in detached mode.
run:
	docker compose --env-file .env.local up -d --build

# Runs all services without detached mode (for debugging).
rund:
	docker compose --env-file .env.local up --build

# Shows all service statuses.
services:
	docker compose ps

# Stops all running services.
stop:
	docker compose down

# Cleans up all resources including volumes.
clean:
	docker compose down -v

# Настройка и прогрев системы.
# Создаст схему БД, сгенерирует и загрузит все датасеты в таблицы.
setup: schema gen load-datasets

# Очистка системы
teardown: .drop-ch

# Создаёт схему базы данных.
schema:
	@echo "Создаём таблицы в ClickHouse..."
	@./initdb/scripts/run_sql.sh \
		initdb/ddl/dim_gauges.sql \
		initdb/ddl/dim_realty.sql \
		initdb/ddl/dim_tariffs.sql \
		initdb/ddl/dim_users.sql \
		initdb/ddl/fact_gauge_bindings.sql \
		initdb/ddl/fact_invoice.sql \
		initdb/ddl/fact_meter_readings.sql

.drop-ch:
	@echo "Удаляем таблицы в ClickHouse..."
	@./initdb/scripts/run_sql.sh \
		initdb/dml/drop.sql

# Загружает датасеты во все таблицы
# с предварительной очисткой базы и пересозданием схемы.
load-datasets: teardown schema
	@for t in dim_users dim_realty dim_gauges dim_tariffs fact_gauge_bindings fact_meter_readings fact_invoice; do \
		echo "Загружаем $$t..."; \
		docker exec -i clickhouse clickhouse-client --user admin --database energosbyt \
			--query "$$(cat initdb/dml/load/$$t.sql)" < data/$$t.csv || exit 1; \
	done

# Генерирует датасеты в data/.
gen:
	./gradlew --no-daemon generateData
