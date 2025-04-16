package br.com.exemplo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.Console;
import java.io.FileInputStream;
import java.security.KeyStore;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java -jar cliente-http.jar <URL> [--cert <CAMINHO_CERTIFICADO>] [--tls <PROTOCOLO_TLS>]");
            return;
        }

        String url = args[0];
        String caminhoCertificado = null;
        String protocoloTLS = null; // Ex: TLSv1.2, TLSv1.3

        // Parse dos parâmetros
        for (int i = 1; i < args.length; i++) {
            if ("--cert".equals(args[i]) && i + 1 < args.length) {
                caminhoCertificado = args[++i];
            } else if ("--tls".equals(args[i]) && i + 1 < args.length) {
                protocoloTLS = args[++i];
            }
        }

        CloseableHttpClient httpClient;

        if (caminhoCertificado != null) {
            // Com certificado
            Console console = System.console();
            if (console == null) {
                System.err.println("Erro: não foi possível acessar o console.");
                return;
            }

            char[] senhaChars = console.readPassword("Digite a senha do certificado: ");
            String senha = new String(senhaChars);

            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(caminhoCertificado), senha.toCharArray());

            SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                    .loadKeyMaterial(keyStore, senha.toCharArray())
                    .build();

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    protocoloTLS != null ? new String[]{ protocoloTLS } : null,
                    null,
                    NoopHostnameVerifier.INSTANCE
            );

            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } else if (protocoloTLS != null) {
            // Sem certificado, mas com protocolo TLS forçado
            SSLContext sslContext = SSLContext.getInstance(protocoloTLS);
            sslContext.init(null, null, null);

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContext,
                    new String[]{ protocoloTLS },
                    null,
                    NoopHostnameVerifier.INSTANCE
            );

            httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
        } else {
            // Sem certificado, sem protocolo especificado
            httpClient = HttpClients.createDefault();
        }

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
