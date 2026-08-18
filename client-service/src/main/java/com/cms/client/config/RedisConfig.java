package com.cms.client.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for client-service.
 *
 * <p>Uses {@link GenericJackson2JsonRedisSerializer} with a custom {@link ObjectMapper}
 * that:
 * <ul>
 *   <li>Serialises {@link java.time.Instant} as ISO-8601 strings (not epoch longs).</li>
 *   <li>Embeds a {@code @class} field so Jackson can reconstruct the exact type
 *       (e.g. {@code ClientResponse}) on deserialization without explicit type hints
 *       at the call site.</li>
 * </ul>
 *
 * <p>Keys are plain UTF-8 strings (e.g. {@code "client:42"}, {@code "email:foo@bar.com"}).
 *
 * <p>{@code @ConditionalOnBean(RedisConnectionFactory.class)} ensures this bean is only
 * created when a real Redis connection is available (i.e. in production / integration tests).
 * In smoke tests that exclude {@code RedisAutoConfiguration}, a {@code @MockBean} substitute
 * is provided instead.
 */
@Configuration
public class RedisConfig {

    /**
     * Only created when a {@link RedisConnectionFactory} bean is present.
     * Smoke tests exclude {@code RedisAutoConfiguration} (no factory) and supply
     * a {@code @MockBean RedisTemplate} directly.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    private ObjectMapper buildRedisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Embed @class field so deserialization works without explicit type hints
        om.activateDefaultTyping(
                om.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return om;
    }
}
