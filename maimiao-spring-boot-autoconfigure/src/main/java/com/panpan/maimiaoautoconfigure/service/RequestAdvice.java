package com.panpan.maimiaoautoconfigure.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.PropertyNamingStrategy;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.panpan.maimiaoautoconfigure.annotation.CheckUnique;
import com.panpan.maimiaoautoconfigure.annotation.CheckUniqueField;
import com.panpan.maimiaoautoconfigure.annotation.CheckUniqueFields;
import com.panpan.maimiaoautoconfigure.config.MaimiaoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdvice;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.List;
import java.util.Map;
/**
 * @author xiaobo
 * @description
 * @date 2022/7/21 16:56
 */
@Slf4j
@RestControllerAdvice
public class RequestAdvice implements RequestBodyAdvice {

    @Autowired(required = false)
    private Map<String, DataSource> dataSourceMap;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.hasMethodAnnotation(CheckUnique.class);
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return inputMessage;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        try{
            if (body != null) {
                checkObj(body);
            }
        }catch (Exception e) {
            if (e instanceof MaimiaoException) {
                throw new RuntimeException(e.getMessage());
            }
            log.error("统一请求失败",e);
        }
        return body;
    }

    @Override
    public Object handleEmptyBody(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return body;
    }

    public void checkObj(Object body) throws IllegalAccessException {
        Integer id = null;
        SerializeConfig config = new SerializeConfig();
        config.propertyNamingStrategy = PropertyNamingStrategy.SnakeCase;
        if (!(body instanceof List)) {
            Map<String, Object> parseMap = JSON.parseObject(JSON.toJSONString(body, config, SerializerFeature.WriteMapNullValue), Map.class);
            Object idObj = parseMap.get("id");
            if (idObj != null){
                id = (Integer)idObj;
            }
            boolean assignableFrom = body.getClass().isAnnotationPresent(CheckUniqueField.class);
            if (assignableFrom) {
                CheckUniqueField annotation = body.getClass().getAnnotation(CheckUniqueField.class);
                String dataSourceName = annotation.dataSourceName();
                String tableName = annotation.tableName();
                String value = annotation.value();
                int deleteValue = annotation.deleteValue();
                String deleteCol = annotation.deleteCol();
                boolean empty = annotation.empty();
                String tips = annotation.tips();
                String[] split = value.split(",");
                outer:
                for (String s : split) {
                    String whereString = " where 1=1 ";
                    String[] split1 = s.split("#");
                    for (String s1 : split1) {
                        boolean b = false;
                        Object colObj = parseMap.get(s1);
                        if (colObj == null) {
                            b = true;
                        } else {
                            if (colObj instanceof String) {
                                if (StringUtils.isBlank((CharSequence) colObj)) {
                                    b = true;
                                }
                            }
                        }
                        if (b){
                            if (!empty) {
                                break outer;
                            }
                            whereString +=  " and " + s1 + " is null ";
                        } else {
                            whereString += " and " + s1 + "='" + colObj+"'";
                        }
                    }
                    String sql = "select id from " + tableName + whereString;
                    if (StringUtils.isNotBlank(deleteCol) && !"null".equals(deleteCol)){
                        sql= sql+ " and " + deleteCol+"="+deleteValue;
                    }
                    sql = sql + " limit 1";
                    Integer idResult = null;
                    if (dataSourceMap != null && !dataSourceName.isEmpty()){
                        try {
                            Connection connection =  dataSourceMap.get(dataSourceName).getConnection();
                            if (connection != null){
                                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                                ResultSet resultSet = preparedStatement.executeQuery();
                                if (resultSet != null && resultSet.next()){
                                    idResult = resultSet.getInt("id");
                                }
                            }
                        } catch (SQLException e) {
                            log.error("统一请求查询数据失败", e);
                            idResult = null;
                        }
                        if (idResult != null && (id == null || !idResult.equals(id))) {
                            throw new MaimiaoException(tips);
                        }
                    }
                }
            } else {
                Field[] declaredFields = body.getClass().getDeclaredFields();
                for (Field declaredField : declaredFields) {
                    declaredField.setAccessible(true);
                    boolean sunAssign = declaredField.isAnnotationPresent(CheckUniqueFields.class);
                    if (sunAssign) {
                        Object o = declaredField.get(body);
                        if (sunAssign) {
                            if (o != null) {
                                checkObj(o);
                            }
                        }
                    }
                }
            }
        }
    }
}
