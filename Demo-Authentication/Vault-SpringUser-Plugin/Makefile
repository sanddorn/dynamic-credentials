VERSION=1.0.0-SNAPSHOT

ifndef GOARCH
	UNAME = $(shell uname -m)
	ifeq ($(UNAME), x86_64)
		GOARCH=amd64
	else
		GOARCH=$(UNAME)
	endif
endif

UNAME = $(shell uname -s)

ifndef GOOS
	ifeq ($(UNAME), Linux)
		GOOS = linux
	else ifeq ($(UNAME), Darwin)
		GOOS = darwin
	endif
endif

GOFILES=backend.go client.go path_config.go path_credentials.go path_roles.go user_token.go

.DEFAULT_GOAL := all

all: fmt build start

build: fmt $(GOFILES)
	CGO_ENABLED=0 GOOS=$(GOOS) GOARCH="$(GOARCH)" go build -o vault/plugins/vault-plugin-spring-boot cmd/vault-spring-user-plugin/main.go

start: build
	vault server -dev -dev-root-token-id=root -dev-plugin-dir=./vault/plugins

enable: start
	vault secrets enable -path=backenduser vault-plugin-spring-boot

clean:
	rm -f ./vault/plugins/vault-plugin-secrets-mock

fmt: $(GOFILES)
	go fmt $$(go list ./...)

docker: build
	docker build -t $(REGISTRY)vault:$(VERSION)

deploy: docker
	docker push $(REGISTRY)vault:$(VERSION)



.PHONY: build clean fmt start enable
