package com.cms.client.config;

import com.cms.client.mapper.ClientMapper;
import com.cms.client.mapper.ClientMapperImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {
    @Bean
    public ClientMapper clientMapper() {
        return new ClientMapperImpl();
    }
}
