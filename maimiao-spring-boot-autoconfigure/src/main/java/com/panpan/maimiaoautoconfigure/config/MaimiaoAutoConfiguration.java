package com.panpan.maimiaoautoconfigure.config;

import com.panpan.maimiaoautoconfigure.service.RequestAdvice;
import com.panpan.maimiaoautoconfigure.service.ResponseAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xiaobo
 * @description
 * @date 2023/2/21 14:33
 */
@Configuration
public class MaimiaoAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication
    public ResponseAdvice responseAdvice() {
        return new ResponseAdvice();
    }

    @Bean
    @ConditionalOnWebApplication
    public RequestAdvice requestAdvice() {
        return new RequestAdvice();
    }
}
