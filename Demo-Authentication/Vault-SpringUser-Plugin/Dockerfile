FROM hashicorp/vault:1.10.3

RUN mkdir /plugins
COPY vault/plugins/vault-plugin-spring-boot /plugins
RUN chown -R vault:vault plugins

