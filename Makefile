ENV_FILE ?= .env.local
DATA_DIR ?= $(CURDIR)/data
export ENV_FILE := $(abspath $(ENV_FILE))
export DATA_DIR := $(abspath $(DATA_DIR))
COMPOSE = docker compose --env-file "$(ENV_FILE)"

.PHONY: run rund stop gen setup teardown load
.DEFAULT_GOAL := run

run:
	@if [ -n "$$GITHUB_ENV" ]; then \
		printf 'COMPOSE_ENV_FILES=%s\n' "$(ENV_FILE)" >> "$$GITHUB_ENV"; \
	fi
	$(COMPOSE) up -d --build

rund:
	$(COMPOSE) up --build

stop:
	$(COMPOSE) down

setup:
	$(MAKE) gen
	$(MAKE) load

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

# Генерирует датасеты в data/.
gen:
	mkdir -p "$(DATA_DIR)"
	$(COMPOSE) run --rm --build --no-deps --user "$$(id -u):$$(id -g)" \
		--volume "$(DATA_DIR):/data" --entrypoint java backend -jar generator.jar /data

load: teardown schema
	@for t in dim_users dim_realty dim_gauges dim_tariffs fact_gauge_bindings fact_meter_readings fact_invoice; do \
		echo "Загружаем $$t..."; \
		docker exec -i clickhouse clickhouse-client --user admin --database energosbyt \
			--query "$$(cat initdb/dml/load/$$t.sql)" < data/$$t.csv || exit 1; \
	done

teardown:
	@echo "Удаляем таблицы в ClickHouse..."
	@./initdb/scripts/run_sql.sh \
		initdb/dml/drop.sql