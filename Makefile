FRONTEND_DIST = node_modules/@hexlet/java-flight-booking-frontend/dist
JAR = build/libs/app.jar
CONSOLE = io.hexlet.flightbooking.Console
PORT ?= 8080

.PHONY: install build start db-migrate db-seed test-api lint lint-fix contract

install:
	./gradlew --console=plain -q classes
	npm install

build:
	rm -rf public/assets public/index.html
	mkdir -p public
	cp -R $(FRONTEND_DIST)/. public/
	./gradlew --console=plain -q shadowJar

start: db-migrate db-seed
	java -jar $(JAR)

db-migrate:
	java -cp $(JAR) $(CONSOLE) migrate

db-seed:
	java -cp $(JAR) $(CONSOLE) seed

test-api:
	./gradlew --console=plain test

test:
	rm -rf tmp/artifacts
	mkdir -p tmp/artifacts/contract
	cp contract/openapi.yaml tmp/artifacts/contract/openapi.yaml
	npm test

lint:
	./gradlew --console=plain -q spotlessCheck

lint-fix:
	./gradlew --console=plain -q spotlessApply

contract:
	npx tsp compile contract
