.PHONY: build test run lint fmt
build: ; ./gradlew clean build

test-unit: ; ./gradlew :bot-gateway:test --tests "*unit.*"
test-integration: ; ./gradlew :bot-gateway:test --tests "*integration.*"
test-e2e: ; ./gradlew :bot-gateway:test --tests "*e2e.*"
test: test-unit test-integration test-e2e

run:   ; ./gradlew :bot-gateway:run

lint:  ; ./gradlew ktlintCheck detekt

fmt:   ; ./gradlew ktlintFormat
