package com.panpan.maimiaoautoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaobo
 * @description
 * @date 2022/6/23 13:13
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckUniqueField {
    String value();
    String tableName();
    String deleteCol() default "null";//逻辑删除字段
    int deleteValue() default 0;//逻辑删除时，表示不删除的值
    String dataSourceName()default "dataSource";
    boolean empty() default true;//控制是否要考虑字段是空的情况
    String tips() default "信息已存在";
}
