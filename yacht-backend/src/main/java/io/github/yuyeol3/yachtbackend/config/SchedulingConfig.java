package io.github.yuyeol3.yachtbackend.config;

import io.github.yuyeol3.yachtbackend.config.role.ApiServerOnly;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@ApiServerOnly
@EnableScheduling
public class SchedulingConfig {
}
