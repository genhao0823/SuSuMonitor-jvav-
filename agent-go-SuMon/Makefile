.PHONY: build run test vet lint clean

build:
	go build -o bin/susumonitor-agent.exe cmd/susumonitor-agent/main.go

run:
	go run cmd/susumonitor-agent/main.go

test:
	go test ./...

vet:
	go vet ./...

lint: vet
	@echo "lint: use go vet for now"

clean:
	rm -rf bin/
