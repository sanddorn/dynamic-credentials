# Dynamic Credentials

You can find the demo project for dynamic credentials and slides of my talk here.

The source code is tested with:

* Apache Maven 3.8.4
* Java 17
* Kubernetes 1.24.0

## Step 2 (Demo-PKI)

This will use the database and the PKI module from vault. 

#### Installation

Create a minikube (or full blown kubernetes cluster). Find details for minikube on https://minikube.sigs.k8s.io/docs/

Create a namespace for your test:

```shell
kubectl create namespace dynamic-credentials
```

If you want to use a local mongodb, you may use the `Deployment/values-minikube-mongodb.yaml` as an example for
the `bitnami/mongodb` helm chart. You can also use a standalone database or the mongodb cloud database.

Now for the vault container
```shell
helm repo add hashicorp https://helm.releases.hashicorp.com
helm repo update   
helm upgrade --install -n dynamic-credentials vault hashicorp/vault -f values-minikube-vault.yaml
```

When using the hashicorp helm start, you'll have to `vault operator init` and then unseal. It is supposed, that you are quite familiar with the start-up of a vault server. 

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
```

Now configure the PKI system use the following commands:
```shell
vault secrets enable pki
vault secrets tune -max-lease-ttl=87600h pki
vault write pki/root/generate/internal \
    common_name=cluster.local \
    issuer_name=root-2022 \
    ttl=87600h
vault write pki/roles/backend \
    allow_any_name=true \
    issuer_ref=root-2022 \
    max_ttl=15m
```
You should either use the CA certificate generated in the 3rd vault command or you can retrieve the certificate from the vault by
```shell
curl -v --header "X-Vault-Token: $VAULT_TOKEN" --request GET http://localhost:8200/v1/pki/cert/ca   
```

Create a truststore with this certificate:
```shell
keytool -import -file ./ca.pem -alias vaultca -keystore truststore 
```

You'll have to edit the `./Deployment/values-minikube.yaml` and add the key `frontend.truststore` as it is commented out in the file.


### Install the Application
Now you can deploy validate and adapt the `Deployment/values.minikube.yaml` and deploy backend and frontend with the helm charts from `Deployment/backend`and `Deployment/frontend`

Find the Hero application on `frontend.credentials/hero`.



