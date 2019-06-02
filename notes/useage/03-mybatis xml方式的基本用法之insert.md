```java
    int insert(SysUser user);
```

```xml
    <insert id="insert">
        insert into system_user (id,user_name,user_password,user_email,user_info,head_img,create_time)
        value (#{id},#{userName},#{userPasssword},#{userEmail},#{userInfo},#{headInfo},#{headImg,jdbcType="BLOB"},#{createTime,jdbcType="TIMESTAMP"})
    </insert>
```
### 标签解析
insert标签属性：
> id：命名空间中的唯一标识符，可用来代表这条语句。

> parameterType：参数类型，类型的完全限定名或别名。可选，mybatis可以自动推断传入的类型,比如上面的参数类型为SysUser

> flushCache：默认为true，只要语句被调用，就清除一级和二级缓存

> timeout：最大等待数据库返回结果的时间

> statementType：要执行的sql语句的类型：
* PREPARED ，默认值，mybatis使用PreparedStatement对象，PreparedStatement用于执行带参数的预编译SQL语句；
* STATEMENT，mybatis使用Statement对象，Statement用于执行不带参数的SQL语句；
* CALLABLE，mybatis使用CallableStatement对象，CallableStatement用于执行存储过程。
> useGeneratedKeys：默认值为false，如果为true，mybatis会调用JDBC的getGeneratedKeys方法获取数据库生成的主键
> keyProperty:获取到主键后赋值的属性名，如果有多个，可以是用逗号分隔的列表
> databaseId:如果配置了databaseIdProvider,mybatis会加载所有的不带databaseId的或匹配当前databaseId的语句。

为了防止类型错误，对于一些特殊的数据类型，建议指定具体的jdbcType值。
