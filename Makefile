PROJECT=upscaler

JMETER=jmeter  
TEST_PLAN=./performance/jmeter/teste-carga.jmx
RESULTS=./performance/jmeter/reports/results.jtl
HTML_REPORT=./performance/jmeter/reports/html

build:
	docker build -t java-backend ./backend
	docker build -t autoscaler ./autoscaler
	docker build -t nginx-lb ./nginx

deploy:
	@echo "Deploying stack $(PROJECT)..."
	@for i in 1 2 3 4 5; do \
		docker stack deploy -c docker-compose.yml $(PROJECT) && break; \
		echo "Deploy failed (attempt $$i). Retrying in 2s..."; \
		sleep 2; \
	done
	@if [ "$$(getenforce 2>/dev/null)" = "Enforcing" ]; then \
		echo "SELinux enforcing detected. Switching to permissive mode..."; \
		sudo setenforce 0; \
	else \
		echo "SELinux already permissive or disabled."; \
	fi

down:
	docker stack rm $(PROJECT)

rebuild: down build deploy

clear:
	docker stack rm $(PROJECT)
	docker image rm -f java-backend autoscaler nginx-lb || true
	docker network prune -fd

status:
	docker stack services $(PROJECT)

stop:
	docker service scale \
		upscaler_backend=0 \ 
		upscaler_nginx=0 \
		upscaler_autoscaler=0
	@echo "You can crush the flowers, but you can't stop the spring..."

start:
	docker service scale \
		upscaler_backend=10 \
		upscaler_nginx=1 \
		upscaler_autoscaler=1

test: start
	@echo "Running backend performance test..."
	$(JMETER) -n -t $(TEST_PLAN) -l $(RESULTS)
	@echo "Removing old HTML report..."
	@rm -rf $(HTML_REPORT)/*
	@echo "Generating new HTML report..."
	$(JMETER) -g $(RESULTS) -o $(HTML_REPORT)
	@echo "Report generated at $(HTML_REPORT)/index.html"
