package com.matheushenrique.lumi.config;

import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    // se o Atlas não responder em 8 segundos, mostra o erro em vez de travar
    @Bean
    public MongoClientSettingsBuilderCustomizer tempoLimiteConexao() {
        return settings -> settings
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(8, TimeUnit.SECONDS))
                .applyToSocketSettings(socket -> socket.connectTimeout(8, TimeUnit.SECONDS));
    }
}
