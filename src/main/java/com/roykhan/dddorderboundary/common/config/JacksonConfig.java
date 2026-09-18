package com.roykhan.dddorderboundary.common.config;

import com.roykhan.dddorderboundary.common.response.ApiResponseSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

@Configuration
public class JacksonConfig {

    /**
     * ApiResponse 전용 시리얼라이저를 등록한다.
     *
     * <p>JacksonModule 타입의 빈은 스프링 부트가 자동으로 ObjectMapper에 반영한다.
     */
    @Bean
    SimpleModule apiResponseModule() {
        SimpleModule module = new SimpleModule("ApiResponseModule");
        module.addSerializer(new ApiResponseSerializer());
        return module;
    }
}
