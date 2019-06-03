### mybatis配置文件之属性设置
这是 MyBatis 中极为重要的调整设置，它们会改变 MyBatis 的运行时行为。 下表描述了设置中各项的意图、默认值等。
```xml
<settings>
         <!--全局地开启或关闭配置文件中的所有映射器已经配置的任何缓存。默认true-->
        <setting name="cacheEnabled" value="true"/>
         <!--延迟加载的全局开关。当开启时，所有关联对象都会延迟加载。 特定关联关系中可通过设置 fetchType 属性来覆盖该项的开关状态。默认false-->
        <setting name="lazyLoadingEnabled" value="true"/>
        <!--当开启时，任何方法的调用都会加载该对象的所有属性。 否则，每个属性会按需加载（参考 lazyLoadTriggerMethods)。默认false（在 3.4.1 及之前的版本默认值为 true）-->
        <setting name="aggressiveLazyLoading" value="true"/>
        <!--是否允许单一语句返回多结果集（需要驱动支持）。默认true-->
        <setting name="multipleResultSetsEnabled" value="true"/>
        <!--使用列标签代替列名。不同的驱动在这方面会有不同的表现，具体可参考相关驱动文档或通过测试这两种不同的模式来观察所用驱动的结果。默认true-->
        <setting name="useColumnLabel" value="true"/>
        <!--允许 JDBC 支持自动生成主键，需要驱动支持。 如果设置为 true 则这个设置强制使用自动生成主键，尽管一些驱动不能支持但仍可正常工作（比如 Derby）。默认false-->
        <setting name="useGeneratedKeys" value="false"/>
        <!--指定 MyBatis 应如何自动映射列到字段或属性。 NONE 表示取消自动映射；PARTIAL 只会自动映射没有定义嵌套结果集映射的结果集。 FULL 会自动映射任意复杂的结果集（无论是否嵌套）。默认PARTIAL-->
        <setting name="autoMappingBehavior" value="PARTIAL"/>
        <!--
        指定发现自动映射目标未知列（或者未知属性类型）的行为。
        NONE: 不做任何反应
        WARNING: 输出提醒日志 ('org.apache.ibatis.session.AutoMappingUnknownColumnBehavior' 的日志等级必须设置为 WARN)
        FAILING: 映射失败 (抛出 SqlSessionException),默认值为NONE
        -->
        <setting name="autoMappingUnknownColumnBehavior" value="WARNING"/>
        <!--配置默认的执行器。SIMPLE 就是普通的执行器；REUSE 执行器会重用预处理语句（prepared statements）； BATCH 执行器将重用语句并执行批量更新。
            默认为SIMPLE
        -->
        <setting name="defaultExecutorType" value="SIMPLE"/>
        <!--设置超时时间，它决定驱动等待数据库响应的秒数。默认	未设置 (null)-->
        <setting name="defaultStatementTimeout" value="25"/>
        <!--为驱动的结果集获取数量（fetchSize）设置一个提示值。此参数只可以在查询设置中被覆盖。默认	未设置 (null)-->
        <setting name="defaultFetchSize" value="100"/>
        <!--允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false。默认为	False-->
        <setting name="safeRowBoundsEnabled" value="false"/>
        <!--允许在嵌套语句中使用分页（ResultHandler）。如果允许使用则设置为 false。默认为 TRUE-->
        <setting name="safeResultHandlerEnabled" value="false"/>
        <!--是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射。默认为	False-->
        <setting name="mapUnderscoreToCamelCase" value="false"/>
        <!--MyBatis 利用本地缓存机制（Local Cache）防止循环引用（circular references）和加速重复嵌套查询。 默认值为 SESSION，这种情况下会缓存一个会话中执行的所有查询。 若设置值为 STATEMENT，本地会话仅用在语句执行上，对相同 SqlSession 的不同调用将不会共享数据。-->
        <setting name="localCacheScope" value="SESSION"/>
        <!--当没有为参数提供特定的 JDBC 类型时，为空值指定 JDBC 类型。 某些驱动需要指定列的 JDBC 类型，多数情况直接用一般类型即可，比如 NULL、VARCHAR 或 OTHER。默认OTHER-->
        <setting name="jdbcTypeForNull" value="OTHER"/>
        <!--指定哪个对象的方法触发一次延迟加载。默认equals,clone,hashCode,toString-->
        <setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString"/>
        <!--指定动态 SQL 生成的默认语言。-->
        <setting name="defaultScriptingLanguage" value="org.apache.ibatis.scripting.xmltags.XMLLanguageDriver"/>
        <!--指定 Enum 使用的默认 TypeHandler 。（新增于 3.4.5）-->
        <setting name="defaultEnumTypeHandler" value="org.apache.ibatis.type.EnumTypeHandler"/>
        <!--指定当结果集中值为 null 的时候是否调用映射对象的 setter（map 对象时为 put）方法，这在依赖于 Map.keySet() 或 null 值初始化的时候比较有用。注意基本类型（int、boolean 等）是不能设置成 null 的。-->
        <setting name="callSettersOnNulls" value="false"/>
        <!--当返回行的所有列都是空时，MyBatis默认返回 null。 当开启这个设置时，MyBatis会返回一个空实例。 请注意，它也适用于嵌套的结果集 （如集合或关联）。（新增于 3.4.2）,默认false-->
        <setting name="returnInstanceForEmptyRow" value="false"/>
        <!--指定 MyBatis 增加到日志名称的前缀。默认为 未设置-->
        <setting name="logPrefix" value="aa"/>
        <!--指定 MyBatis 所用日志的具体实现，未指定时将自动查找。默认为未设置-->
        <setting name="logImpl" value="LOG4J"/>
        <!--指定 Mybatis 创建具有延迟加载能力的对象所用到的代理工具。默认JAVASSIST （MyBatis 3.3 以上）-->
        <setting name="proxyFactory" value="JAVASSIST"/>
        <!--指定 VFS 的实现，自定义 VFS 的实现的类全限定名，以逗号分隔。默认未设置-->
        <setting name="vfsImpl" value=""/>
        <!--允许使用方法签名中的名称作为语句参数名称。 为了使用该特性，你的项目必须采用 Java 8 编译，并且加上 -parameters 选项。（新增于 3.4.1），默认为true-->
        <setting name="useActualParamName" value="true"/>
        <!--指定一个提供 Configuration 实例的类。 这个被返回的 Configuration 实例用来加载被反序列化对象的延迟加载属性值。 这个类必须包含一个签名为static Configuration getConfiguration() 的方法。（新增于 3.2.3），默认	未设置-->
        <setting name="configurationFactory" value=""/>
    </settings>
```