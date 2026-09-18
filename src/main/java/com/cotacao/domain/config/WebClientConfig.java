package com.cotacao.domain.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient comprasGovWebClient() {
		//Confira o buffer de memória para 16mb (evita erro de BufferOverFlow com JSONs muito grandes)
    	ExchangeStrategies strategies = ExchangeStrategies.builder()
    			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
    			.build();
    	
    	return WebClient.builder().exchangeStrategies(strategies).build();
		
	}
	
}
