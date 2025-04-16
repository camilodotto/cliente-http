package br.com.exemplo;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
        if (args.length != 2) {
            System.out.println("Uso: java -jar cliente-http.jar <URL> <CAMINHO_CERTIFICADO>");
            return;
        }

        String url = args[0];
        String caminhoCertificado = args[1];

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

        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
        CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();

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
