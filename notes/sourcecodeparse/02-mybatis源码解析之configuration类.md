### Configuration类
Configuration类代码量很庞大，分析的时候对照mybatis-config.xml就比较清晰了，由于字段中用到了Configuration的内部类，所以先看看该类的实现：
```java
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










首先看看该类的字段：
```java
  
```
接下来，跟踪一下XMLConfigBuilder类的构造函数,看看是如何创建了该类的实例：
```java
    //一上来就初始化了Configuration对象（该对象在XMLConfigBuilder的父类BaseBuilder中）
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }
  //下面是Configuration类的构造函数：
  public Configuration() {
        //transactionManager type="JDBC",使用JDBC的事务管理机制：即利用java.sql.Connection对象完成对事务的提交（commit()）、回滚（rollback()）、关闭（close()）等
        typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
        //transactionManager type="MANAGED",使用MANAGED的事务管理机制：这种机制MyBatis自身不会去实现事务管理，而是让程序的容器如（JBOSS，Weblogic）来实现对事务的管理
        typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
    
        // <dataSource type="JNDI"> mybaties会从在应用服务器向配置好的JNDI数据源DataSource获取数据库连接。在生产环境中优先考虑这种方式。
        typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
        //<dataSource type="POOLED">mybaties会创建一个数据库连接池，连接池的一个连接将会被用作数据库操作。一旦数据库操作完成，
        // mybaties会将此连接返回给连接池。在开发或测试环境中经常用到此方式。
        typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
        //<dataSource type="UNPOOLED">mybaties会为每一个数据库操作创建一个新的连接，并关闭它。该方式适用于只有小规模数量并发用户的简单应用程序上。
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
    
        //<databaseIdProvider type="DB_VENDOR" />
        // MyBatis 可以根据不同的数据库厂商执行不同的语句，这种多厂商的支持是基于映射语句中的 databaseId 属性。 
        // MyBatis 会加载不带 databaseId 属性和带有匹配当前数据库 databaseId 属性的所有语句。 
        // 如果同时找到带有 databaseId 和不带 databaseId 的相同语句，则后者会被舍弃。 
        // 为支持多厂商特性只要在 mybatis-config.xml 文件中加入 databaseIdProvider 即可
        typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);
    
        //XMLLanguageDriver:用于创建动态、静态SqlSource。
        typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
        //RawLanguageDriver:在确保只有静态sql时，可以使用，不得含有任何动态sql的内容
        typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
        
        //配置日志  <setting name="logImpl" value="LOG4J"/>
        typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
        typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
        typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
        typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
        typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
        typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
        typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
        
        //  <setting name="proxyFactory" value="CGLIB"/>  Mybatis延迟加载是通过动态代理完成的
        typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
        typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
        
        ///把XMLLanguageDriver加到languageRegistry对象的map字段里，并设置languageRegistry对象的defaultDriverClass字段为XMLLanguageDriver
        languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
        //把RawLanguageDriver加到languageRegistry对象的map字段里
        languageRegistry.register(RawLanguageDriver.class);
      }
  //可以看到在初始化Configuration对象的时候，注册了很多的别名，这些别名在Configuration配置文件或者mapper中使用，
  // 但是这些别名保存在哪里了呢？通过查看Configuration类，发现有下面这句话，
  protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
  //所以在Configuration构造函数执行之前typeAliasRegistry创建好了，在跟踪 typeAliasRegistry.registerAlias()方法
   public void registerAlias(String alias, Class<?> value) {
     if (alias == null) {
       throw new TypeException("The parameter alias cannot be null");
     }
     // issue #748
     String key = alias.toLowerCase(Locale.ENGLISH);
     if (TYPE_ALIASES.containsKey(key) && TYPE_ALIASES.get(key) != null && !TYPE_ALIASES.get(key).equals(value)) {
       throw new TypeException("The alias '" + alias + "' is already mapped to the value '" + TYPE_ALIASES.get(key).getName() + "'.");
     }
     TYPE_ALIASES.put(key, value);
   }
   //可以看到，别名放到了TYPE_ALIASES变量中，也就是保存在一个HashMap里。
  private final Map<String, Class<?>> TYPE_ALIASES = new HashMap<String, Class<?>>();
  
//接下来看看XMLConfigBuilder的parse方法，看看是如何解析配置文件到Configuration对象的各个字段里的
  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    //该对象创建的时候设置配置文件未解析过，然后解析之后就设置为已解析，防止重复解析。
    parsed = true;
    //配置文件的根节点为configuration
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  private void parseConfiguration(XNode root) {
    try {
      //根据传递的参数就知道解析的就是配置文件的节点了
      propertiesElement(root.evalNode("properties"));
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      typeAliasesElement(root.evalNode("typeAliases"));
      pluginElement(root.evalNode("plugins"));
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      typeHandlerElement(root.evalNode("typeHandlers"));
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
```
接下来逐个分析解析的过程，先来看看propertiesElement:

我们可以在配置文件做如下配置：
```xml
    <properties resource="prop.properties"><!-- 也可以配置url,但url和resource只能存在一个 -->
        <property name="username" value="username"/>
        <property name="password" value="cyy"/>
        <property name="test" value="test"/>
    </properties>
```
 ```java
private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
    //获取properties下的property，也就是username，password，test，保存到defaults中
      Properties defaults = context.getChildrenAsProperties();
      //获取properties的resource属性的值
      String resource = context.getStringAttribute("resource");
      //获取properties的url属性的值
      String url = context.getStringAttribute("url");
      //url和resource只能存在一个,否则会抛异常
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      //把读取到的数据再次放到defaults中，此处如果配置重复了，后放进来的会覆盖之前的
      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      //获取之前的配置信息，如果有的话，先取出来加到defaults中，以防被覆盖掉
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      //把配置信息给XPathParser和Configuration对象各保存一份
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

```

总结：先读取配置文件，然后解析，解析之前先把configuration初始化好，然后把解析的数据设置到该对象对应的各个属性中。