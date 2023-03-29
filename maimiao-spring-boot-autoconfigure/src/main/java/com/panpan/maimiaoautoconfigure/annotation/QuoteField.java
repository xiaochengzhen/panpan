package com.panpan.maimiaoautoconfigure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaobo
 * description 获取引用字段信息字段
 * date 2022/6/23 13:13
 */
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QuoteField {

    String value();//关联属性名称
    String tableName();
    String associatedField();//关联字段在关联表中的字段名称
    String getField();//关联字段表中需要获取的字段名称
    String dataSourceName()default "dataSource";
}
