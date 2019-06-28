SqlSession 实例在 MyBatis 中是非常强大的一个类。在这里你会看到所有执行语句、提交或回滚事务和获取映射器实例的方法。

在 SqlSession 类中有超过 20 个方法，所以将它们组合成易于理解的分组。

#### 执行语句方法
 
这些方法被用来执行定义在 SQL 映射的 XML 文件中的 SELECT、INSERT、UPDATE 和 DELETE 语句。它们都会自行解释，每一句都使用语句的 ID 属性和参数对象，参数可以是原生类型（自动装箱或包装类）、JavaBean、POJO 或 Map。
```java
<T> T selectOne(String statement, Object parameter)
<E> List<E> selectList(String statement, Object parameter)
<T> Cursor<T> selectCursor(String statement, Object parameter)
<K,V> Map<K,V> selectMap(String statement, Object parameter, String mapKey)
int insert(String statement, Object parameter)
int update(String statement, Object parameter)
int delete(String statement, Object parameter)
```
selectOne 和 selectList 的不同仅仅是 selectOne 必须返回一个对象或 null 值。如果返回值多于一个，那么就会抛出异常。如果你不知道返回对象的数量，请使用 selectList。如果需要查看返回对象是否存在，可行的方案是返回一个值即可（0 或 1）。selectMap 稍微特殊一点，因为它会将返回的对象的其中一个属性作为 key 值，将对象作为 value 值，从而将多结果集转为 Map 类型值。因为并不是所有语句都需要参数，所以这些方法都重载成不需要参数的形式。  
```java
try (Cursor<MyEntity> entities = session.selectCursor(statement, param)) {
   for (MyEntity entity:entities) {
      // process one entity
   }
}
```
下面的insert, update 和 delete 方法返回受影响的行数
```java
int insert(String statement)
int update(String statement)
int delete(String statement)
```
最后，还有 select 方法的三个高级版本，它们允许你限制返回行数的范围，或者提供自定义结果控制逻辑，这通常在数据集合庞大的情形下使用。
```java
<E> List<E> selectList (String statement, Object parameter, RowBounds rowBounds)
<T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds)
<K,V> Map<K,V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowbounds)
void select (String statement, Object parameter, ResultHandler<T> handler)
void select (String statement, Object parameter, RowBounds rowBounds, ResultHandler<T> handler)
```
RowBounds 参数会告诉 MyBatis 略过指定数量的记录，还有限制返回结果的数量。RowBounds 类有一个构造方法来接收 offset 和 limit，另外，它们是不可二次赋值的。
```java
int offset = 100;
int limit = 25;
RowBounds rowBounds = new RowBounds(offset, limit);
```
所以在这方面，不同的驱动能够取得不同级别的高效率。为了取得最佳的表现，请使用结果集的 SCROLL_SENSITIVE 或 SCROLL_INSENSITIVE 的类型(换句话说：不用 FORWARD_ONLY)。
 
ResultHandler 参数允许你按你喜欢的方式处理每一行。你可以将它添加到 List 中、创建 Map 和 Set，或者丢弃每个返回值都可以，它取代了仅保留执行语句过后的总结果列表的死板结果。你可以使用 ResultHandler 做很多事，并且这是 MyBatis 自身内部会使用的方法，以创建结果集列表。

从3.4.6开始，传递给CALLABLE语句的ResultHandler用于存储过程的每个REFCURSOR输出参数（如果有）。

它的接口很简单。
```java
package org.apache.ibatis.session;
public interface ResultHandler<T> {
  void handleResult(ResultContext<? extends T> context);
}
```
ResultContext 参数允许你访问结果对象本身、被创建的对象数目、以及返回值为 Boolean 的 stop 方法，你可以使用此 stop 方法来停止 MyBatis 加载更多的结果。

使用 ResultHandler 的时候需要注意以下两种限制：

从被 ResultHandler 调用的方法返回的数据不会被缓存。
 
当使用结果映射集（resultMap）时，MyBatis 大多数情况下需要数行结果来构造外键对象。如果你正在使用 ResultHandler，你可以给出外键（association）或者集合（collection）尚未赋值的对象。

#### 批量立即更新方法
有一个方法可以刷新（执行）存储在 JDBC 驱动类中的批量更新语句。当你将 ExecutorType.BATCH 作为 ExecutorType 使用时可以采用此方法。
```java
List<BatchResult> flushStatements()
```
#### 事务控制方法
控制事务作用域有四个方法。当然，如果你已经设置了自动提交或你正在使用外部事务管理器，这就没有任何效果了。然而，如果你正在使用 JDBC 事务管理器，由Connection 实例来控制，那么这四个方法就会派上用场：  
```java
void commit()
void commit(boolean force)
void rollback()
void rollback(boolean force)
```
默认情况下 MyBatis 不会自动提交事务，除非它侦测到有插入、更新或删除操作改变了数据库。如果你已经做出了一些改变而没有使用这些方法，那么你可以传递 true 值到 commit 和 rollback 方法来保证事务被正常处理（注意，在自动提交模式或者使用了外部事务管理器的情况下设置 force 值对 session 无效）。很多时候你不用调用 rollback()，因为 MyBatis 会在你没有调用 commit 时替你完成回滚操作。然而，如果你需要在支持多提交和回滚的 session 中获得更多细粒度控制，你可以使用回滚操作来达到目的。

注意 MyBatis-Spring 和 MyBatis-Guice 提供了声明事务处理，所以如果你在使用 Mybatis 的同时使用了Spring 或者 Guice，那么请参考它们的手册以获取更多的内容。

#### 本地缓存
Mybatis 使用到了两种缓存：本地缓存（local cache）和二级缓存（second level cache）。

每当一个新 session 被创建，MyBatis 就会创建一个与之相关联的本地缓存。任何在 session 执行过的查询语句本身都会被保存在本地缓存中，那么，相同的查询语句和相同的参数所产生的更改就不会二度影响数据库了。本地缓存会被增删改、提交事务、关闭事务以及关闭 session 所清空。

默认情况下，本地缓存数据可在整个 session 的周期内使用，这一缓存需要被用来解决循环引用错误和加快重复嵌套查询的速度，所以它可以不被禁用掉，但是你可以设置 localCacheScope=STATEMENT 表示缓存仅在语句执行时有效。

注意，如果 localCacheScope 被设置为 SESSION，那么 MyBatis 所返回的引用将传递给保存在本地缓存里的相同对象。对返回的对象（例如 list）做出任何更新将会影响本地缓存的内容，进而影响存活在 session 生命周期中的缓存所返回的值。因此，不要对 MyBatis 所返回的对象作出更改，以防后患。

你可以随时调用以下方法来清空本地缓存：void clearCache()

确保 SqlSession 被关闭,void close()

你必须保证的最重要的事情是你要关闭所打开的任何 session。保证做到这点的最佳方式是下面的工作模式:
```java
    SqlSession session = sqlSessionFactory.openSession();
    try {
        // following 3 lines pseudocod for "doing some work"
        session.insert(...);
        session.update(...);
        session.delete(...);
        session.commit();
    } finally {
        session.close();
    }
```
还有，如果你正在使用jdk 1.7以上的版本还有MyBatis 3.2以上的版本，你可以使用try-with-resources语句：
```java
try (SqlSession session = sqlSessionFactory.openSession()) {
    // following 3 lines pseudocode for "doing some work"
    session.insert(...);
    session.update(...);
    session.delete(...);
    session.commit();
}
```
注意,就像 SqlSessionFactory，你可以通过调用当前使用中的 SqlSession 的 getConfiguration 方法来获得 Configuration 实例。
```java
Configuration getConfiguration()
```
#### 使用映射器
```java
<T> T getMapper(Class<T> type)
```
上述的各个 insert、update、delete 和 select 方法都很强大，但也有些繁琐，可能会产生类型安全问题并且对于你的 IDE 和单元测试也没有实质性的帮助。在上面的入门章节中我们已经看到了一个使用映射器的示例。

因此，一个更通用的方式来执行映射语句是使用映射器类。一个映射器类就是一个仅需声明与 SqlSession 方法相匹配的方法的接口类。下面的示例展示了一些方法签名以及它们是如何映射到 SqlSession 上的。

```java
public interface AuthorMapper {
  // (Author) selectOne("selectAuthor",5);
  Author selectAuthor(int id);
  // (List<Author>) selectList(“selectAuthors”)
  List<Author> selectAuthors();
  // (Map<Integer,Author>) selectMap("selectAuthors", "id")
  @MapKey("id")
  Map<Integer, Author> selectAuthors();
  // insert("insertAuthor", author)
  int insertAuthor(Author author);
  // updateAuthor("updateAuthor", author)
  int updateAuthor(Author author);
  // delete("deleteAuthor",5)
  int deleteAuthor(int id);
}
```
总之，每个映射器方法签名应该匹配相关联的 SqlSession 方法，而字符串参数 ID 无需匹配。相反，方法名必须匹配映射语句的 ID。

此外，返回类型必须匹配期望的结果类型，单返回值时为所指定类的值，多返回值时为数组或集合。所有常用的类型都是支持的，包括：原生类 型、Map、POJO 和 JavaBean。

注意，映射器接口不需要去实现任何接口或继承自任何类。只要方法可以被唯一标识对应的映射语句就可以了。
 
注意，映射器接口可以继承自其他接口。当使用 XML 来构建映射器接口时要保证语句被包含在合适的命名空间中。而且，唯一的限制就是你不能在两个继承关系的接口中拥有相同的方法签名（潜在的危险做法不可取）。

你可以传递多个参数给一个映射器方法。如果你这样做了，默认情况下它们将会以 "param" 字符串紧跟着它们在参数列表中的位置来命名，比如：#{param1}、#{param2}等。如果你想改变参数的名称（只在多参数情况下），那么你可以在参数上使用 @Param("paramName") 注解。

你也可以给方法传递一个 RowBounds 实例来限制查询结果。

#### 映射器注解
因为最初设计时，MyBatis 是一个 XML 驱动的框架。配置信息是基于 XML 的，而且映射语句也是定义在 XML 中的。而到了 MyBatis 3，就有新选择了。MyBatis 3 构建在全面且强大的基于 Java 语言的配置 API 之上。这个配置 API 是基于 XML 的 MyBatis 配置的基础，也是新的基于注解配置的基础。注解提供了一种简单的方式来实现简单映射语句，而不会引入大量的开销。
 
注意 不幸的是，Java 注解的的表达力和灵活性十分有限。尽管很多时间都花在调查、设计和试验上，最强大的 MyBatis 映射并不能用注解来构建——并不是在开玩笑，的确是这样。比方说，C#属性就没有这些限制，因此 MyBatis.NET 将会比 XML 有更丰富的选择。也就是说，基于 Java 注解的配置离不开它的特性。

#### 映射申明样例
这个例子展示了如何使用 @SelectKey 注解来在插入前读取数据库序列的值：
```java
@Insert("insert into table3 (id, name) values(#{nameId}, #{name})")
@SelectKey(statement="call next value for TestSequence", keyProperty="nameId", before=true, resultType=int.class)
int insertTable3(Name name);
```
这个例子展示了如何使用 @SelectKey 注解来在插入后读取数据库识别列的值：
```java
@Insert("insert into table2 (name) values(#{name})")
@SelectKey(statement="call identity()", keyProperty="nameId", before=false, resultType=int.class)
int insertTable2(Name name);
```
这个例子展示了如何使用 @Flush 注解去调用 SqlSession#flushStatements()：
```java
@Flush
List<BatchResult> flush();
```
这些例子展示了如何通过指定 @Result 的 id 属性来命名结果集：
```java
@Results(id = "userResult", value = {
  @Result(property = "id", column = "uid", id = true),
  @Result(property = "firstName", column = "first_name"),
  @Result(property = "lastName", column = "last_name")
})
@Select("select * from users where id = #{id}")
User getUserById(Integer id);
```
```java
@Results(id = "companyResults")
@ConstructorArgs({
  @Arg(property = "id", column = "cid", id = true),
  @Arg(property = "name", column = "name")
})
@Select("select * from company where id = #{id}")
Company getCompanyById(Integer id);
```
这个例子展示了单一参数使用 @SqlProvider 注解：
```java
@SelectProvider(type = UserSqlBuilder.class, method = "buildGetUsersByName")
List<User> getUsersByName(String name);

class UserSqlBuilder {
  public static String buildGetUsersByName(final String name) {
    return new SQL(){{
      SELECT("*");
      FROM("users");
      if (name != null) {
        WHERE("name like #{value} || '%'");
      }
      ORDER_BY("id");
    }}.toString();
  }
}
```
这个例子展示了多参数使用 @SqlProvider 注解：
```java
@SelectProvider(type = UserSqlBuilder.class, method = "buildGetUsersByName")
List<User> getUsersByName(
    @Param("name") String name, @Param("orderByColumn") String orderByColumn);

class UserSqlBuilder {

  // If not use @Param, you should be define same arguments with mapper method
  public static String buildGetUsersByName(
      final String name, final String orderByColumn) {
    return new SQL(){{
      SELECT("*");
      FROM("users");
      WHERE("name like #{name} || '%'");
      ORDER_BY(orderByColumn);
    }}.toString();
  }

  // If use @Param, you can define only arguments to be used
  public static String buildGetUsersByName(@Param("orderByColumn") final String orderByColumn) {
    return new SQL(){{
      SELECT("*");
      FROM("users");
      WHERE("name like #{name} || '%'");
      ORDER_BY(orderByColumn);
    }}.toString();
  }
}
```
这个例子展示了使用ProviderMethodResolver的默认实现：
```java
@SelectProvider(type = UserSqlProvider.class)
List<User> getUsersByName(String name);

// Implements the ProviderMethodResolver on your provider class
class UserSqlProvider implements ProviderMethodResolver {
  // In default implementation, it will resolve a method that method name is matched with mapper method
  public static String getUsersByName(final String name) {
    return new SQL(){{
      SELECT("*");
      FROM("users");
      if (name != null) {
        WHERE("name like #{value} || '%'");
      }
      ORDER_BY("id");
    }}.toString();
  }
}
```
