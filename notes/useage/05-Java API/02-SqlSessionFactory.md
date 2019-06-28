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
默认的 openSession()方法没有参数，它会创建有如下特性的 SqlSession：

- 会开启一个事务（也就是不自动提交）。
- 将从由当前环境配置的 DataSource 实例中获取 Connection 对象。事务隔离级别将会使用驱动或数据源的默认设置。
- 预处理语句不会被复用，也不会批量处理更新。

这些方法大都是可读性强的。向 autoCommit 可选参数传递 true 值即可开启自动提交功能。
若要使用自己的 Connection 实例，传递一个 Connection 实例给 connection 参数即可。注意并未覆写同时设置 Connection 和 autoCommit 两者的方法，因为 MyBatis 会使用正在使用中的、设置了 Connection 的环境。MyBatis 为事务隔离级别调用使用了一个 Java 枚举包装器，称为 TransactionIsolationLevel，若不使用它，将使用 JDBC 所支持五个隔离级（NONE、READ_UNCOMMITTED、READ_COMMITTED、REPEATABLE_READ 和 SERIALIZABLE），并按它们预期的方式来工作。

还有一个可能对你来说是新见到的参数，就是 ExecutorType。这个枚举类型定义了三个值:
- ExecutorType.SIMPLE：这个执行器类型不做特殊的事情。它为每个语句的执行创建一个新的预处理语句。
- ExecutorType.REUSE：这个执行器类型会复用预处理语句。
- ExecutorType.BATCH：这个执行器会批量执行所有更新语句，如果 SELECT 在它们中间执行，必要时请把它们区分开来以保证行为的易读性。
 
注意,在 SqlSessionFactory 中还有一个方法我们没有提及，就是 getConfiguration()。这 个方法会返回一个 Configuration 实例，在运行时你可以使用它来自检 MyBatis 的配置。
 
注意,如果你使用的是 MyBatis 之前的版本，你要重新调用 openSession，因为旧版本的 session、事务和批量操作是分离开来的。如果使用的是新版本，那么就不必这么做了，因为它们现在都包含在 session 的作用域内了。你不必再单独处理事务或批量操作就能得到想要的全部效果。