package com.panpan.maimiaoautoconfigure.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.panpan.maimiaoautoconfigure.annotation.QuoteFields;
import com.panpan.maimiaoautoconfigure.annotation.QuoteField;
import com.panpan.maimiaoautoconfigure.annotation.Quote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * @author xiaobo
 * description 获取数据返回信息
 * date 2022/7/21 16:56
 */
@Slf4j
@RestControllerAdvice
public class QuoteAdvice implements ResponseBodyAdvice<Object> {

    @Autowired(required = false)
    private Map<String, DataSource> dataSourceMap;

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        return methodParameter.hasMethodAnnotation(Quote.class);
    }

    @Override
    public Object beforeBodyWrite(Object generalResponse, MethodParameter methodParameter, MediaType mediaType, Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest serverHttpRequest, ServerHttpResponse serverHttpResponse) {
        try {
            if (generalResponse != null) {
                if (generalResponse != null) {
                    if (generalResponse instanceof String){
                        return generalResponse;
                    }
                    assignData(generalResponse);
                }
            }
        } catch (Exception e) {
            log.error("统一赋值error",e);
        }
        return generalResponse;
    }

    public void assignData(Object data) {
        if (data != null) {
            Map<String, Map<String, Object>> useMap = getUseMap(data);
            if (!(data instanceof List)) {
                setValue(data, useMap);
            } else {
                List<?> objects = (List<?>) data;
                if (objects != null && !objects.isEmpty()) {
                    for (Object object : objects) {
                        setValue(object, useMap);
                    }
                }
            }
        }
    }

    public void setValue(Object obj, Map<String, Map<String, Object>> useMap) {
        if (useMap != null && !useMap.isEmpty()) {
            Map<String, Object> parseMap = JSON.parseObject(JSON.toJSONString(obj, SerializerFeature.WriteMapNullValue), Map.class);
            Class<?> clazz = obj.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            if (declaredFields != null && declaredFields.length > 0) {
                for (Field declaredField : declaredFields) {
                    declaredField.setAccessible(true);
                    boolean annotationPresent = declaredField.isAnnotationPresent(QuoteField.class);
                    boolean sunAssign = declaredField.isAnnotationPresent(QuoteFields.class);
                    if (annotationPresent || sunAssign) {
                        if (sunAssign) {
                            try {
                                Object o = declaredField.get(obj);
                                if (o != null) {
                                    if (!(o instanceof List)) {
                                        setValue(o, useMap);
                                    } else {
                                        List<?> objects = (List<?>) o;
                                        if (objects != null && !objects.isEmpty()) {
                                            for (Object object : objects) {
                                                setValue(object, useMap);
                                            }
                                        }
                                    }
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            QuoteField annotation = declaredField.getAnnotation(QuoteField.class);
                            String value = annotation.value();
                            Object valueObj = parseMap.get(value);
                            String valueS = "";
                            if (valueObj instanceof Integer) {
                                valueS = valueObj+"";
                            }
                            if (valueObj instanceof String) {
                                valueS = valueObj.toString();
                            }
                            if (StringUtils.isNotBlank(valueS)) {
                                String tableKey = getTableKey(annotation);
                                if (StringUtils.isNotBlank(tableKey)) {
                                    Map<String, Object> stringStringMap = useMap.get(tableKey);
                                    if (stringStringMap != null && !stringStringMap.isEmpty()) {
                                        try {
                                            Class<?> type = declaredField.getType();
                                            Object fileValue = stringStringMap.get(valueS);
                                            if (fileValue != null){
                                                if (type.getName().equals("java.lang.Byte")) {
                                                    Byte b = Byte.valueOf(stringStringMap.get(valueS).toString());
                                                    declaredField.set(obj, b);
                                                } else {
                                                    declaredField.set(obj, fileValue);
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
                }
            }
        }
    }

    public Map<String, Map<String, Object>> getUseMap(Object data) {
        Map<String, String> tableNameValuesMap = new HashMap<>();//表名， 引用字段values
        if (!(data instanceof List)) {
            setColMap(data, tableNameValuesMap);
        } else {
            List<?> objects = (List<?>) data;
            if (objects != null && !objects.isEmpty()) {
                for (Object object : objects) {
                    setColMap(object, tableNameValuesMap);
                }
            }
        }
        return getUseMap(tableNameValuesMap);
    }

    public void setColMap(Object data, Map<String, String> tableNameValuesMap) {
        Map<String, Object> mapObject = JSON.parseObject(JSON.toJSONString(data, SerializerFeature.WriteMapNullValue), Map.class);
        Field[] declaredFields = data.getClass().getDeclaredFields();
        if (declaredFields != null && declaredFields.length > 0) {
            for (Field declaredField : declaredFields) {
                boolean annotationPresent = declaredField.isAnnotationPresent(QuoteField.class);
                if (annotationPresent) {
                    QuoteField annotation = declaredField.getAnnotation(QuoteField.class);
                    String tableKey = getTableKey(annotation);
                    if (StringUtils.isNotBlank(tableKey)){
                        Object valueObj = mapObject.get(annotation.value());
                        String valueS = "";
                        if (valueObj instanceof Integer) {
                            valueS = valueObj+"";
                        }
                        if (valueObj instanceof String) {
                            valueS = valueObj.toString();
                        }
                        if (StringUtils.isNotBlank(valueS)) {
                            valueS = "'"+valueS+"'";
                            //可以优化的地方，如果是mapValue 不同的情况下，会查询数据库多次，可以tableKey中mapValue 不参与判断存在，但是tableNameValuesMap 的key需要 mapValue
                            String values = tableNameValuesMap.get(tableKey);
                            if (StringUtils.isNotBlank(values)) {
                                tableNameValuesMap.put(tableKey, values+","+ valueS);
                            } else {
                                tableNameValuesMap.put(tableKey, valueS);
                            }
                        }
                    }
                }
                boolean sunAssign = declaredField.isAnnotationPresent(QuoteFields.class);
                if (sunAssign) {
                    declaredField.setAccessible(true);
                    try {
                        Object o = declaredField.get(data);
                        if (o != null){
                            if (!(o instanceof List)) {
                                setColMap(o, tableNameValuesMap);
                            } else {
                                List<?> objects = (List<?>) o;
                                if (objects != null && !objects.isEmpty()) {
                                    for (Object object : objects) {
                                        setColMap(object, tableNameValuesMap);
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

    public String getTableKey(QuoteField annotation) {
        String tableKey = "";
        String dataSourceName = annotation.dataSourceName();
        String tableName = annotation.tableName();
        String mapkey = annotation.mapkey();
        String mapValue = annotation.mapValue();
        if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(mapkey) && StringUtils.isNotBlank(mapValue)) {
            tableKey = dataSourceName+"&"+ tableName + "&" + mapkey + "&" + mapValue;
        }
        return tableKey;
    }

    public Map<String, Map<String, Object>> getUseMap(Map<String, String> tableNameValuesMap) {
        Map<String, Map<String, Object>> resultMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> tableColMap = new HashMap<>();
        if (tableNameValuesMap != null && !tableNameValuesMap.isEmpty()){
            tableNameValuesMap.forEach((k, v)->{
                if (StringUtils.isNotBlank(k) && StringUtils.isNotBlank(v)){
                    String[] split2 = v.split(",");
                    Set<String> set = new HashSet(Arrays.asList(split2));
                    v = StringUtils.join(set, ",");
                    String[] split1 = k.split("&");
                    String dataSourceName = "";
                    String tableName = "";
                    String mapKey = "";
                    String mapValue = "";
                    if (split1.length == 4) {
                        dataSourceName = split1[0];
                        tableName = split1[1];
                        mapKey = split1[2];
                        mapValue = split1[3];
                    }
                    String selectSqlKey = dataSourceName+"&"+tableName+"&"+mapKey+"&"+v;
                    if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(mapKey) && StringUtils.isNotBlank(mapValue)) {
                        List<Map<String, Object>> mapList = tableColMap.get(selectSqlKey);
                        if (CollectionUtils.isEmpty(mapList)) {
                            List<Map<String, Object>> mapListResult = new ArrayList<>();
                            String sql = "select * from " + tableName + " where " + mapKey + " in (" + v + ")";
                            try {
                                if (dataSourceMap != null && !dataSourceName.isEmpty()){
                                    Connection connection =  dataSourceMap.get(dataSourceName).getConnection();
                                    if (connection != null){
                                        PreparedStatement preparedStatement = connection.prepareStatement(sql);
                                        ResultSet resultSet = preparedStatement.executeQuery();
                                        if (resultSet != null){
                                            ResultSetMetaData metaData = resultSet.getMetaData();
                                            if (metaData != null && metaData.getColumnCount() > 0){
                                                while(resultSet.next()){
                                                    int columnCount = metaData.getColumnCount();
                                                    Map<String, Object> map = new HashMap<>();
                                                    for(int i = 1; i<= columnCount; i++) {
                                                        String columnName = metaData.getColumnName(i);
                                                        Object object = resultSet.getObject(columnName);
                                                        map.put(columnName, object);
                                                    }
                                                    mapListResult.add(map);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                            if (!CollectionUtils.isEmpty(mapListResult)) {
                                tableColMap.put(selectSqlKey, mapListResult);
                            }
                        }
                    }
                    Map<String, Object> map = new HashMap<>();
                    if (tableColMap != null && !tableColMap.isEmpty()) {
                        List<Map<String, Object>> maps = tableColMap.get(selectSqlKey);
                        if (!CollectionUtils.isEmpty(maps)) {
                            for (Map<String, Object> mapAll : maps) {
                                Object mapKeyObj = mapAll.get(mapKey);
                                Object mapValueObj = mapAll.get(mapValue);
                                if (mapKeyObj != null && mapValueObj != null) {
                                    map.put(mapKeyObj.toString(), mapValueObj);
                                }
                            }
                        }
                    }
                    resultMap.put(k, map);
                }
            });
        }
        return resultMap;
    }
}
