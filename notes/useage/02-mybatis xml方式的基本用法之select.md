mybatis默认遵循"下划线转驼峰"命名方式的，所以在创建实体类时一般都按照这种方式进行创建。

在实体类中不要使用基本类型，使用包装类进行代替。由于java中的基本类型会有默认值，在动态sql的部分，使用!=null进行判断，结果总会为true，因而会导致很多隐藏的问题

mybatis使用java的动态代理可以直接通过接口来调用相应的方法，不需要提供接口的实现类，更不需要在实现类中使用sqlsession以通过命名空间间接调用。

当有多个参数的时候，通过@param注解设置参数的名字省去了手动构造map参数的过程。

当Mapper接口和xml文件关联的时候，命名空间namespace的值就需要配置成接口的全限定名称。

### 使用xml和接口方式
1. 创建表sys_user,sys_role,sys_privilege,sys_user_role,sys_role_privilege并添加数据

2. 创建model，下划线转为驼峰命名法

3. 创建mapper接口

4. 创建mapper xml文件，修改命名空间为对应的mapper接口的全限定名称，mybatis内部就是通过这个值将接口和xml关联起来的。

5. 在mybatis-config.xml中的mappers元素中配置所有的mapper，如果接口和mapper文件在同一个包下，不用手动指定每个mapper，可以直接
```xml
<package name="com.yyb.mapper"/>
```

### 接口方法和xml关联
mybatis通过标签的id属性值和定义的接口方法名将接口方法和xml中定义的SQL语句关联在一起。如果不对应程序启动就会报错。规则如下：

> 当只使用xml而不使用接口的时候，namespace的值可以设置为任意不重复的名称。

> 标签的id属性值在任何时候都不能出现英文句号“.”，并且同意命名空间下不能出现重复的id

> 因为接口方法是可以重载的，而id属性值不能重复，所以同名方法对应着xml中的同一个id的方法。

### xml中标签和属性的作用
```xml
    <select id="selectById" resultMap="User_Map">
        select * from sys_user where id=#{id}
    </select>
```
> \<select\>：映射查询语句使用的标签

> id：命名空间中的唯一标识符，可用来代表这条语句

> resultMap：用于设置返回值的类型和映射关系

> select 标签中的是sql查询语句

> \#{id}：mybatis sql中使用预编译参数的一种方式，id是传入的参数名

### resultMap标签
```xml
    <resultMap id="User_Map" type="SysUser">
        <!--<constructor>-->
            <!--<idArg column="id" javaType="java.lang.Long" jdbcType="BIGINT"/>-->
            <!--<arg column="user_name" javaType="java.lang.String" jdbcType="VARCHAR"/>-->
            <!--<arg column="user_email" javaType="java.lang.String" jdbcType="VARCHAR"/>-->
            <!--<arg column="user_info" javaType="java.lang.String" jdbcType="VARCHAR"/>-->
            <!--<arg column="user_password" javaType="java.lang.String" jdbcType="VARCHAR"/>-->
            <!--<arg column="head_img" javaType="java.lang.Byte" jdbcType="BLOB"/>-->
            <!--<arg column="create_time" javaType="java.lang.Date" jdbcType="TIMESTAMP"/>-->
        <!--</constructor>-->
        <id property="id" column="id"></id>
        <result property="userName" column="user_name"></result>
        <result property="userEmail" column="user_email"></result>
        <result property="userInfo" column="user_info"></result>
        <result property="userPassword" column="user_password"></result>
        <result property="headImg" column="head_img" jdbcType="BLOB"></result>
        <result property="cretateTime" column="create_time" jdbcType="TIMESTAMP"></result>
    </resultMap>
```
resultMap标签用于配置java对象的属性和查询结果列的对应关系，通过resultMap中配置的column和property可以将查询列的值映射到type对象的属性上。

resultMap包含的属性如下：

> id：必填且唯一。供select标签引用

> type：必填，用于配置查询列所映射到的java对象类型

> extends：选填，用于配置当前的resultMap继承自其他的resultMap，属性值为继承resultMap的id。

> autoMapping：选填，ture|false，用于配置是否启用非映射字段的自动映射功能，该配置可以覆盖全局的autoMappingBehavior配置。

resultMap包含的所有标签如下：

> constructor：配置使用构造方法注入结果，包含以下两个子标签：
* idArg：id参数，标记结果作为id，可以帮助提高整体性能。
* arg：注入到构造方法的一个普通结果。
    
> id：一个id结果，标记结果作为id，可以帮组提高整体性能

> result：注入到java对象属性的普通结果

> association：一个复杂的类型关联，许多结果将包成这种类型

> collection：复制类型的结果

> discriminator：根据结果值来决定使用哪种结果映射

> case：基于某些值的结果映射

标签属性之间的关系

> constructor：通过构造方法注入属性的结果值。构造方法中的idArg、arg参数分别对应着resultMap中的id、result标签，它们的含义相同，只是注入方式不同。

> resultMap中的id和result标签包含的属性相同，只是id代表的是主键的字段（可以有多个），它们的属性值通过setter方法注入的。

id和result标签包含的属性

> column：数据库列名，或者列的别名

> property：映射到列结果的属性（model中的属性名）
* 可以映射简单的如“username”
* 也可以映射到一些复杂对象中的属性，例如“address.street.number”

> javaType：一个java类的完全限定名，或一个类型别名（通过typeAlias配置或者默认的类型）。
* 如果映射到一个javabean，mybatis通常可以自动判断属性的类型。
* 如果映射到hashMap，则需要明确地指定javaType属性。

> jdbcType：列对应的数据库类型。jdbc类型仅仅需要对插入、删除操作可能为空的列进行处理，这是jdbc jdbcType的需要，而不是mybatis的需要。

> typeHandler：使用这个属性可以覆盖默认的类型处理器。这个属性值是类的完全限定名或类型别名 

### 接口方法的返回值类型
返回值类型是由xml中的resultType（或resultMap中的type）决定的，不是由接口中写的返回值类型决定的。

### resultType和resultMap区别
resultType：需要在SQL中为所有列名和属性名不一致的列设置别名，使得最终的查询结果的类型和resultType指定对象的属性名保持一致，进而实现自动映射。

resultMap：可以配置property属性（无需考虑大小写一致）和column属性的映射，或者在SQL中设置别名这两种方式实现将查询列映射到对象属性的目的。

### 下划线转驼峰
mybatis提供了一个全局属性mapUnderscoreToCamelCase，通过配置这个属性为true可以自动将以下划线方式命名的列映射到java对象的驼峰式命名属性中。默认为false，可以如下方式配置：
```xml
<setting name="mapUnderscoreToCamelCase" value="true"></setting>
```

### 多表关联
* 情况一：只查单表结果，参考UserMapper的selectRoleByUserId方法
* 情况二：多表结果，但只有少量字段，可以在原model中添加相应的字段，也可以新建类，参考UserMapper的selectRoleExtByUserId方法
* 情况三：多表结果，但有大量字段，情况二方式同样可取，但是麻烦，可以在原model中新增实体类，也可以新建类，参考UserMapper的selectUserAndRoleByUserId方法，当然该方式有缺点:
```
u.user_name as 'sysUser.userName',
u.user_email as 'sysUser.userEmail'
```
使用了sysUser硬编码，如果model中的字段修改，xml也得修改，否则就会出错。









