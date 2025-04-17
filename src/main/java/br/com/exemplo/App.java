package br.com.exemplo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.ssl.*;
import java.io.Console;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.util.Arrays;

public class App {

    public static void main(String[] args) throws Exception {
        // Adiciona o provedor BCJSSE
        Security.insertProviderAt(new BouncyCastleJsseProvider(), 1);

        if (args.length < 1) {
            System.out.println("Uso: java -jar cliente-http.jar <URL> [--cert <CAMINHO_CERTIFICADO>] [--tls <PROTOCOLO_TLS>]");
            return;
        }

        String url = args[0];
        String caminhoCertificado = null;
        String protocoloTLS = "TLSv1.2"; // padrão

        // Lê parâmetros
        for (int i = 1; i < args.length; i++) {
            if ("--cert".equals(args[i]) && i + 1 < args.length) {
                caminhoCertificado = args[++i];
            } else if ("--tls".equals(args[i]) && i + 1 < args.length) {
                protocoloTLS = args[++i];
            }
        }

        CloseableHttpClient httpClient;

        // Cifras exigidas pelo servidor
        String[] cipherSuites = new String[]{
            "TLS_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_RSA_WITH_AES_128_GCM_SHA256"
        };

        // Cria contexto SSL
        SSLContext sslContext = SSLContext.getInstance(protocoloTLS, "BCJSSE");

        if (caminhoCertificado != null) {
            Console console = System.console();
            if (console == null) {
                System.err.println("Erro: não foi possível acessar o console.");
                return;
            }

            char[] senhaChars = console.readPassword("Digite a senha do certificado: ");
            String senha = new String(senhaChars);

            KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            keyStore.load(new FileInputStream(caminhoCertificado), senha.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, senha.toCharArray());

            sslContext.init(kmf.getKeyManagers(), null, null);
        } else {
            sslContext.init(null, null, null);
        }

        // Aplica protocolo e cifras
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
            sslContext,
            new String[]{ protocoloTLS },
            cipherSuites,
            NoopHostnameVerifier.INSTANCE
        );

        httpClient = HttpClients.custom()
                .setSSLSocketFactory(sslsf)
                .build();

        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println("Código de status HTTP: " + statusCode);

            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String resposta = EntityUtils.toString(entity, "UTF-8");
                System.out.println("Corpo da resposta:");
                System.out.println(resposta);
            } else {
                System.out.println("Resposta sem corpo.");
            }
        } finally {
            httpClient.close();
        }
    }
}
