package com.cms.account.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for account-service.
 *
 * <p>Uses Jackson JSON serialization for values so cached {@code AccountResponse}
 * records are human-readable in Redis CLI and cross-process safe.
 * Keys are plain strings (no binary overhead).
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        // Build a base mapper first (no type info), then wrap it in a typing-aware mapper
        ObjectMapper baseMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // NON_CONCRETE_AND_ARRAYS: embed @class type info only for abstract/interface
        // types where the concrete type cannot be inferred at deserialisation time.
        // NON_FINAL would also type-tag Java records (which are final classes), causing
        // deserialisation failures â€” AccountResponse is a record, so NON_FINAL breaks it.
        ObjectMapper typingMapper = baseMapper.copy()
                .activateDefaultTyping(
                        baseMapper.getPolymorphicTypeValidator(),
                        ObjectMapper.DefaultTyping.NON_CONCRETE_AND_ARRAYS);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(typingMapper));
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(typingMapper));
        return template;
    }
}
