SHELL := /bin/bash
.DEFAULT_GOAL := help

POSTGRES_SERVICE ?= postgresql@15
NODE_VERSION ?= 22

.PHONY: help dev db backend frontend

help:
	@echo "make dev       Start PostgreSQL, backend, and frontend (Ctrl+C to stop app servers)"
	@echo "make backend   Start PostgreSQL and backend only"
	@echo "make frontend  Start frontend using Node $(NODE_VERSION)"
	@echo "make db        Start local PostgreSQL (left running when app servers stop)"

dev: db
	+$(MAKE) --jobs=2 backend frontend

db:
	brew services start $(POSTGRES_SERVICE)

backend: db
	cd backend && set -a && { [ ! -f .env ] || . ./.env; } && set +a && ./mvnw spring-boot:run

frontend:
	@export NVM_DIR="$${NVM_DIR:-$$HOME/.nvm}"; \
	if [ ! -s "$$NVM_DIR/nvm.sh" ]; then \
		echo "Cannot find nvm at $$NVM_DIR/nvm.sh. Set NVM_DIR to your nvm installation."; exit 1; \
	fi; \
	. "$$NVM_DIR/nvm.sh"; \
	nvm use $(NODE_VERSION) && cd frontend && npm start
