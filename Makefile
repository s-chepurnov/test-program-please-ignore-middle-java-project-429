FRONTEND_DIST=node_modules/@hexlet/java-flight-booking-frontend/dist

.PHONY: build

make install:
	npm ci
	./gradlew compileJava

make build:
	rm -rf src/main/resources/public/assets src/main/resources/public/index.html
	mkdir -p src/main/resources/static
	rsync -av $(FRONTEND_DIST)/ src/main/resources/public/
	./gradlew bootJar

make start:
	./gradlew bootRun
