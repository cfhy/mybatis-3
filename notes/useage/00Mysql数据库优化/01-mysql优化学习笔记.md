该文章内容来自慕课网 [mysql数据库优化](https://www.imooc.com/video/3690)

### 数据库优化的目的
> 避免出现页面访问错误

> 由于慢查询造成页面无法加载

> 由于阻塞造成数据无法提交

### 增加数据库的稳定性,很多数据库问题都是由于低效率的查询引起的

### 优化用户体验

> 流畅页面的访问速度

> 良好的网站功能体验

可以从几个方面进行数据库优化
![1.png](1.png)

SQL及索引优化最重要，结构良好的sql，有效的索引，表结构的设计

### SQL及索引优化

####  MySQL慢查日志的开启方式和存储格式

使用MYSQL慢查询日志对有效率问题的sql进行监控,以下为临时设置，如果需要全局设置，需要修改ini配置文件，去掉set global

```sql
show variables like 'slow_query_log'
-- 开启慢查询日志，将 slow_query_log 全局变量设置为“ON”状态 ，临时生效，mysql重启后就会失效
set GLOBAL slow_query_log =on
-- 设置慢查询日志存放的位置
-- set global slow_query_log_file = 'D:\\ProgramFiles\\mysql8\\logs\\query_log_file.log'
set global slow_query_log_file = '/hone/mysql/sql_log/mysql-show.log'
-- 是否要把没有使用索引的查询记录到慢查询日志中
set global log_queries_not_using_indexes=on;
-- 超过1s的慢查询记录到慢查询日志中，通常100ms
set global long_query_time =1
```
#### 慢查询日志的存储格式
```
TCP Port: 3306, Named Pipe: MySQL
Time                 Id Command    Argument
# Time: 2019-06-09T13:28:18.199806Z
执行sql的主机信息
# User@Host: root[root] @ localhost [127.0.0.1]  Id:    10
SQL的执行信息
# Query_time: 0.001008  Lock_time: 0.000136 Rows_sent: 15  Rows_examined: 353
use mybatis-demo;
SQL执行时间
SET timestamp=1560086898;
SQL的内容
SELECT QUERY_ID, SUM(DURATION) AS SUM_DURATION FROM INFORMATION_SCHEMA.PROFILING GROUP BY QUERY_ID;
```
#### 慢查询日志的分析工具
mysqldumpslow 

pt-query-digest

#### 如何通过慢查日志发现有问题的SQL

1. 查询次数多切每次查询占用时间长的SQL

2. IO大的SQL

3. 未命中索引的SQL

找到执行慢的sql后，通过explain查询和分析SQL的执行计划
![2.png](2.png)

table:显示这一行数据是关于哪张表的

type：这是重要的列，显示连接使用了何种类型。从最好到最差的连接类型为const、eq_reg、ref、range、index和All

possible_keys:显示可能应用在这张表中的索引。如果为空，没有可能的索引。

key：实际使用的索引。如果为NULL，则没有使用索引

key_len：使用的索引的长度。在不损失精度性的情况下，长度越短越好

ref：显示索引的那一列被使用了，如果可能的话，是一个常数

rows：MYSQL认为必须检查的用来返回请求数据的行数

extra：using filesort，Using temporary这两种都需要优化，一般出现在group by或者order by中

进度：https://www.imooc.com/video/3711
    