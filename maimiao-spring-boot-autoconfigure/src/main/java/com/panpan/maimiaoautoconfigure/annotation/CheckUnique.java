package com.panpan.maimiaoautoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaobo
 * description 校验唯一性标识注解
 * date  2022/6/23 13:13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckUnique {

    String value() default "";
}
