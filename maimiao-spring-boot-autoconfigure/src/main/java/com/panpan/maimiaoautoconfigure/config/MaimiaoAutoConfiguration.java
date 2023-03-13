package com.panpan.maimiaoautoconfigure.config;

import com.panpan.maimiaoautoconfigure.service.CheckUniqueAdvice;
import com.panpan.maimiaoautoconfigure.service.DefaultValueAdvice;
import com.panpan.maimiaoautoconfigure.service.QuoteAdvice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xiaobo
 * description 配置类
 * date 2023/2/21 14:33
 */
@Configuration
public class MaimiaoAutoConfiguration {

    @Bean
    @ConditionalOnWebApplication
    public QuoteAdvice quoteAdvice() {
        return new QuoteAdvice();
    }

    @Bean
    @ConditionalOnWebApplication
    public CheckUniqueAdvice checkUniqueAdvice() {
        return new CheckUniqueAdvice();
    }

    @Bean
    @ConditionalOnWebApplication
    public DefaultValueAdvice defaultValueAdvice() {
        return new DefaultValueAdvice();
    }
}
