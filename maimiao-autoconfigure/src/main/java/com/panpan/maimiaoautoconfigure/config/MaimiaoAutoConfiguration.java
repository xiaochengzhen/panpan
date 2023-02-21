package com.panpan.maimiaoautoconfigure.config;

import com.panpan.maimiaoautoconfigure.service.ResponseAdvice;
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
    public ResponseAdvice responseAdvice() {
        return new ResponseAdvice();
    }
}
