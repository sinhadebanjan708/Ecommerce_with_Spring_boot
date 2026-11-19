package com.elvero.shop.config;

import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    /** Same wire format Sequelize produced: 2026-11-15T22:33:29.000Z (all stored times are UTC). */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer utcDateTimeFormat() {
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return builder -> builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(format));
    }
}
