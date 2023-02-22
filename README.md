### 简单好用的数据校验和关联数据获取工具
#### 数据校验
  - 场景：业务判断数据库某一个属性值唯一
  - 使用：只需三步
    * 1、需要在控制层对应方法上面加@CheckUnique注解，表示要用到此功能
    * 2、请求类上@CheckUniqueField注解，如果是字段类型是引用类型，且，引用类型中也需要用到此功能，在这个引用数据类型上加@CheckUniqueFields注解
    * 3、如果校验不通过，会抛出异常，异常信息可以配置，用户自行处理异常。
  - @CheckUniqueField解释
   value 校验字段
    dataSourceName 配置的dataSource名称，默认dataSource
    tableName 对应数据库表名称
    deleteCol 如果支持逻辑删除，逻辑删除的字段名称
    deleteValue 逻辑删除，标识不删除的逻辑值
    empty 如果校验字段是空的情况，是否还需要校验，默认true
    tips 提示信息
  
#### 关联数据获取
  - 场景：通常情况下，我们关联信息只是存的关联id，但是用户查询接口通常需要看到关联信息不限于id，所以查询接口中需要在业务代码中重新查询关联信息，这样就使代码臃肿，
        怎么不需要在业务代码中写多余的代码呢
  - 使用：只需二步
      * 1、需要在控制层对应方法上面加@Quote，表示要用到此功能
      * 2、响应类对应的字段上加上@QuoteField注解，如果是字段类型是引用类型，且，引用类型中也需要用到此功能，在这个引用数据类型上加@QuoteFields注解
  - @QuoteField注解解释
    value 关联字段
    dataSourceName 配置的dataSource名称，默认dataSource
    tableName 对应数据库表名称
    mapkey 关联字段在关联表中的字段名称
    mapValue 关联字段表中需要获取的字段名称
    
