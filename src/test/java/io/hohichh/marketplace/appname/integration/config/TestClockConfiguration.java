package io.hohichh.marketplace.appname.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import java.time.Clock;
import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestClockConfiguration {

    @Bean
    @Primary
    public Clock testClock() {
        return mock(Clock.class);
    }
}