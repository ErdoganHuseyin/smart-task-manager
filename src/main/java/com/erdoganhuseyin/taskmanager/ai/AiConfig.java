package com.erdoganhuseyin.taskmanager.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /** "What time is it?" as a bean, so a test can replace it with a fixed time. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
