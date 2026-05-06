package com.opensearch.utility.command.scripts.config;

import com.opensearch.utility.command.scripts.domain.command.StartMigrationCommand;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MigrationWebClientFactory {

    public WebClient createWebClient(StartMigrationCommand.ClusterConfig config) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("migration-pool-" + System.nanoTime())
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .pendingAcquireTimeout(Duration.ofSeconds(45))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getConnectionTimeoutMs())
                .responseTimeout(Duration.ofMillis(config.getSocketTimeoutMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(config.getSocketTimeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(config.getSocketTimeoutMs(), TimeUnit.MILLISECONDS)));

        // Configure SSL if URL is HTTPS
        if (config.getUrl() != null && config.getUrl().toLowerCase().startsWith("https")) {
            httpClient = configureSSL(httpClient, config);
        }

        return WebClient.builder()
                .baseUrl(config.getUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(
                        config.getUsername(),
                        config.getPassword()))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    private HttpClient configureSSL(HttpClient httpClient, StartMigrationCommand.ClusterConfig config) {
        if (!config.isSslVerify()) {
            log.warn("SSL certificate verification is DISABLED for cluster: {}", config.getUrl());
            try {
                SslContext sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
                return httpClient.secure(spec -> spec.sslContext(sslContext));
            } catch (SSLException e) {
                log.error("Failed to configure insecure SSL context: {}", e.getMessage());
                throw new RuntimeException("Failed to configure SSL", e);
            }
        }

        // Default SSL with certificate verification enabled
        return httpClient.secure();
    }
}
