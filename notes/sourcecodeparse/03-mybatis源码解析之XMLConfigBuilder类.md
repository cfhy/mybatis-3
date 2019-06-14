上一节分析到了XMLConfigBuilder构造函数，XMLConfigBuilder创建好后，紧接着调用了它的parse()方法
```java
  public Configuration parse() {
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    //XMLConfigBuilder创建的时候parsed设置为false，然后解析之前就设置为已解析，防止重复解析xml配置文件。
    parsed = true;
    //配置文件的根节点为configuration
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }
```
接下来看看parseConfiguration方法是如何解析xml的：
```java
private void parseConfiguration(XNode root) {
    try {
      //下面的代码非常清晰，根据evalNode传递的参数就知道解析的是配置文件的哪个节点了
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

先来看看我们是如何配置的properties：
```
      <properties resource="prop.properties">
          <property name="username" value="dev_user"/>
          <property name="password" value="F2Fa3!33TYyg"/>
      </properties>
      
      prop.properities如下：
      username = root
      password=123456
      driver = com.mysql.cj.jdbc.Driver
      url = jdbc:mysql://localhost:3306/mybatis?useSSL=false&serverTimezone=Hongkong
      
```
解析来看看properties是如何被解析的：
 ```java
  /**
   * 解析properties节点
   * @param context
   * @throws Exception
   */
  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {
      //获取子节点的所有Properties，此处我们的配置为username和password
      Properties defaults = context.getChildrenAsProperties();
      //获取resource属性的值，此处为我们设置的prop.properties
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

```
可以看到，最终读取的Properties存到了configuration对象的variables字段里。解析来分析settings的解析过程：
```java
 /**
   * 获取settings下的所有子节点
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
```
