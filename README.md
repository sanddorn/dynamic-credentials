![CI](https://github.com/sanddorn/dynamic-credentials/actions/workflows/maven-main.yml/badge.svg)
[![Dynamic-Credentials](https://img.shields.io/endpoint?url=https://cloud.cypress.io/badge/simple/onz467&style=flat&logo=cypress)](https://cloud.cypress.io/projects/onz467/runs)
---
# Dynamic Credentials

You can find the demo project for dynamic credentials and slides of my talk here.

The source code is tested with:

* Apache Maven 3.8.6
* Java 17
* Kubernetes 1.24.0

## Steps

The different steps are located in separate directories. The demo application is based on Spring Boot, MonogDB with a thymeleaf frontend. Backend and Frontend are separate applications connected with a REST Service.

### Step 0 (Demo-Start)

You'll find the beginning of the journey in the `main` branch of the repository.
All credentials are stored in plain text in the `application.properties` or in the kubernetes secret resources.

### Step 1 (Demo-Database)

Vault with dynamic database credentials. 
The MongoDB Credentials are stored and rotated from vault. The essential new class is the `VaultSecretRotationConfiguration`.

### Step 2 (Demo-PKI)

Based on the Demo-Database, dynamic TLS certificates (and private key) is added. 
You should start to investigate with the class `VaultCertificateHandler`. This class modifies the HTTP-Server to use the newly generated key-pair.


### Step 3 (Demo-Auhtentication)

Based on the Demo-Database, the basic authentication between the frontend and the backend is added. 
For that, a MongoDB-based `UserDetailsService` was added. 
In the `frontend`-module a `VaultSecretRotationConfig` was added.
Additionally a `vault`-module was created in `Vault-SpringUser-Plugin`.



