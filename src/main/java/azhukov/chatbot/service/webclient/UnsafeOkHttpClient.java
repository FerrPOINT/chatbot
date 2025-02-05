package azhukov.chatbot.service.webclient;

import okhttp3.OkHttpClient;

import javax.net.ssl.*;
import java.net.Proxy;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class UnsafeOkHttpClient {
    public static OkHttpClient getUnsafeOkHttpClient(Proxy proxy) {
        try {
            // Создаем trust manager, который не проверяет сертификаты
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };

            // Инициализируем SSLContext с данным trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            // Собираем OkHttpClient с отключенной проверкой hostname и сертификатов
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    // Всегда возвращаем true – не проверяем hostname
                    return true;
                }
            });

            builder.proxy(proxy);

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
