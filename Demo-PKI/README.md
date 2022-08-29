# Dynamic Credentials Demo Project

You can find the demo project for dynamic credentials.

The source code is tested with:

* Apache Maven 3.8.6
* Java 17
* docker-compose 1.29.2

#### Installation

You can use the `docker-compose.yaml` to start the application. 

It's a bit tricky this time, as you'll need the CA-Certificate from vault, to start the frontend (as a client to a vault-provided certificate).

So, after the start of the application, you'll have to edit the `docker-compose.yaml` and change the `TRUSTED_CERTIFICATE` to the new root certificate created in your vault instance. After correction of the environment you'll have to restart the frontend-container (and only this container!)

As the configuration for the PKI is recreated each time the `vault_provisioner` is run, be careful with a full `docker-compose` restart.

