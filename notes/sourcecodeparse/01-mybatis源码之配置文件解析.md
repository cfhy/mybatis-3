## mybatis 源码之配置文件解析

### 准备工作参考【test/yyb/useful/start02】
由于本次只研究mybatis配置文件的加载过程，所以配置了一份最全的配置文件，但无法保证sql语句功能的正常运行。

### 编写测试方法
```java
  @Test
    public void test() throws IOException {
        String resource = "yyb/useful/start02/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        SqlSession session = sqlSessionFactory.openSession();
    }
```
### 读取配置文件
MyBatis 包含一个名叫 Resources 的工具类，它包含一些实用方法，可使从 classpath 或其他位置加载资源文件更加容易。
```java
InputStream inputStream = Resources.getResourceAsStream(resource);
```
层层深入后，最终来到【ClassLoaderWrapper.java】,其最终的实现代码为：
```java
InputStream getResourceAsStream(String resource, ClassLoader[] classLoader) {
    for (ClassLoader cl : classLoader) {
      if (null != cl) {

        // try to find the resource as passed
        InputStream returnValue = cl.getResourceAsStream(resource);

        // now, some class loaders want this leading "/", so we'll add it and try again if we didn't find the resource
        if (null == returnValue) {
          returnValue = cl.getResourceAsStream("/" + resource);
        }

        if (null != returnValue) {
          return returnValue;
        }
      }
    }
    return null;
  }
```
可以看到，最终仍然调用的是classLoader的getResourceAsStream方法。
### 解析配置文件
读取mybatis的配置文件后，会把读取的流交给SqlSessionFactoryBuilder的build方法，SqlSessionFactoryBuilder类的职责就是创建SqlSessionFactory对象，类里面全是build方法的重载，所以创建SqlSessionFactory的方式有很多种，接下来跟踪build方法，发现最终调用的是如下build方法：
```java
    public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
      try {
        XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
        return build(parser.parse());
      } catch (Exception e) {
        throw ExceptionFactory.wrapException("Error building SqlSession.", e);
      } finally {
        ErrorContext.instance().reset();
        try {
          inputStream.close();
        } catch (IOException e) {
          // Intentionally ignore. Prefer previous error.
        }
      }
    }
```
可以发现，该类并没干实事，而是把解析xml的工作转交给了XMLConfigBuilder，下面来看看XMLConfigBuilder的构造方法：
```
  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }
```
可以看到，在创建XMLConfigBuilder对象的时候，又创建了XPathParser对象，而XPathParser对象的创建又需要XMLMapperEntityResolver对象，跟踪一下XPathParser构造方法:
```java
  public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
    commonConstructor(validation, variables, entityResolver);
    this.document = createDocument(new InputSource(inputStream));
  }
  
  private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
      this.validation = validation;
      this.entityResolver = entityResolver;
      this.variables = variables;
      XPathFactory factory = XPathFactory.newInstance();
      this.xpath = factory.newXPath();
    }
    
  private Document createDocument(InputSource inputSource) {
        // important: this must only be called AFTER common constructor
        try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          factory.setValidating(validation);
    
          factory.setNamespaceAware(false);
          factory.setIgnoringComments(true);
          factory.setIgnoringElementContentWhitespace(false);
          factory.setCoalescing(false);
          factory.setExpandEntityReferences(true);
    
          DocumentBuilder builder = factory.newDocumentBuilder();
          builder.setEntityResolver(entityResolver);
          builder.setErrorHandler(new ErrorHandler() {
            @Override
            public void error(SAXParseException exception) throws SAXException {
              throw exception;
            }
    
            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
              throw exception;
            }
    
            @Override
            public void warning(SAXParseException exception) throws SAXException {
            }
          });
          return builder.parse(inputSource);
        } catch (Exception e) {
          throw new BuilderException("Error creating document instance.  Cause: " + e, e);
        }
      }
```
可以看到，构造函数中主要是初始化XpathParser的成员变量，XPathParser这个类为了解析xml封装的工具类，使用了DOM解析和XPath技术，如果想进一步了解，可以看看[这篇文章](https://www.cnblogs.com/eternalisland/p/6287044.html)。XPathParser创建好后，接下来继续看XMLConfigBuilder的构造函数：
```java
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }
```
这里首先执行“super(new Configuration());”，可以看到XMLConfigBuilder继承自BaseBuilder，
![baseBuilder.png](../assert/baseBuilder.png),BaseBuilder有很多的子类，所以可以重点分析下:
```java
public abstract class BaseBuilder {
  //mybatis配置对象，用于保存xml解析后的数据  
  protected final Configuration configuration;
  //类型别名注册对象
  protected final TypeAliasRegistry typeAliasRegistry;
  //TypeHandler注册对象
  protected final TypeHandlerRegistry typeHandlerRegistry;

  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }
  //用于获取配置对象
  public Configuration getConfiguration() {
    return configuration;
  }
  //正则，表达式为空就用默认值  
  protected Pattern parseExpression(String regex, String defaultValue) {
    return Pattern.compile(regex == null ? defaultValue : regex);
  }
  //String类型转换 为布尔类型，为空则为默认值 
  protected Boolean booleanValueOf(String value, Boolean defaultValue) {
    return value == null ? defaultValue : Boolean.valueOf(value);
  }
//String类型转换 为Integer类型，为空则为默认值
  protected Integer integerValueOf(String value, Integer defaultValue) {
    return value == null ? defaultValue : Integer.valueOf(value);
  }
//把逗号分隔的字符串转为HashSet，为空则为默认值
  protected Set<String> stringSetValueOf(String value, String defaultValue) {
    value = value == null ? defaultValue : value;
    return new HashSet<>(Arrays.asList(value.split(",")));
  }
//根据别名获取对应的JdbcType，JdbcType是一个枚举，数据库的字段类型
  protected JdbcType resolveJdbcType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }
//根据别名获取对应的ResultSetType，
//FORWARD_ONLY(ResultSet.TYPE_FORWARD_ONLY),只允许结果集的游标向下移动。
//下面两个都能够实现任意的前后滚动，使用各种移动的ResultSet指针的方法。二者的区别在于前者对于修改不敏感，而后者对于修改敏感。
//SCROLL_INSENSITIVE(ResultSet.TYPE_SCROLL_INSENSITIVE),
//SCROLL_SENSITIVE(ResultSet.TYPE_SCROLL_SENSITIVE);
  protected ResultSetType resolveResultSetType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }
//根据别名获取对应的ParameterMode
//  IN, OUT, INOUT,存储过程的参数类型
  protected ParameterMode resolveParameterMode(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }
//根据别名解析类之后，只是加载了类，并未创建实例，可以调用该方法创建实例
  protected Object createInstance(String alias) {
    Class<?> clazz = resolveClass(alias);
    if (clazz == null) {
      return null;
    }
    try {
      return resolveClass(alias).newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }
  protected <T> Class<? extends T> resolveClass(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }
//根据别名解析TypeHandler
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    if (typeHandlerAlias == null) {
      return null;
    }
    Class<?> type = resolveClass(typeHandlerAlias);
    //判断该TypeHandler是否实现了TypeHandler接口，如果没实现，抛异常
    if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
      throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    @SuppressWarnings("unchecked") // already verified it is a TypeHandler
    Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
    return resolveTypeHandler(javaType, typeHandlerType);
  }
  //获取某个typeHanlder实例，首先会从allTypeHandlersMap中取，如果不存在则创建一个
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    if (typeHandlerType == null) {
      return null;
    }
    // javaType ignored for injected handlers see issue #746 for full detail
    TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
    if (handler == null) {
      // not in registry, create a new one
      handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
    }
    return handler;
  }

  protected <T> Class<? extends T> resolveAlias(String alias) {
    return typeAliasRegistry.resolveAlias(alias);
  }
  //下面的代码时typeAliasRegistry类的resolveAlias()方法
  //逻辑很简单，就是判断typeAliases这个map里是否存在别名，存在直接返回，不存在就加载类并返回
   public <T> Class<T> resolveAlias(String string) {
      try {
        if (string == null) {
          return null;
        }
        // issue #748
        String key = string.toLowerCase(Locale.ENGLISH);
        Class<T> value;
        if (typeAliases.containsKey(key)) {
          value = (Class<T>) typeAliases.get(key);
        } else {
            //下面这句代码层层深入后发现，最终就是这句话： Class<?> c = Class.forName(name, true, cl);
          value = (Class<T>) Resources.classForName(string);
        }
        return value;
      } catch (ClassNotFoundException e) {
        throw new TypeException("Could not resolve type alias '" + string + "'.  Cause: " + e, e);
      }
    }
}
```

总结：本文介绍了如何读取配置文件到内存中。通过源码跟踪走到了XMLConfigBuilder的创建，然而该类的创建又牵扯了许多其他的类，比如XPathParser，该类主要用于解析XML。还有创建了Configuration对象的时候，调研父类(BaseBuilder)的构造，所以我们又分析了该类。该类主要是持有 Configuration，TypeAliasRegistry，TypeHandlerRegistry三大对象，以及一些公用的方法。下一篇分析一下Configuration类。
