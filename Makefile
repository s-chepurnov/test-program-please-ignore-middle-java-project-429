FRONTEND_DIST=node_modules/@hexlet/java-flight-booking-frontend/dist

.PHONY: build contract

install:
	npm ci
	./gradlew compileJava

build:
	rm -rf src/main/resources/public/assets src/main/resources/public/index.html
	mkdir -p src/main/resources/public
	rsync -av $(FRONTEND_DIST)/ src/main/resources/public/
	./gradlew bootJar

start:
	./gradlew bootRun

contract:
	npx tsp compile contract

test:
	./gradlew test
