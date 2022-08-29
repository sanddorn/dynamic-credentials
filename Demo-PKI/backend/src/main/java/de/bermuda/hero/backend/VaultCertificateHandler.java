package de.bermuda.hero.backend;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultPkiOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.CertificateBundle;
import org.springframework.vault.support.VaultCertificateRequest;
import org.springframework.vault.support.VaultCertificateResponse;

@Configuration
@ConditionalOnProperty("spring.cloud.vault.enabled")
public class VaultCertificateHandler implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>,
        DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(VaultCertificateHandler.class);

    private static final String ROTATING_KEY = "rotatingKey";
    private static final String KEYSTORE_FILENAME = "server-key.pkcs12";
    private static final String KEYSTORE_PASSPHRASE = "123456";

    private final VaultTemplate vaultTemplate;
    private final String commonName;
    private final String pkiRoleName;
    private final int port;
    private final TaskScheduler taskScheduler;
    private final ApplicationContext applicationContext;
    private final Ssl ssl;

    private Date certificateNotAfter;
    private BigInteger certificateSerial;


    public VaultCertificateHandler(VaultTemplate vaultTemplate,
                                   @Value("${kubernetes.service.name:localhost}") String commonName,
                                   @Value("${spring.cloud.vault.pki.role-name}") String pkiRoleName,
                                   @Value("${server.port:8443}") int port,
                                   ApplicationContext applicationContext
    ) {

        Objects.requireNonNull(pkiRoleName);
        this.vaultTemplate = vaultTemplate;
        this.commonName = commonName;
        this.pkiRoleName = pkiRoleName;
        this.port = port;
        this.applicationContext = applicationContext;
        this.ssl = new Ssl();

        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(3);
        threadPoolTaskScheduler.initialize();

        taskScheduler = threadPoolTaskScheduler;


    }

    @Override
    public void customize(TomcatServletWebServerFactory factory) {
        generateSslFromVaultCertificate();

        //        X509Certificate caCert = bundle.getX509IssuerCertificate();
        //        LOGGER.info("CA-SerialNumber: {}", caCert.getSerialNumber());
        // TODO: Put this into Frontend-Config.
        //        try {
        //            KeyStore trustStore = KeyStore.getInstance("pkcs12");
        //            trustStore.load(null, null);
        //            trustStore.setCertificateEntry("ca", caCert);
        //            String trustStorePath = saveKeyStoreToFile("server-trust.pkcs12", trustStore);
        //
        //            ssl.setTrustStore(trustStorePath);
        //            ssl.setTrustStorePassword("123456");
        //            ssl.setTrustStoreType(trustStore.getType());
        //        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException e) {
        //            LOGGER.error("Cannot create Truststore", e);
        //        }

        factory.setSsl(ssl);
        factory.setPort(port);
        taskScheduler.schedule(new RotateSSLKeyTask(), certificateNotAfter);
    }

    @Override
    public void destroy() throws Exception {
        vaultTemplate.opsForPki().revoke(certificateSerial.toString());
    }


    private CertificateBundle issueCertificate() {
        final VaultPkiOperations vaultPkiOperations = vaultTemplate.opsForPki();

        VaultCertificateRequest request = VaultCertificateRequest.builder()
                                                                 .ttl(Duration.ofMinutes(15))
                                                                 .commonName(commonName)
                                                                 .build();
        CertificateBundle certificateBundle;
        try {
            VaultCertificateResponse response = vaultPkiOperations.issueCertificate(pkiRoleName, request); // (3)
            certificateBundle = response.getRequiredData(); // (4)
        } catch (VaultException e) {
            LOGGER.error("VaultException ", e);
            throw e;
        }

        LOGGER.info("Cert-SerialNumber: {}", certificateBundle.getSerialNumber());
        return certificateBundle;
    }

    private void generateSslFromVaultCertificate() {
        CertificateBundle bundle = issueCertificate();
        KeyStore keyStore = bundle.createKeyStore(ROTATING_KEY);
        String keyStorePath = saveKeyStoreToFile(keyStore);

        ssl.setEnabled(true);
        ssl.setClientAuth(Ssl.ClientAuth.NONE);

        ssl.setKeyStore(keyStorePath);
        ssl.setKeyAlias(ROTATING_KEY);
        ssl.setKeyStoreType(keyStore.getType());
        ssl.setKeyPassword("");
        ssl.setKeyStorePassword(KEYSTORE_PASSPHRASE);
        certificateSerial = bundle.getX509Certificate().getSerialNumber();
        certificateNotAfter = bundle.getX509Certificate().getNotAfter();
    }

    private String saveKeyStoreToFile(KeyStore keyStore) {
        File f = new File(KEYSTORE_FILENAME);
        try (FileOutputStream fos = new FileOutputStream(f)) {
            keyStore.store(fos, KEYSTORE_PASSPHRASE.toCharArray());
            LOGGER.debug("KeyStore: {}", f.getPath());
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Error", e);
        }
        return f.getPath();
    }

    private class RotateSSLKeyTask implements Runnable {
        @Override
        public void run() {
            LOGGER.debug("Rekeying Certificate");
            generateSslFromVaultCertificate();
            taskScheduler.schedule(new RotateSSLKeyTask(), certificateNotAfter);
            rotateServerKeyInAllConnectors();
        }

        private void rotateServerKeyInAllConnectors() {
            if (applicationContext instanceof AnnotationConfigServletWebServerApplicationContext) {
                final AnnotationConfigServletWebServerApplicationContext context =
                        (AnnotationConfigServletWebServerApplicationContext) VaultCertificateHandler.this.applicationContext;
                final TomcatWebServer webServer = (TomcatWebServer) context.getWebServer();
                Connector[] connectors = webServer.getTomcat().getService().findConnectors();
                Arrays.stream(connectors)
                      .filter(connector -> connector.getScheme().equals("https"))
                      .forEach(this::rotateConnectorWithRotatingKey);
            }
        }

        private void rotateConnectorWithRotatingKey(Connector connector) {
            SSLHostConfig[] sslHostConfigs = connector.getProtocolHandler().findSslHostConfigs();
            Arrays.stream(sslHostConfigs).filter(isRotatingKey())
                  .forEach(sslHostConfig -> {

                      try {
                          connector.addSslHostConfig(sslHostConfig);
                      } catch (RuntimeException e) {
                          LOGGER.info("Renewing caused exception.", e);
                      }
                  });
        }

        private Predicate<? super SSLHostConfig> isRotatingKey() {
            return sslHostConfig -> sslHostConfig.getCertificates()
                                                 .stream()
                                                 .anyMatch(sslHostConfigCertificate -> sslHostConfigCertificate
                                                         .getCertificateKeyAlias()
                                                         .equals(ROTATING_KEY));
        }
    }
}
