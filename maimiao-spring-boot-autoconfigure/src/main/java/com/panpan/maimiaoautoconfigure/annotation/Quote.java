package com.panpan.maimiaoautoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaobo
 * description 引用标识
 * date 2022/6/23 13:13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Quote {

    String value() default "";
}
