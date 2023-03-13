package com.panpan.maimiaoautoconfigure.service;

import com.panpan.maimiaoautoconfigure.annotation.DefaultValueField;
import com.panpan.maimiaoautoconfigure.annotation.DefaultValueFields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;

/**
 * @author xiaobo
 * description 设置默认值
 * date 2022/7/21 16:56
 */
@Slf4j
@RestControllerAdvice
@Order(10)
public class DefaultValueAdvice implements RequestBodyAdvice {

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        //return methodParameter.hasMethodAnnotation(DefaultValue.class);
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        try{
            if (body != null) {
                setDefaultValues(body);
            }
        }catch (Exception e) {
            log.error("设置默认值失败",e);
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    public void setDefaultValues(Object body) {
        if (body != null) {
            if (body instanceof List) {
                List<?> objects = (List<?>) body;
                if (objects != null && !objects.isEmpty()) {
                    for (Object object : objects) {
                        if (object != null){
                            setDefaultValue(object);
                        }
                    }
                }
            } else {
                setDefaultValue(body);
            }
        }
    }

    public void setDefaultValue(Object body){
        if (body != null){
            Class aClass = body.getClass();
            Field[] fields = aClass.getDeclaredFields();
            if (fields != null && fields.length > 0){
                for (Field field : fields) {
                    field.setAccessible(true);
                    try {
                        MergedAnnotations annotations = MergedAnnotations.from(field);
                        MergedAnnotation<?> defaultValueFieldAnnotation = annotations.get(DefaultValueField.class);
                        Object fieldValue = field.get(body);
                        if (defaultValueFieldAnnotation.isPresent() && fieldValue == null) {
                            String value = defaultValueFieldAnnotation.getString("value");
                            Class<?> type = field.getType();
                            switch (type.getSimpleName()) {
                                case "Integer":
                                case "int":
                                    field.set(body, Integer.valueOf(value));
                                    break;
                                case "Byte":
                                case "byte":
                                    field.set(body, Byte.valueOf(value));
                                    break;
                                case "Short":
                                case "short":
                                    field.set(body, Short.valueOf(value));
                                    break;
                                case "Long":
                                case "long":
                                    field.set(body, Long.valueOf(value));
                                    break;
                                case "Boolean":
                                case "boolean":
                                    field.set(body, Boolean.valueOf(value));
                                    break;
                                case "BigDecimal":
                                    field.set(body, new BigDecimal(value));
                                    break;
                                case "String":
                                    field.set(body, value);
                                    break;
                                default:
                                    break;
                            }
                        } else {
                            MergedAnnotation<?> defaultValueFieldsAnnotation = annotations.get(DefaultValueFields.class);
                            if (defaultValueFieldsAnnotation.isPresent()) {
                                if (fieldValue != null){
                                    if (fieldValue instanceof List){
                                        List<?> objects = (List<?>) fieldValue;
                                        if (objects != null && !objects.isEmpty()) {
                                            for (Object object : objects) {
                                                if (object != null){
                                                    setDefaultValue(object);
                                                }
                                            }
                                        }
                                    } else {
                                        setDefaultValue(fieldValue);
                                    }
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
