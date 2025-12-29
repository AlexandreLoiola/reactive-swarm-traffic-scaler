PROJECT=upscaler

build:
	docker build -t java-backend ./backend
	docker build -t autoscaler ./autoscaler
	docker build -t nginx-lb ./nginx

deploy:
	docker stack deploy -c docker-compose.yml $(PROJECT)

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

start:
	docker service scale \
		upscaler_backend=2 \
		upscaler_nginx=1 \
		upscaler_autoscaler=1
