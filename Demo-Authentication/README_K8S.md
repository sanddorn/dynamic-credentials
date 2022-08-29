# Dynamic Credentials

You can find the demo project for dynamic credentials and slides of my talk here.

The source code is tested with:

* Apache Maven 3.8.4
* Java 17
* Kubernetes 1.24.0

## Steps

### Step 0

You'll find the beginning of the journey in the `main` branch of the repository. All credentials are stored in plain
text in the `application.properties` or in the kubernetes secret resources.

#### Installation

Create a minikube (or full blown kubernetes cluster). Find details for minikube on https://minikube.sigs.k8s.io/docs/

Create a namespace for your test:

```shell
kubectl create namespace dynamic-credentials
```

If you want to use a local mongodb, you may use the `Deployment/values-minikube-mongodb.yaml` as an example for
the `bitnami/mongodb` helm chart. You can also use a standalone database or the mongodb cloud database. You'll need a
db `hero` and a user to login to this database:

```json
db.createUser(
    {
      user: "hero",
      password: "hero1234!",
      roles: [
        {role: "readWrite", db: "hero"}
      ]
    }
)
```

Now you can deploy validate and adapt the `Deployment/values.minikube.yaml` and deploy backend and frontend with the helm charts from `Deployment/backend`and `Deployment/frontend` 

Find the Hero application on `frontend.credentials/hero`.

### Step 1

Vault with dynamic database passwords.


#### Installation

You may use the previous installation of step 0. You'll have to create a vault-container with the plugin:
```shell
cd Vault-SpringUser-Plugin 
REGISTRY=<<YOUR REGISTRY/>> make deploy
```

Adapt the `values-minikube-vault.yaml` to match your registry, then you can install the vault-chart to your minikube:

```shell
helm upgrade --install -n dynamic-credentials vault hashicorp/vault -f values-minikube-vault.yaml
```

When using the hashicorp helm start, you'll have to `vault operator init` and then unseal. It is supposed, that you are quite familiar with the start-up of a vault server. 

To add the plugin into the plugin catalog, the `sha256` is needed:
```shell
sha256sum vault/plugins/vault-plugin-spring-boot
```

With the generated checksum, the plugin can be added:
```shell
vault write sys/plugins/catalog/secret/vault-plugin-spring-boot sha256="<<SHA265>>" command=vault-plugin-spring-boot
```

Create necessary vault-resources:
```shell
echo 'path "*" { capabilities = ["create", "read", "update", "delete", "list"]}' | vault  policy write application_policy - 
vault auth enable kubernetes
vault write auth/kubernetes/config token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" kubernetes_host="https://$KUBERNETES_PORT_443_TCP_ADDR:443" kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt issuer="\"https://kubernetes.default.svc.cluster.local\""
vault write auth/kubernetes/role/backend bound_service_account_names=backend bound_service_account_namespaces=dynamic-credentials policies=default,application_policy ttl=1h
vault write auth/kubernetes/role/frontend bound_service_account_names=frontend bound_service_account_namespaces=dynamic-credentials policies=default,application_policy ttl=1h
vault secrets enable -version=2 -path=secret kv
vault kv put secret/frontend USERNAME=admin PASSWORD=admin
vault kv put secret/backend USERNAME=admin PASSWORD=admin
vault secrets enable database
vault write database/config/mongo \
    plugin_name=mongodb-database-plugin \
    allowed_roles="*" \
    connection_url="mongodb://{{username}}:{{password}}@mongodb:27017/admin" \
    username="root" \
    password="rootPassword1"
vault write database/roles/backend \
    db_name=mongo \
    creation_statements='{ "db": "hero", "roles": [{ "role": "readWrite" }] }' \
    default_ttl="300" \
    max_ttl="300"
vault secrets enable -path=backenduser vault-plugin-spring-boot
vault write backenduser/config username=root password=rootPassword1 url=mongodb://mongodb:27017
vault write backenduser/role/hero database=hero collection=user roles=USER class=de.bermuda.hero.backend.UserEntry ttl=300
```



