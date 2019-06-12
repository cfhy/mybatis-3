### Configuration类
Configuration类代码量很庞大，分析的时候对照mybatis-config.xml就比较清晰了，由于字段中用到了Configuration的内部类，所以先看看该类的实现：
```java
    /**
     * 自定义了一个Map，用于存储String类型的key
     * 该map新增了name和conflictMessageProducer字段，重写了get和put方法
     * @param <V>
     */
    protected static class StrictMap<V> extends HashMap<String, V> {

        private static final long serialVersionUID = -4950446264854982944L;
        private final String name;
        private BiFunction<V, V, String> conflictMessageProducer;

        public StrictMap(String name, int initialCapacity, float loadFactor) {
            super(initialCapacity, loadFactor);
            this.name = name;
        }

        public StrictMap(String name, int initialCapacity) {
            super(initialCapacity);
            this.name = name;
        }

        public StrictMap(String name) {
            super();
            this.name = name;
        }

        public StrictMap(String name, Map<String, ? extends V> m) {
            super(m);
            this.name = name;
        }

        /**
         * Assign a function for producing a conflict error message when contains value with the same key.
         * <p>
         * function arguments are 1st is saved value and 2nd is target value.
         * @param conflictMessageProducer A function for producing a conflict error message
         * @return a conflict error message
         * @since 3.5.0
         */
        public StrictMap<V> conflictMessageProducer(BiFunction<V, V, String> conflictMessageProducer) {
            this.conflictMessageProducer = conflictMessageProducer;
            return this;
        }

        /**
         * 存放的时候先判断，如果key已存在，报错
         * 如果key包含.，说明是类的全路径，则获取类名称作为key，如果类名称不存在，则添加，否则把类名称包装一下再添加
         * @param key
         * @param value
         * @return
         */
        @SuppressWarnings("unchecked")
        public V put(String key, V value) {
            if (containsKey(key)) {
                throw new IllegalArgumentException(name + " already contains value for " + key
                        + (conflictMessageProducer == null ? "" : conflictMessageProducer.apply(super.get(key), value)));
            }
            if (key.contains(".")) {
                final String shortKey = getShortName(key);
                if (super.get(shortKey) == null) {
                    super.put(shortKey, value);
                } else {
                    super.put(shortKey, (V) new Ambiguity(shortKey));
                }
            }
            return super.put(key, value);
        }

        /**
         * 根据key获取值，如果值不存在，则报错，如果值是包装过的key，也报错。
         * @param key
         * @return
         */
        public V get(Object key) {
            V value = super.get(key);
            if (value == null) {
                throw new IllegalArgumentException(name + " does not contain value for " + key);
            }
            if (value instanceof Ambiguity) {
                throw new IllegalArgumentException(((Ambiguity) value).getSubject() + " is ambiguous in " + name
                        + " (try using the full name including the namespace, or rename one of the entries)");
            }
            return value;
        }

        protected static class Ambiguity {
            final private String subject;

            public Ambiguity(String subject) {
                this.subject = subject;
            }

            public String getSubject() {
                return subject;
            }
        }

        private String getShortName(String key) {
            final String[] keyParts = key.split("\\.");
            return keyParts[keyParts.length - 1];
        }
    }
```
可以看到，StrictMap使用String作为key，并在HashMap的基础上增加了name字段和conflictMessageProducer字段，重写了get和put方法，其实也就多了一层校验而已。conflictMessageProducer用于消息提示，有点和js的回调函数类似，外部传个函数进来，在map内部调用。而name暂时看不出有什么用。也许是便于区分吧。

接下来看看Configuration类的字段
```java
 //对应<environment/>节点，Environment内部也就是id,transactionFactory,dataSource字段
    protected Environment environment;

    //以下代码对应<settings>下的子节点
    //允许在嵌套语句中使用分页（RowBounds）。如果允许使用则设置为 false。
    protected boolean safeRowBoundsEnabled;
    //允许在嵌套语句中使用分页（ResultHandler）。如果允许使用则设置为 false。
    protected boolean safeResultHandlerEnabled = true;
    //是否开启自动驼峰命名规则（camel case）映射，即从经典数据库列名 A_COLUMN 到经典 Java 属性名 aColumn 的类似映射。
    protected boolean mapUnderscoreToCamelCase;
    //当开启时，任何方法的调用都会加载该对象的所有属性。 否则，每个属性会按需加载（参考 lazyLoadTriggerMethods)。
    protected boolean aggressiveLazyLoading;
    //是否允许单一语句返回多结果集（需要驱动支持）。
    protected boolean multipleResultSetsEnabled = true;
    //允许 JDBC 支持自动生成主键，需要驱动支持。 如果设置为 true 则这个设置强制使用自动生成主键，尽管一些驱动不能支持但仍可正常工作（比如 Derby）。
    protected boolean useGeneratedKeys;
    //使用列标签代替列名。不同的驱动在这方面会有不同的表现，具体可参考相关驱动文档或通过测试这两种不同的模式来观察所用驱动的结果。
    protected boolean useColumnLabel = true;
    //全局地开启或关闭配置文件中的所有映射器已经配置的任何缓存。
    protected boolean cacheEnabled = true;
    //指定当结果集中值为 null 的时候是否调用映射对象的 setter（map 对象时为 put）方法，这在依赖于 Map.keySet() 或 null 值初始化的时候比较有用。注意基本类型（int、boolean 等）是不能设置成 null 的。
    protected boolean callSettersOnNulls;
    //允许使用方法签名中的名称作为语句参数名称。 为了使用该特性，你的项目必须采用 Java 8 编译，并且加上 -parameters 选项。（新增于 3.4.1）
    protected boolean useActualParamName = true;
    //当返回行的所有列都是空时，MyBatis默认返回 null。 当开启这个设置时，MyBatis会返回一个空实例。 请注意，它也适用于嵌套的结果集 （如集合或关联）。（新增于 3.4.2）
    protected boolean returnInstanceForEmptyRow;
    //指定 MyBatis 增加到日志名称的前缀。
    protected String logPrefix;
    //指定 MyBatis 所用日志的具体实现，未指定时将自动查找。
    protected Class<? extends Log> logImpl;
    //指定 VFS 的实现
    protected Class<? extends VFS> vfsImpl;
    //MyBatis 利用本地缓存机制（Local Cache）防止循环引用（circular references）和加速重复嵌套查询。
    // 默认值为 SESSION，这种情况下会缓存一个会话中执行的所有查询。 若设置值为 STATEMENT，本地会话仅用在语句执行上，对相同 SqlSession 的不同调用将不会共享数据。
    protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
    //当没有为参数提供特定的 JDBC 类型时，为空值指定 JDBC 类型。 某些驱动需要指定列的 JDBC 类型，多数情况直接用一般类型即可，比如 NULL、VARCHAR 或 OTHER。
    protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
    //指定哪个对象的方法触发一次延迟加载。
    protected Set<String> lazyLoadTriggerMethods = new HashSet<>(Arrays.asList("equals", "clone", "hashCode", "toString"));
    //设置超时时间，它决定驱动等待数据库响应的秒数。
    protected Integer defaultStatementTimeout;
    //为驱动的结果集获取数量（fetchSize）设置一个提示值。此参数只可以在查询设置中被覆盖。
    protected Integer defaultFetchSize;
    //	配置默认的执行器。SIMPLE 就是普通的执行器；REUSE 执行器会重用预处理语句（prepared statements）； BATCH 执行器将重用语句并执行批量更新。
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
    //指定 MyBatis 应如何自动映射列到字段或属性。 NONE 表示取消自动映射；PARTIAL 只会自动映射没有定义嵌套结果集映射的结果集。 FULL 会自动映射任意复杂的结果集（无论是否嵌套）。
    protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
    //指定发现自动映射目标未知列（或者未知属性类型）的行为。
    protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;

    //对应<properties/>节点
    protected Properties variables = new Properties();

    protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();

    //对应<objectFactory/>节点
    protected ObjectFactory objectFactory = new DefaultObjectFactory();

    protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();

    //<settings>下的子节点。延迟加载的全局开关。当开启时，所有关联对象都会延迟加载。 特定关联关系中可通过设置 fetchType 属性来覆盖该项的开关状态。
    protected boolean lazyLoadingEnabled = false;
    //<settings>下的子节点。指定 Mybatis 创建具有延迟加载能力的对象所用到的代理工具。
    protected ProxyFactory proxyFactory = new JavassistProxyFactory(); // #224 Using internal Javassist instead of OGNL
    //多数据库时，区分数据库的标识，比如"oracle","mysql"
    protected String databaseId;
    /**
     * Configuration factory class.
     * Used to create Configuration for loading deserialized unread properties.
     *
     * @see <a href='https://code.google.com/p/mybatis/issues/detail?id=300'>Issue 300 (google code)</a>
     */
    protected Class<?> configurationFactory;

    //对应<mappers/>节点，存储所有的mapper，也包含了存取mapper的方法
    protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
    //对应<plugins/>节点，存储所有的plugin，也包含了存取Interceptor的方法
    protected final InterceptorChain interceptorChain = new InterceptorChain();
    //对应<typeHandlers/>节点，存储所有的typeHandler，也包含了存取typeHandler的方法
    protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
    //对应<typeAliases/>节点，存储所有的typeAlias，也包含了存取typeAlias的方法
    protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();

    protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();

    //存储xml映射文件中<select>、<insert>、<delete>、<update>标签的解析出来的内容
    protected final Map<String, MappedStatement> mappedStatements = new StrictMap<MappedStatement>("Mapped Statements collection")
            .conflictMessageProducer((savedValue, targetValue) ->
                    ". please check " + savedValue.getResource() + " and " + targetValue.getResource());
    //存储xml映射文件中<cache>标签的解析出来的内容
    protected final Map<String, Cache> caches = new StrictMap<>("Caches collection");
    //存储xml映射文件中<ResultMap>标签的解析出来的内容
    protected final Map<String, ResultMap> resultMaps = new StrictMap<>("Result Maps collection");
    //存储xml映射文件中<ParameterMap>标签的解析出来的内容
    protected final Map<String, ParameterMap> parameterMaps = new StrictMap<>("Parameter Maps collection");
    //存储xml映射文件中<selectkey>标签的解析出来的内容
    protected final Map<String, KeyGenerator> keyGenerators = new StrictMap<>("Key Generators collection");

    protected final Set<String> loadedResources = new HashSet<>();
    //存储xml映射文件中<sql>标签的解析出来的内容
    protected final Map<String, XNode> sqlFragments = new StrictMap<>("XML fragments parsed from previous mappers");
    //未解析处理的statements
    protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList<>();
    //未解析处理的CacheRefs
    protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList<>();
    //未解析处理的ResultMaps
    protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList<>();
    //未解析处理的Methods
    protected final Collection<MethodResolver> incompleteMethods = new LinkedList<>();

    /*
     * A map holds cache-ref relationship. The key is the namespace that
     * references a cache bound to another namespace and the value is the
     * namespace which the actual cache is bound to.
     */
    protected final Map<String, String> cacheRefMap = new HashMap<>();
```
接下来看看该类的构造方法：
```java
  public Configuration() {
          //使用JDBC的事务管理机制：即利用java.sql.Connection对象完成对事务的提交（commit()）、回滚（rollback()）、关闭（close()）等
          typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
          //使用MANAGED的事务管理机制：这种机制MyBatis自身不会去实现事务管理，而是让程序的容器如（JBOSS，Weblogic）来实现对事务的管理
          typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
  
          //mybatis会从在应用服务器向配置好的JNDI数据源DataSource获取数据库连接。
          typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
          //mybatis会创建一个数据库连接池，连接池的一个连接将会被用作数据库操作。一旦数据库操作完成,mybatis会将此连接返回给连接池。
          typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
          //mybatis会为每一个数据库操作创建一个新的连接，并关闭它。该方式适用于只有小规模数量并发用户的简单应用程序上。
          typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
  
          //以下为缓存策略，在mapper配置文件中使用，eviction是缓存的淘汰算法，可选值有"LRU"、"FIFO"、"SOFT"、"WEAK"，缺省值是LRU
          //PERPETUAL为默认的缓存方式，可以自定义成其他的
          typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
          //FIFO：先进先出，按对象进入缓存的顺序来移除
          typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
          //LRU：最近最少使用，移除最长时间不被使用的对象，默认策略
          typeAliasRegistry.registerAlias("LRU", LruCache.class);
          //SOFT：软引用，移除基于垃圾回收器状态和软引用规则的对象
          typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
          //WEAK：弱引用，更积极地移除基于垃圾收集器状态和弱引用规则的对象
          typeAliasRegistry.registerAlias("WEAK", WeakCache.class);
  
          //MyBatis 可以根据不同的数据库厂商执行不同的语句，这种多厂商的支持是基于映射语句中的 databaseId 属性。
          // MyBatis 会加载不带 databaseId 属性和带有匹配当前数据库 databaseId 属性的所有语句。
          // 如果同时找到带有 databaseId 和不带 databaseId 的相同语句，则后者会被舍弃。
          // 为支持多厂商特性只要在 mybatis-config.xml 文件中加入 databaseIdProvider 即可
          typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);
  
          //XMLLanguageDriver:用于创建动态、静态SqlSource。
          typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
          //RawLanguageDriver:在确保只有静态sql时，可以使用，不得含有任何动态sql的内容
          typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
  
          //日志
          typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
          typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
          typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
          typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
          typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
          typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
          typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
  
          //动态代理的方式
          typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
          typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
  
          ///把XMLLanguageDriver加到languageRegistry对象的map字段里，并设置languageRegistry对象的defaultDriverClass字段为XMLLanguageDriver
          languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
          //把RawLanguageDriver加到languageRegistry对象的map字段里
          languageRegistry.register(RawLanguageDriver.class);
      }
```
接下来，就是一堆get，set方法了，没什么好分析的。
