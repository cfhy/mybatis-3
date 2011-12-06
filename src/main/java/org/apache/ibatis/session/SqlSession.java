package org.apache.ibatis.session;

import org.apache.ibatis.executor.BatchResult;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * The primary Java interface for working with MyBatis. 
 * Through this interface you can execute commands, get mappers and manage transactions.
 *
 */
public interface SqlSession {

  /**
    * Retrieve a single row mapped from the statement key
    * @param statement
    * @return Mapped object
    */
  Object selectOne(String statement);

  /**
    * Retrieve a single row mapped from the statement key and parameter.    
    * @param statement Unique identifier matching the statement to use.
    * @param parameter A parameter object to pass to the statement.
    * @return Mapped object
    */
  Object selectOne(String statement, Object parameter);

  /**
    * Retrieve a list of mapped objects from the statement key and parameter.    
    * @param statement Unique identifier matching the statement to use.
    * @return List of mapped object
    */
  List selectList(String statement);

  /**
    * Retrieve a list of mapped objects from the statement key and parameter.    
    * @param statement Unique identifier matching the statement to use.
    * @param parameter A parameter object to pass to the statement.
    * @return List of mapped object
    */
  List selectList(String statement, Object parameter);

  /**
    * Retrieve a list of mapped objects from the statement key and parameter,
    * within the specified row bounds.
    * @param statement Unique identifier matching the statement to use.
    * @param parameter A parameter object to pass to the statement.
    * @param  rowBounds  Bounds to limit object retrieval
    * @return List of mapped object
    */
  List selectList(String statement, Object parameter, RowBounds rowBounds);

  /**
    * The selectMap is a special case in that it is designed to convert a list 
    * of results into a Map based on one of the properties in the resulting 
    * objects.
    * Eg. Return a of Map[Integer,Author] for selectMap("selectAuthors","id")
    * @param statement Unique identifier matching the statement to use.
    * @param  mapKey The property to use as key for each value in the list.
    * @return Map containing key pair data.
    */
  Map selectMap(String statement, String mapKey);

  /**
    * The selectMap is a special case in that it is designed to convert a list 
    * of results into a Map based on one of the properties in the resulting 
    * objects.
    * @param statement Unique identifier matching the statement to use.
    * @param parameter A parameter object to pass to the statement.
    * @param  mapKey The property to use as key for each value in the list.
    * @return Map containing key pair data.
    */
  Map selectMap(String statement, Object parameter, String mapKey);

  /**
    * The selectMap is a special case in that it is designed to convert a list 
    * of results into a Map based on one of the properties in the resulting 
    * objects.
    * @param statement Unique identifier matching the statement to use.
    * @param parameter A parameter object to pass to the statement.
    * @param  mapKey The property to use as key for each value in the list.
    * @param  rowBounds  Bounds to limit object retrieval
    * @return Map containing key pair data.
    */
  Map selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  /**
   * Retrieve a single row mapped from the statement key and parameter 
   * using a {@code ResultHandler}.
   * @param statement Unique identifier matching the statement to use.
   * @param parameter A parameter object to pass to the statement.
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, Object parameter, ResultHandler handler);

  /**
   * Retrieve a single row mapped from the statement
   * using a {@code ResultHandler}.    
   * @param statement Unique identifier matching the statement to use.
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, ResultHandler handler);

  /**
   * Retrieve a single row mapped from the statement key and parameter    
   * using a {@code ResultHandler} and {@code RowBounds}
   * @param statement Unique identifier matching the statement to use.
   * @param rowBounds RowBound instance to limit the query results
   * @param handler ResultHandler that will handle each retrieved row
   * @return Mapped object
   */
  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  /**
    * Execute an insert statement.
    * @param statement Unique identifier matching the statement to execute.
    * @return int The number of rows affected by the insert.
    */
  int insert(String statement);

  /**
    * Execute an insert statement with the given parameter object. Any generated 
    * autoincrement values or selectKey entries will modify the given parameter
    * object properties. Only the number of rows affected will be returned.
    * @param statement Unique identifier matching the statement to execute.
    * @param parameter A parameter object to pass to the statement.
    * @return int The number of rows affected by the insert.
    */
  int insert(String statement, Object parameter);

  /**
    * Execute an update statement. The number of rows affected will be returned.
    * @param statement Unique identifier matching the statement to execute.
    * @return int The number of rows affected by the update.
    */
  int update(String statement);

  /**
    * Execute an update statement. The number of rows affected will be returned.
    * @param statement Unique identifier matching the statement to execute.
    * @param parameter A parameter object to pass to the statement.
    * @return int The number of rows affected by the update.
    */
  int update(String statement, Object parameter);

  /**
    * Execute a delete statement. The number of rows affected will be returned.
    * @param statement Unique identifier matching the statement to execute.
    * @return int The number of rows affected by the delete.
    */
  int delete(String statement);

  /**
    * Execute a delete statement. The number of rows affected will be returned.
    * @param statement Unique identifier matching the statement to execute.
    * @param parameter A parameter object to pass to the statement.
    * @return int The number of rows affected by the delete.
    */
  int delete(String statement, Object parameter);

  /**
   * Flushes batch statements and commits database connection.
   * Note that database connection will not be committed if no updates/deletes/inserts were called.
   * To force the commit call {@link SqlSession#commit(boolean)}
   */
  void commit();

  /**
   * Flushes batch statements and commits database connection.
   * @param force forces connection commit
   */
  void commit(boolean force);

  /**
   * Discards pending batch statements and rolls database connection back.
   * Note that database connection will not be rolled back if no updates/deletes/inserts were called.
   * To force the rollback call {@link SqlSession#rollback(boolean)}
   */
  void rollback();

  /**
   * Discards pending batch statements and rolls database connection back.
   * Note that database connection will not be rolled back if no updates/deletes/inserts were called.
   * @param force forces connection rollback
   */
  void rollback(boolean force);

  /**
   * Flushes batch statements.
   * @return BatchResult list of updated records
   */
  public List<BatchResult> flushStatements();

  /**
   * Closes the session
   */
  void close();

  /**
   * Clears local session cache
   */
  void clearCache();

  /**
   * Retrieves current configuration
   * @return Configuration
   */
  Configuration getConfiguration();

  /**
   * Retrieves a mapper.
   * @param type Mapper interface class
   * @return a mapper bound to this SqlSession
   */
  <T> T getMapper(Class<T> type);

  /**
   * Restrives inner database connection
   * @return Connection
   */
  Connection getConnection();
}
