### 简单好用的数据校验和关联数据获取工具
#### 数据校验
  - 场景：业务判断数据库某一个属性值唯一
  - 使用：只需三步
    * 1、需要在控制层对应方法上面加@CheckUnique注解，表示要用到此功能
      ```JAVA
        @CheckUnique
        @PostMapping("/save")
        public ResultDTO saveTblRole(@RequestBody TblRole tblRole) {}
    * 2、请求类上@CheckUniqueField注解，如果是字段类型是引用类型，且引用类型中也需要用到此功能，在这个引用数据类型上加@CheckUniqueFields注解，如果校验多个字段唯一，可以用#隔开，
         如果需要同时校验多个字段中每个字段都唯一，可以用英文逗号隔开
      ```JAVA
      @CheckUniqueField(value = "role_code", tableName = "tbl_role", dataSourceName = "mpSource", tips = "角色编码已存在")
      public class TblRole {}
    * 3、如果校验不通过，会抛出异常，异常信息可以配置，用户自行处理异常。
  - @CheckUniqueField属性解释\
      value 校验字段\
      dataSourceName 配置的dataSource名称，默认dataSource\
      tableName 对应数据库表名称\
      deleteCol 如果支持逻辑删除，逻辑删除的字段名称\
      deleteValue 逻辑删除，标识不删除的逻辑值\
      empty 如果校验字段是空的情况，是否还需要校验，默认true\
      tips 提示信息
  
#### 关联数据获取
  - 场景：通常情况下，我们关联信息只是存的关联id，但是用户查询接口通常需要看到关联信息不限于id，所以查询接口中需要在业务代码中重新查询关联信息，这样就使代码臃肿，
        怎么不需要在业务代码中写多余的代码呢
  - 使用：只需二步
      * 1、需要在控制层对应方法上面加@Quote，表示要用到此功能
        ```JAVA
        @Quote
        @GetMapping("/get/{id}")
        public ResultDTO getTblRole(@PathVariable Integer id) {
          ResultDTO resultDTO = new ResultDTO("200", "success", tblRoleService.getTblRoleVO(id));
          return resultDTO;
        }
      * 2、响应类对应的字段上加上@QuoteField注解，如果是字段类型是引用类型，且，引用类型中也需要用到此功能，在这个引用数据类型上加@QuoteFields注解
        ```JAVA
        @QuoteField(value = "userId", associatedField = "id", getField = "user_name", tableName = "user", dataSourceName ="mpSource")
        private String userName;
  - @QuoteField属性解释\
      value 关联属性名称\
      dataSourceName 配置的dataSource名称，默认dataSource\
      tableName 对应数据库表名称\
      associatedField 关联字段在关联表中的字段名称\
      getField 关联字段表中需要获取的字段名称
  - v1.0.10开始支持本地缓存，默认关闭，开启需要配置caffeine.cache=true
    
#### 属性设置默认值
  - 场景：我们保存或者修改数据的时候，总会有默认值的设置，如果前端不设置值，后端设置默认值，为了不侵入业务代码，可以通过此功能在属性上面设置完成
  - 使用：只需一步
    * 1、需要在字段上面加@DefaultValueField，表示要给此字段设值默认值
      ```JAVA
      @DefaultValueField("maimiao")
      private String name;
    * 2、如果是字段类型是引用类型，且引用类型中也需要用到此功能，在这个引用数据类型上加@DefaultValueFields注解
      ```JAVA
      @DefaultValueFields
      private TestDefault testDefault;
    
#### 依赖导入
 ``` maven 
 <dependency>
    <groupId>io.github.xiaochengzhen</groupId>
    <artifactId>maimiao-spring-boot-starter</artifactId>
    <version>1.0.10</version>
  </dependency>
