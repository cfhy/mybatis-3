/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLConfigBuilder extends BaseBuilder {

  private boolean parsed;
  private final XPathParser parser;
  private String environment;
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  /**
   * 解析配置文件
   * @param root
   */
  private void parseConfiguration(XNode root) {
    try {
      //issue #117 read properties first
      //获取properties节点下的数据，并保存到Configuration对象的variables字段中
      propertiesElement(root.evalNode("properties"));
      //获取settings节点下的数据，并保持到settings中，以供接下来的程序使用
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      //加载自定义的Vfs实现，并保存到Configuration对象的vfsImpl字段中
      loadCustomVfs(settings);
      //加载自定义的日志实现，并保存到Configuration对象的logImpl字段中
      loadCustomLogImpl(settings);
      //获取typeAliases下的所有数据，并保存到Configuration对象的typeAliasRegistry对象的map中
      typeAliasesElement(root.evalNode("typeAliases"));
      //获取plugins下的所有数据,把拦截器添加到Configuration对象的interceptorChain对象的interceptors list中
      pluginElement(root.evalNode("plugins"));
      //获取objectFactory下的所有数据，并保存到Configuration对象的objectFactory字段
      objectFactoryElement(root.evalNode("objectFactory"));
      //获取objectWrapperFactory节点,并保存到Configuration对象的objectWrapperFactory字段
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      //获取reflectorFactory节点,并保存到Configuration对象的reflectorFactory字段
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      //执行到这里，settings节点下还有很多值未设置，该方法用于设置这些属性的值
      settingsElement(settings);
      // read it after objectFactory and objectWrapperFactory issue #631
      //获取environments节点下的数据
      environmentsElement(root.evalNode("environments"));
      //获取databaseIdProvider节点
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      //获取typeHandlers节点
      typeHandlerElement(root.evalNode("typeHandlers"));
      //获取mappers节点
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  /**
   * 获取settings下的所有子节点，并保持到Properties中，以供后面的程序使用
   * @param context
   * @return
   */
  private Properties settingsAsProperties(XNode context) {
    if (context == null) {
      return new Properties();
    }
    //拿到settings节点下配置的所有属性
    Properties props = context.getChildrenAsProperties();
    // Check that all settings are known to the configuration class
    //检查这些settings在configuration类中是否有对应的set方法，如果没有，说明是乱配置的，直接报错
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  /**
   * 加载自定义的Vfs，并设置到configuration中
   * @param props
   * @throws ClassNotFoundException
   */
  private void loadCustomVfs(Properties props) throws ClassNotFoundException {
    String value = props.getProperty("vfsImpl");
    if (value != null) {
      String[] clazzes = value.split(",");
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  /**
   * 设置自定义的日志实现 ，这里为什么是用resolveClass，而不是classForName，主要是mybatis内定了几种日志实现，
   * 比如别名为SLF4J、COMMONS_LOGGING，对应的实现为Slf4jImpl、JakartaCommonsLoggingImpl，如果配置的别名，
   * 则会根据别名找对应的实现类，如果配置的是自定义实现，则调用Resources.classForName(clazz)获取实现类的Class，并设置到configuration中
   * @param props
   */
  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }

  /**
   * 获取typeAliases下的所有数据，
   * 由于可以配置 package和单个别名，所有有两种解析方式，比如
   *     <typeAliases>
   *         <typeAlias alias="Author" type="yyb.model.Author"/>
   *         <typeAlias alias="Blog" type="yyb.model.Blog"/>
   *         <package name="yyb.model"/>
   *     </typeAliases>
   * @param parent
   */
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          //遍历typeAliases下的所有子节点，如果是package，则获取它的包名，然后根据包名获取该包下的所有类
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else {
          //遍历typeAliases下的所有子节点，如果不是package，则获取它的别名和别名对应的类，然后获取该类的Class
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            Class<?> clazz = Resources.classForName(type);
            if (alias == null) {
              //如果别名为空，则解析类上是否有Alias注解，若有则使用注解的值，否则使用类名作为别名
              typeAliasRegistry.registerAlias(clazz);
            } else {
              //如果别名为空，并且map中不存在，则添加；
              // 如果存在，并且值不一致，也添加，但是map中添加已存在的key会覆盖之前的值，所以最后添加的别名优先级最高
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  /**
   * 解析xml配置中的拦截器
   * <plugin interceptor="yyb.useful.start02.ExamplePlugin">
   *    <property name="someProperty" value="100"/>
   * </plugin>
   * @param parent
   * @throws Exception
   */
  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      //遍历所有的plugin节点，读取interceptor属性
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        //获取plugin下的节点的所有property属性
        Properties properties = child.getChildrenAsProperties();
        //添加interceptor的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该拦截器的实例，
        //从强制转换可以看出，plugin必定要实现Interceptor接口
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        //把读取的属性设置到拦截器实例中
        interceptorInstance.setProperties(properties);
        //把拦截器添加到Configuration对象的interceptorChain对象的interceptors list中
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  /**
   * 解析objectFactory
   * <objectFactory type="yyb.useful.start02.ExampleObjectFactory">
   *     <property name="someProperty" value="100"/>
   * </objectFactory>
   * @param context
   * @throws Exception
   */
  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {
      //获取objectFactory 的type属性
      String type = context.getStringAttribute("type");
      //获取所有的property节点
      Properties properties = context.getChildrenAsProperties();
      //添加objectFactory的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该ObjectFactory的实例
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      //设置属性到factory对象中
      factory.setProperties(properties);
      //设置factory到Configuration对象的objectFactory字段
      configuration.setObjectFactory(factory);
    }
  }

  /**
   * 解析objectWrapperFactory节点
   *  <objectWrapperFactory type=""/>
   * @param context
   * @throws Exception
   */
  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      //获取type属性的值
      String type = context.getStringAttribute("type");
      //添加objectWrapperFactory类型的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该objectWrapperFactory的实例
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      //添加到Configuration对象的objectWrapperFactory字段中
      configuration.setObjectWrapperFactory(factory);
    }
  }

  /**
   * 解析reflectorFactory节点,和objectWrapperFactory一模一样
   * <reflectorFactory type=""/>
   * @param context
   * @throws Exception
   */
  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
      configuration.setReflectorFactory(factory);
    }
  }

  /**
   * 解析properties节点
   * @param context
   * @throws Exception
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      //获取子节点的所有Properties，此处我们的配置为username和password
      Properties defaults = context.getChildrenAsProperties();
      //获取resource属性的值，此处为prop.properties
      String resource = context.getStringAttribute("resource");
      //获取url属性的值
      String url = context.getStringAttribute("url");
      //如果resource和url都有值，则报错
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }
      if (resource != null) {
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) {
        //url用来配置远程地址，比如"https://raw.githubusercontent.com/cfhy/mybatis-3/master/src/test/java/prop.properties"
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      //首先default存放的是properties的子property，然后存的是resource或者url读到的property，然后存的是builder方法传进来的property
      //由于后面的会覆盖掉前面设置的同名的key，所以通过代码传进来的优先级最高，properties下面配置的优先级最低
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      //把最终的properties扔给解析器和配置对象各一份，方便以后使用
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  /**
   * 为settings元素设置值
   * @param props
   */
  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  /**
   * 解析environments节点下的数据
   *  <environments default="development">
   *     <environment id="development">
   *         <transactionManager type="JDBC">
   *             <property name="..." value="..."/>
   *         </transactionManager>
   *         <dataSource type="POOLED">
   *             <property name="driver" value="${driver}"/>
   *             <property name="url" value="${url}"/>
   *             <property name="username" value="${username}"/>
   *             <property name="password" value="${password}"/>
   *         </dataSource>
   *     </environment>
   *     <environment id="production">
   *         <transactionManager type="yyb.useful.start02.ExampleTransactionFactory"></transactionManager>
   *         <dataSource type="UNPOOLED">
   *             <property name="driver" value="${driver}"/>
   *             <property name="url" value="${url}"/>
   *             <property name="username" value="${username}"/>
   *             <property name="password" value="${password}"/>
   *         </dataSource>
   *     </environment>
   * </environments>
   * @param context
   * @throws Exception
   */
  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      if (environment == null) {
        //获取environments的default属性的值
        environment = context.getStringAttribute("default");
      }
      for (XNode child : context.getChildren()) {
        //获取environment的id属性的值
        String id = child.getStringAttribute("id");
        //如果environment和id相等，则为要使用的环境
        if (isSpecifiedEnvironment(id)) {
          //创建TransactionFactory类
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          //创建DataSourceFactory类
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          //获取DataSource对象
          DataSource dataSource = dsFactory.getDataSource();
          //构建Environment对象，并设置到configuration中
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  /**
   * 解析databaseIdProvider节点
   *  <databaseIdProvider type="DB_VENDOR">
   *  	<property name="HSQL Database Engine" value="hsql" />
   *  </databaseIdProvider>
   * @param context
   * @throws Exception
   */
  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      // 获取type属性的值
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
        type = "DB_VENDOR";
      }
      Properties properties = context.getChildrenAsProperties();
      //添加databaseIdProvider类型的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该databaseIdProvider的实例
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      //根据DataSource获取databaseId，并设置到configuration对象中
      configuration.setDatabaseId(databaseId);
    }
  }

  /**
   * 解析transactionManager节点
   *  <transactionManager type="JDBC">
   *      <property name="..." value="..."/>
   *  </transactionManager>
   * @param context
   * @return
   * @throws Exception
   */
  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      //获取type属性
      String type = context.getStringAttribute("type");
      //获取transactionManager节点的所有property节点
      Properties props = context.getChildrenAsProperties();
      //添加transactionManager的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该TransactionFactory的实例
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      //把属性设置到TransactionFactory
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  /**
   *
   * <dataSource type="UNPOOLED">
   *     <property name="driver" value="${driver}"/>
   *     <property name="url" value="${url}"/>
   *     <property name="username" value="${username}"/>
   *     <property name="password" value="${password}"/>
   * </dataSource>
   * @param context
   * @return
   * @throws Exception
   */
  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      //此处的getChildren()方法中完成了${}替换
      Properties props = context.getChildrenAsProperties();
      //添加dataSource的别名到typeAliasRegistry对象的map中，并且根据返回Class创建该DataSourceFactory的实例
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      //为DataSource设置数据库连接属性
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  /**
   * 解析typeHandlers节点
   * <typeHandlers>
   *     <typeHandler handler="yyb.useful.start02.ExampleTypeHandler"/>
   *     <package name="yyb.useful.start02"/>
   * </typeHandlers>
   * @param parent
   */
  private void typeHandlerElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          //遍历typeHandlers下的所有子节点，如果是package，则获取它的包名，然后根据包名获取该包下的所有类
          String typeHandlerPackage = child.getStringAttribute("name");
          //此处和
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          //遍历typeHandlers下的所有子节点，如果不是package，则获取它的别名和别名对应的类，然后获取该类的Class
          //获取javaType，jdbcType，handler属性的值
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          //添加javaType的别名到typeAliasRegistry对象的map中，并且返回Class
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          //添加jdbcType的别名到typeAliasRegistry对象的map中，并且返回Class
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          //添加typeHandler的别名到typeAliasRegistry对象的map中，并且返回Class
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          //最终把typeHandler保存到了TypeHandlerRegistry的typeHandlerMap中
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              //处理javaTypeClass存在，jdbcType不存在的情况
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              //处理javaTypeClass存在，jdbcType也存在的情况
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            //处理javaTypeClass不存在的情况
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  /**
   *  <mappers>
   *      <mapper resource="yyb/useful/start01/BlogMapper.xml"/>
   *  </mappers>
   * @param parent
   * @throws Exception
   */
  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        //遍历mappers下的所有子节点，如果是package，则获取它的包名，然后根据包名获取该包下的所有类
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
          //如果是单个单个的配置mapper
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");
          //如果配置的是resource属性
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            //读取mapper文件
            InputStream inputStream = Resources.getResourceAsStream(resource);
            //创建XMLMapperBuilder对象，和创建XMLMapperBuilder差不多
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            //解析Mapper
            mapperParser.parse();
          } else if (resource == null && url != null && mapperClass == null) {
            //如果配置的是url
            ErrorContext.instance().resource(url);
            //读取url对应的文件
            InputStream inputStream = Resources.getUrlAsStream(url);
            //创建XMLMapperBuilder对象，和创建XMLMapperBuilder差不多
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            //解析Mapper
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            //如果配置的是class属性，则创建class对象
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            //解析mapper接口，并顺带解析映射文件
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
