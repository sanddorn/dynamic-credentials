# Vault Plugin For Spring Boot UserDetailsService

This plugin creates documents in a mongoDB, which represent a user entry for the Spring Boot UserDetailsService.

## Build

Use the `Makefile` for building the plugin. You can cross-build by setting `GOOS` and `GOARCH` like

```shell
env GOOS=linux GOARCH=arm64 make build
```

## Run

If you want to try the plugin, you may use the `Makefile` like

```shell
make start
```

and

```shell
make enable 
```

Please remember, the plugin is not configurable with the `vault-ui`. It can configured via CLI or HTTP-API. (See
#configuration for details)

## Deployment

To use the plugin in a non-development environment, you'll have to start the `vault` server with a configured
plugin-directory. Then you can copy the plugin into the plugin directory. To start the plugin, it must be configured
into the plugins catalog with

```shell
export SHASUM=`shasum -a 256 vault/plugins/vault-plugin-spring-boot | cut -f1 -d ' '`
vault write sys/plugins/catalog/secret/vault-plugin-spring-boot sha256="${SHASUM}" command=vault-plugin-spring-boot
```

# Configuration

As any other vault secrets plugin, you'll first have to enable the plugin:

```shell
vault secrets enable -path=backenduser vault-plugin-spring-boot
```

When enabled, you can configure the mongoDB Connection:

```shell
vault write backenduser/config username=admin password=admin url=mongodb://database:27017
```

and create a role:

```shell
vault write backenduser/role/hero database=hero collection=user roles=USER class=de.bermuda.hero.backend.UserEntry ttl=300
```

Now you can get credentials from the plugin:

```shell
curl -v -H "X-Vault-Token: root"  http://localhost:8200/v1/backenduser/creds/hero
*   Trying 127.0.0.1:8200...
* Connected to localhost (127.0.0.1) port 8200 (#0)
> GET /v1/backenduser/creds/hero HTTP/1.1
> Host: localhost:8200
> User-Agent: curl/7.79.1
> Accept: */*
> X-Vault-Token: root
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 200 OK
< Cache-Control: no-store
< Content-Type: application/json
< Strict-Transport-Security: max-age=31536000; includeSubDomains
< Date: Wed, 17 Aug 2022 13:03:24 GMT
< Content-Length: 303
< 
{"request_id":"890d352b-44e0-ca0d-d2c2-f28a26323c52","lease_id":"backenduser/creds/hero/T3QYOzJXVvn0ESallAKTxKnG","renewable":true,"lease_duration":2764800,"data":{"password":"Q0e\u0026(SGa19naF7qd","token":"usrWzkYORuHCfnfQ","username":"usrWzkYORuHCfnfQ"},"wrap_info":null,"warnings":null,"auth":null}
```

The plugin will create user entries like:

```json
{
  "_id": "usrQbJqPH9fiacuW",
  "password": "@kwCF(r3Eu6RDMH8",
  "roles": [
    "USER"
  ],
  "_class": "de.bermuda.hero.backend.UserEntry"
}
```