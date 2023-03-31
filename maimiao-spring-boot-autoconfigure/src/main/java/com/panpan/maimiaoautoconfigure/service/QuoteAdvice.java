package com.panpan.maimiaoautoconfigure.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.panpan.maimiaoautoconfigure.annotation.QuoteFields;
import com.panpan.maimiaoautoconfigure.annotation.QuoteField;
import com.panpan.maimiaoautoconfigure.annotation.Quote;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaobo
 * description 获取数据返回信息
 * date 2022/7/21 16:56
 */
@Slf4j
@RestControllerAdvice
public class QuoteAdvice implements ResponseBodyAdvice<Object> {

    @Autowired
    private Environment env;

    private Cache<String, List<Map<String, Object>>> caffeine;

    private boolean cache = false;

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
            List<Field> fieldList = new ArrayList<>();
            Class<?> clazz = obj.getClass();
            Field[] declaredFields = clazz.getDeclaredFields();
            if (declaredFields != null && declaredFields.length > 0) {
                fieldList.addAll(Arrays.asList(declaredFields));
            }
            Class<?> superclass = clazz.getSuperclass();
            while (superclass != null) {
                Field[] declaredFieldsSuper = superclass.getDeclaredFields();
                if (declaredFieldsSuper != null && declaredFieldsSuper.length > 0) {
                    fieldList.addAll(Arrays.asList(declaredFieldsSuper));
                }
                superclass = superclass.getSuperclass();
            }
            if (!CollectionUtils.isEmpty(fieldList)) {
                for (Field declaredField : fieldList) {
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
        List<Field> fieldList = new ArrayList<>();
        Field[] declaredFields = data.getClass().getDeclaredFields();
        if (declaredFields != null && declaredFields.length > 0) {
            fieldList.addAll(Arrays.asList(declaredFields));
        }
        Class<?> superclass = data.getClass().getSuperclass();
        while (superclass != null) {
            Field[] declaredFieldsSuper = superclass.getDeclaredFields();
            if (declaredFieldsSuper != null && declaredFieldsSuper.length > 0) {
                fieldList.addAll(Arrays.asList(declaredFieldsSuper));
            }
            superclass = superclass.getSuperclass();
        }
        if (!CollectionUtils.isEmpty(fieldList)) {
            for (Field declaredField : fieldList) {
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
                            //可以优化的地方，如果是getField 不同的情况下，会查询数据库多次，可以tableKey中getField 不参与判断存在，但是tableNameValuesMap 的key需要 getField
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
        String associatedField = annotation.associatedField();
        String getField = annotation.getField();
        if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(associatedField) && StringUtils.isNotBlank(getField)) {
            tableKey = dataSourceName + "&" + tableName + "&" + associatedField + "&" + getField;
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
                    String associatedField = "";
                    String getField = "";
                    if (split1.length == 4) {
                        dataSourceName = split1[0];
                        tableName = split1[1];
                        associatedField = split1[2];
                        getField = split1[3];
                    }
                    String selectSqlKey = dataSourceName+"&"+tableName+"&"+associatedField+"&"+v;
                    if (StringUtils.isNotBlank(tableName) && StringUtils.isNotBlank(associatedField) && StringUtils.isNotBlank(getField)) {
                        List<Map<String, Object>> mapColumn = tableColMap.get(selectSqlKey);
                        if (CollectionUtils.isEmpty(mapColumn)) {
                            if (cache) {
                                List<Map<String, Object>> maps = caffeine.get(selectSqlKey, key -> handleDb(selectSqlKey));
                                tableColMap.put(selectSqlKey, maps);
                            } else {
                                tableColMap.put(selectSqlKey, handleDb(selectSqlKey));
                            }
                        }
                    }
                    Map<String, Object> map = new HashMap<>();
                    if (!CollectionUtils.isEmpty(tableColMap)) {
                        List<Map<String, Object>> maps = tableColMap.get(selectSqlKey);
                        if (!CollectionUtils.isEmpty(maps)) {
                            for (Map<String, Object> mapAll : maps) {
                                Object associatedFieldObj = mapAll.get(associatedField);
                                Object getFieldObj = mapAll.get(getField);
                                if (associatedFieldObj != null && getFieldObj != null) {
                                    map.put(associatedFieldObj.toString(), getFieldObj);
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

    private List<Map<String, Object>> handleDb(String selectSqlKey) {
        String[] split = selectSqlKey.split("&");
        String dataSourceName = split[0];
        String tableName = split[1];
        String associatedField = split[2];
        String v = split[3];
        List<Map<String, Object>> mapListResult = new ArrayList<>();
        String sql = "select * from " + tableName + " where " + associatedField + " in (" + v + ")";
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
        return mapListResult;
    }

    @PostConstruct
    public void buildCaffeine() {
        int initialCapacity = 100;
        int maximumSize = 500;
        int expireAfterWrite = 10;
        if (StringUtils.isNotBlank(env.getProperty("caffeine.initialCapacity"))) {
            initialCapacity = Integer.valueOf(env.getProperty("caffeine.initialCapacity"));
        }
        if (StringUtils.isNotBlank(env.getProperty("caffeine.maximumSize"))) {
            maximumSize = Integer.valueOf(env.getProperty("caffeine.maximumSize"));
        }
        if (StringUtils.isNotBlank(env.getProperty("caffeine.expireAfterWrite"))) {
            expireAfterWrite = Integer.valueOf(env.getProperty("caffeine.expireAfterWrite"));
        }
        if (StringUtils.isNotBlank(env.getProperty("caffeine.cache"))) {
            cache = Boolean.valueOf(env.getProperty("caffeine.cache"));
        }
        caffeine = Caffeine.newBuilder()
                .initialCapacity(initialCapacity)//初始大小
                .maximumSize(maximumSize)//最大数量
                .expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES)//过期时间
                .build();
    }
}
