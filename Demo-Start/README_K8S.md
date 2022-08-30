# Dynamic Credentials

You can find the demo project for dynamic credentials and slides of my talk here.

The source code is tested with:

* Apache Maven 3.8.6
* Java 17
* Kubernetes 1.24.0

## Step 0 (Start)

You'll find the beginning of the journey in the `main` branch of the repository. All credentials are stored in plain
text in the `application.properties` or in the kubernetes secret resources.

### Installation

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


