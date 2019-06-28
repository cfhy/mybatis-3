使用 MyBatis 的主要 Java 接口就是 SqlSession。你可以通过这个接口来执行命令，获取映射器和管理事务。我们会概括讨论一下 SqlSession 本身，但是首先我们还是要了解如何获取一个 SqlSession 实例。

SqlSessions 是由 SqlSessionFactory 实例创建的。SqlSessionFactory 对象包含创建 SqlSession 实例的所有方法。而 SqlSessionFactory 本身是由 SqlSessionFactoryBuilder 创建的，它可以从 XML、注解或手动配置 Java 代码来创建 SqlSessionFactory。

注意 当 Mybatis 与一些依赖注入框架（如 Spring 或者 Guice）同时使用时，SqlSessions 将被依赖注入框架所创建，所以你不需要使用 SqlSessionFactoryBuilder 或者 SqlSessionFactory，可以直接看 SqlSession 这一节。请参考 Mybatis-Spring 或者 Mybatis-Guice 手册了解更多信息。

#### SqlSessionFactoryBuilder
 
SqlSessionFactoryBuilder 有五个 build() 方法，每一种都允许你从不同的资源中创建一个 SqlSession 实例。
```java
SqlSessionFactory build(InputStream inputStream)
SqlSessionFactory build(InputStream inputStream, String environment)
SqlSessionFactory build(InputStream inputStream, Properties properties)
SqlSessionFactory build(InputStream inputStream, String env, Properties props)
SqlSessionFactory build(Configuration config)
```
第一种方法是最常用的，它使用了一个参照了 XML 文档或上面讨论过的更特定的 mybatis-config.xml 文件的 Reader 实例。可选的参数是 environment 和 properties。environment 决定加载哪种环境，包括数据源和事务管理器。比如：
```xml
<environments default="development">
   <environment id="development">
     <transactionManager type="JDBC">
         ...
     <dataSource type="POOLED">
         ...
   </environment>
   <environment id="production">
     <transactionManager type="MANAGED">
         ...
     <dataSource type="JNDI">
         ...
   </environment>
 </environments>
```
如果你调用了参数有 environment 的 build 方法，那么 MyBatis 将会使用 configuration 对象来配置这个 environment。当然，如果你指定了一个不合法的 environment，你就会得到错误提示。如果你调用了不带 environment 参数的 build 方法，那么就使用默认的 environment（在上面的示例中指定为 default="development" 的代码）。

如果你调用了参数有 properties 实例的方法，那么 MyBatis 就会加载那些 properties（属性配置文件），并在配置中可用。那些属性可以用${propName} 语法形式多次用在配置文件中。

回想一下，属性可以从 mybatis-config.xml 中被引用，或者直接指定它。因此理解优先级是很重要的。我们在文档前面已经提及它了，但是这里要再次重申：
如果一个属性存在于这些位置，那么 MyBatis 将会按照下面的顺序来加载它们：
 
首先读取在 properties 元素体中指定的属性；
 
其次，读取从 properties 元素的类路径 resource 或 url 指定的属性，且会覆盖已经指定了的重复属性；
 
最后，读取作为方法参数传递的属性，且会覆盖已经从 properties 元素体和 resource 或 url 属性中加载了的重复属性。
 
因此，通过方法参数传递的属性的优先级最高，resource 或 url 指定的属性优先级中等，在 properties 元素体中指定的属性优先级最低。

总结一下，前四个方法很大程度上是相同的，但是由于覆盖机制，便允许你可选地指定 environment 和/或 properties。以下给出一个从 mybatis-config.xml 文件创建 SqlSessionFactory 的示例：
```java
String resource = "org/mybatis/builder/mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
SqlSessionFactory factory = builder.build(inputStream);
```
注意到这里我们使用了 Resources 工具类，这个类在 org.apache.ibatis.io 包中。Resources 类正如其名，会帮助你从类路径下、文件系统或一个 web URL 中加载资源文件。看一下这个类的源代码或者通过你的 IDE 来查看，就会看到一整套相当实用的方法。这里给出一个简表：
```java
URL getResourceURL(String resource)
URL getResourceURL(ClassLoader loader, String resource)
InputStream getResourceAsStream(String resource)
InputStream getResourceAsStream(ClassLoader loader, String resource)
Properties getResourceAsProperties(String resource)
Properties getResourceAsProperties(ClassLoader loader, String resource)
Reader getResourceAsReader(String resource)
Reader getResourceAsReader(ClassLoader loader, String resource)
File getResourceAsFile(String resource)
File getResourceAsFile(ClassLoader loader, String resource)
InputStream getUrlAsStream(String urlString)
Reader getUrlAsReader(String urlString)
Properties getUrlAsProperties(String urlString)
Class classForName(String className)
```
最后一个 build 方法的参数为 Configuration 实例。configuration 类包含你可能需要了解 SqlSessionFactory 实例的所有内容。Configuration 类对于配置的自查很有用，它包含查找和操作 SQL 映射（当应用接收请求时便不推荐使用）。作为一个 Java API 的 configuration 类具有所有配置的开关，这些你已经了解了。这里有一个简单的示例，教你如何手动配置 configuration 实例，然后将它传递给 build() 方法来创建 SqlSessionFactory。
```java
DataSource dataSource = BaseDataTest.createBlogDataSource();
TransactionFactory transactionFactory = new JdbcTransactionFactory();

Environment environment = new Environment("development", transactionFactory, dataSource);

Configuration configuration = new Configuration(environment);
configuration.setLazyLoadingEnabled(true);
configuration.setEnhancementEnabled(true);
configuration.getTypeAliasRegistry().registerAlias(Blog.class);
configuration.getTypeAliasRegistry().registerAlias(Post.class);
configuration.getTypeAliasRegistry().registerAlias(Author.class);
configuration.addMapper(BoundBlogMapper.class);
configuration.addMapper(BoundAuthorMapper.class);

SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
SqlSessionFactory factory = builder.build(configuration);
```
现在你就获得一个可以用来创建 SqlSession 实例的 SqlSessionFactory 了！
#### SqlSessionFactory
SqlSessionFactory 有六个方法创建 SqlSession 实例。通常来说，当你选择这些方法时你需要考虑以下几点：
   
事务处理：我需要在 session 使用事务或者使用自动提交功能（auto-commit）吗？（通常意味着很多数据库和/或 JDBC 驱动没有事务）
   
连接：我需要依赖 MyBatis 获得来自数据源的配置吗？还是使用自己提供的配置？
   
执行语句：我需要 MyBatis 复用预处理语句和/或批量更新语句（包括插入和删除）吗？
 
基于以上需求，有下列已重载的多个 openSession() 方法供使用。
```java
SqlSession openSession()
SqlSession openSession(boolean autoCommit)
SqlSession openSession(Connection connection)
SqlSession openSession(TransactionIsolationLevel level)
SqlSession openSession(ExecutorType execType,TransactionIsolationLevel level)
SqlSession openSession(ExecutorType execType)
SqlSession openSession(ExecutorType execType, boolean autoCommit)
SqlSession openSession(ExecutorType execType, Connection connection)
Configuration getConfiguration();
```