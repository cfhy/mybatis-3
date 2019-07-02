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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.IncompleteElementException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Discriminator;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.ResultFlag;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 */
public class XMLMapperBuilder extends BaseBuilder {

  private final XPathParser parser;
  private final MapperBuilderAssistant builderAssistant;
  private final Map<String, XNode> sqlFragments;
  private final String resource;

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(reader, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  @Deprecated
  public XMLMapperBuilder(Reader reader, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(reader, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments, String namespace) {
    this(inputStream, configuration, resource, sqlFragments);
    this.builderAssistant.setCurrentNamespace(namespace);
  }

  public XMLMapperBuilder(InputStream inputStream, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    this(new XPathParser(inputStream, true, configuration.getVariables(), new XMLMapperEntityResolver()),
        configuration, resource, sqlFragments);
  }

  private XMLMapperBuilder(XPathParser parser, Configuration configuration, String resource, Map<String, XNode> sqlFragments) {
    super(configuration);
    this.builderAssistant = new MapperBuilderAssistant(configuration, resource);
    this.parser = parser;
    this.sqlFragments = sqlFragments;
    this.resource = resource;
  }

  public void parse() {
    //判断是否已加载过
    if (!configuration.isResourceLoaded(resource)) {
      //解析映射文件
      configurationElement(parser.evalNode("/mapper"));
      configuration.addLoadedResource(resource);
      bindMapperForNamespace();
    }

    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

  public XNode getSqlFragment(String refid) {
    return sqlFragments.get(refid);
  }

  private void configurationElement(XNode context) {
    try {
      //获取命名空间
      String namespace = context.getStringAttribute("namespace");
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      //把命名空间设置到builderAssistant的currentNamespace属性中
      builderAssistant.setCurrentNamespace(namespace);
      //解析cache-ref节点
      cacheRefElement(context.evalNode("cache-ref"));
      //解析Cache节点
      cacheElement(context.evalNode("cache"));
      //解析parameterMap节点
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      //解析所有的resultMap节点
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper/sql"));
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }

  private void buildStatementFromContext(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      buildStatementFromContext(list, configuration.getDatabaseId());
    }
    buildStatementFromContext(list, null);
  }

  private void buildStatementFromContext(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      final XMLStatementBuilder statementParser = new XMLStatementBuilder(configuration, builderAssistant, context, requiredDatabaseId);
      try {
        statementParser.parseStatementNode();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteStatement(statementParser);
      }
    }
  }

  private void parsePendingResultMaps() {
    Collection<ResultMapResolver> incompleteResultMaps = configuration.getIncompleteResultMaps();
    synchronized (incompleteResultMaps) {
      Iterator<ResultMapResolver> iter = incompleteResultMaps.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolve();
          iter.remove();
        } catch (IncompleteElementException e) {
          // ResultMap is still missing a resource...
        }
      }
    }
  }

  private void parsePendingCacheRefs() {
    Collection<CacheRefResolver> incompleteCacheRefs = configuration.getIncompleteCacheRefs();
    synchronized (incompleteCacheRefs) {
      Iterator<CacheRefResolver> iter = incompleteCacheRefs.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().resolveCacheRef();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Cache ref is still missing a resource...
        }
      }
    }
  }

  private void parsePendingStatements() {
    Collection<XMLStatementBuilder> incompleteStatements = configuration.getIncompleteStatements();
    synchronized (incompleteStatements) {
      Iterator<XMLStatementBuilder> iter = incompleteStatements.iterator();
      while (iter.hasNext()) {
        try {
          iter.next().parseStatementNode();
          iter.remove();
        } catch (IncompleteElementException e) {
          // Statement is still missing a resource...
        }
      }
    }
  }

  /**
   * <cache-ref namespace="com.someone.application.data.SomeMapper"/>
   * @param context
   */
  private void cacheRefElement(XNode context) {
    if (context != null) {
      configuration.addCacheRef(builderAssistant.getCurrentNamespace(), context.getStringAttribute("namespace"));
      CacheRefResolver cacheRefResolver = new CacheRefResolver(builderAssistant, context.getStringAttribute("namespace"));
      try {
        cacheRefResolver.resolveCacheRef();
      } catch (IncompleteElementException e) {
        configuration.addIncompleteCacheRef(cacheRefResolver);
      }
    }
  }

  /**
   * <cache eviction="FIFO" flushInterval="60000" size="512" readOnly="true">
   *    <property name="cacheFile" value="/tmp/my-custom-cache.tmp"/>
   * </cache>
   * @param context
   */
  private void cacheElement(XNode context) {
    if (context != null) {
      //读取type属性，未设置则默认为PERPETUAL
      String type = context.getStringAttribute("type", "PERPETUAL");
      //解析别名并创建Class对象
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      //读取eviction属性，未设置则默认为LRU
      String eviction = context.getStringAttribute("eviction", "LRU");
      //解析别名并创建Class对象
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      //读取flushInterval属性
      Long flushInterval = context.getLongAttribute("flushInterval");
      //读取size属性
      Integer size = context.getIntAttribute("size");
      //读取readOnly属性，未设置则默认为false
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      //读取blocking属性，未设置则默认为false
      boolean blocking = context.getBooleanAttribute("blocking", false);
      //读取所有的property
      Properties props = context.getChildrenAsProperties();
      //创建缓存对象，并保持到Configuration对象中
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }

  private void parameterMapElement(List<XNode> list) {
    for (XNode parameterMapNode : list) {
      String id = parameterMapNode.getStringAttribute("id");
      String type = parameterMapNode.getStringAttribute("type");
      Class<?> parameterClass = resolveClass(type);
      List<XNode> parameterNodes = parameterMapNode.evalNodes("parameter");
      List<ParameterMapping> parameterMappings = new ArrayList<>();
      for (XNode parameterNode : parameterNodes) {
        String property = parameterNode.getStringAttribute("property");
        String javaType = parameterNode.getStringAttribute("javaType");
        String jdbcType = parameterNode.getStringAttribute("jdbcType");
        String resultMap = parameterNode.getStringAttribute("resultMap");
        String mode = parameterNode.getStringAttribute("mode");
        String typeHandler = parameterNode.getStringAttribute("typeHandler");
        Integer numericScale = parameterNode.getIntAttribute("numericScale");
        ParameterMode modeEnum = resolveParameterMode(mode);
        Class<?> javaTypeClass = resolveClass(javaType);
        JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
        Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
        ParameterMapping parameterMapping = builderAssistant.buildParameterMapping(parameterClass, property, javaTypeClass, jdbcTypeEnum, resultMap, modeEnum, typeHandlerClass, numericScale);
        parameterMappings.add(parameterMapping);
      }
      builderAssistant.addParameterMap(id, parameterClass, parameterMappings);
    }
  }

  private void resultMapElements(List<XNode> list) throws Exception {
    for (XNode resultMapNode : list) {
      try {
        resultMapElement(resultMapNode);
      } catch (IncompleteElementException e) {
        // ignore, it will be retried
      }
    }
  }

  private ResultMap resultMapElement(XNode resultMapNode) throws Exception {
    return resultMapElement(resultMapNode, Collections.emptyList(), null);
  }

  /**
   *
   *     <resultMap id="detailedBlogResultMap" type="Blog">
   *         <constructor>
   *             <idArg column="blog_id" javaType="int"/>
   *             <arg column="username" javaType="String"/>
   *         </constructor>
   *         <result property="title" column="blog_title"/>
   *         <association property="author" javaType="Author">
   *             <id property="id" column="author_id"/>
   *             <result property="username" column="author_username"/>
   *             <result property="password" column="author_password"/>
   *             <result property="email" column="author_email"/>
   *             <result property="bio" column="author_bio"/>
   *             <result property="favouriteSection" column="author_favourite_section"/>
   *         </association>
   *         <collection property="posts" ofType="Post">
   *             <id property="id" column="post_id"/>
   *             <result property="subject" column="post_subject"/>
   *             <association property="author" javaType="Author"/>
   *             <collection property="comments" ofType="Comment">
   *                 <id property="id" column="comment_id"/>
   *             </collection>
   *             <collection property="tags" ofType="Tag" >
   *                 <id property="id" column="tag_id"/>
   *             </collection>
   *             <discriminator javaType="int" column="draft">
   *                 <case value="1" resultType="DraftPost"/>
   *             </discriminator>
   *         </collection>
   *     </resultMap>
   *
   * 单个ResultMap解析
   * @param resultMapNode resultMap节点
   * @param additionalResultMappings
   * @param enclosingType
   * @return
   * @throws Exception
   */
  private ResultMap resultMapElement(XNode resultMapNode, List<ResultMapping> additionalResultMappings, Class<?> enclosingType) throws Exception {
    ErrorContext.instance().activity("processing " + resultMapNode.getValueBasedIdentifier());
    //// 获取 type 属性， 表示结果集将被映射为 type 指定类型的对象
    String type = resultMapNode.getStringAttribute("type",
        resultMapNode.getStringAttribute("ofType",
            resultMapNode.getStringAttribute("resultType",
                resultMapNode.getStringAttribute("javaType"))));
    //拿到别名对应的类
    Class<?> typeClass = resolveClass(type);
    if (typeClass == null) {
      typeClass = inheritEnclosingType(resultMapNode, enclosingType);
    }
    Discriminator discriminator = null;
    List<ResultMapping> resultMappings = new ArrayList<>();
    //把额外的ResultMap添加到resultMappings中
    resultMappings.addAll(additionalResultMappings);
    //获取ResultMap的所有一级子节点
    List<XNode> resultChildren = resultMapNode.getChildren();
    //遍历这些子节点
    for (XNode resultChild : resultChildren) {
      if ("constructor".equals(resultChild.getName())) {
        //处理constructor元素
        processConstructorElement(resultChild, typeClass, resultMappings);
      } else if ("discriminator".equals(resultChild.getName())) {
        //处理鉴别器
        discriminator = processDiscriminatorElement(resultChild, typeClass, resultMappings);
      } else {
        List<ResultFlag> flags = new ArrayList<>();
        //处理ID标签
        if ("id".equals(resultChild.getName())) {
          flags.add(ResultFlag.ID);
        }
        resultMappings.add(buildResultMappingFromContext(resultChild, typeClass, flags));
      }
    }
    //获取Id， 默认值会拼装所有父节点的 id 或 value 或 property
    String id = resultMapNode.getStringAttribute("id",
            resultMapNode.getValueBasedIdentifier());
    // 获取 extends 属性， 其表示结果集的继承
    String extend = resultMapNode.getStringAttribute("extends");
    // 自动映射属性。 将列名自动映射为属性
    Boolean autoMapping = resultMapNode.getBooleanAttribute("autoMapping");
    //把解析出来的属性扔给ResultMapResolver处理，顾名思义，ResultMapResolver就是专门来处理ResultMap的
    ResultMapResolver resultMapResolver = new ResultMapResolver(builderAssistant, id, typeClass, extend, discriminator, resultMappings, autoMapping);
    try {
      //把解析好的ResultMap返回
      return resultMapResolver.resolve();
    } catch (IncompleteElementException  e) {
      //如果报错了，把该解析器放到incompleteResultMaps中
      configuration.addIncompleteResultMap(resultMapResolver);
      throw e;
    }
  }

  protected Class<?> inheritEnclosingType(XNode resultMapNode, Class<?> enclosingType) {
    if ("association".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      String property = resultMapNode.getStringAttribute("property");
      if (property != null && enclosingType != null) {
        MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
        return metaResultType.getSetterType(property);
      }
    } else if ("case".equals(resultMapNode.getName()) && resultMapNode.getStringAttribute("resultMap") == null) {
      return enclosingType;
    }
    return null;
  }

  /**
   * <constructor>
   *    <idArg column="id" javaType="int"/>
   *    <arg column="username" javaType="String"/>
   *    <arg column="age" javaType="_int"/>
   * </constructor>
   * @param resultChild
   * @param resultType
   * @param resultMappings
   * @throws Exception
   */
  private void processConstructorElement(XNode resultChild, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    List<XNode> argChildren = resultChild.getChildren();
    for (XNode argChild : argChildren) {
      List<ResultFlag> flags = new ArrayList<>();
      flags.add(ResultFlag.CONSTRUCTOR);
      if ("idArg".equals(argChild.getName())) {
        flags.add(ResultFlag.ID);
      }
      resultMappings.add(buildResultMappingFromContext(argChild, resultType, flags));
    }
  }

  /**
   *   <discriminator javaType="int" column="vehicle_type">
   *     <case value="1" resultMap="carResult"/>
   *     <case value="2" resultMap="truckResult"/>
   *     <case value="3" resultMap="vanResult"/>
   *     <case value="4" resultMap="suvResult"/>
   *   </discriminator>
   * @param context
   * @param resultType
   * @param resultMappings
   * @return
   * @throws Exception
   */
  private Discriminator processDiscriminatorElement(XNode context, Class<?> resultType, List<ResultMapping> resultMappings) throws Exception {
    String column = context.getStringAttribute("column");
    String javaType = context.getStringAttribute("javaType");
    String jdbcType = context.getStringAttribute("jdbcType");
    String typeHandler = context.getStringAttribute("typeHandler");
    //获取javaType的Class
    Class<?> javaTypeClass = resolveClass(javaType);
    //获取 TypeHandler的class
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    //获取Jdbc类型
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    Map<String, String> discriminatorMap = new HashMap<>();
    //变量case节点
    for (XNode caseChild : context.getChildren()) {
      //获取值
      String value = caseChild.getStringAttribute("value");
      //获取resultMap的ID
      String resultMap = caseChild.getStringAttribute("resultMap", processNestedResultMappings(caseChild, resultMappings, resultType));
      discriminatorMap.put(value, resultMap);
    }
    return builderAssistant.buildDiscriminator(resultType, column, javaTypeClass, jdbcTypeEnum, typeHandlerClass, discriminatorMap);
  }

  private void sqlElement(List<XNode> list) {
    if (configuration.getDatabaseId() != null) {
      sqlElement(list, configuration.getDatabaseId());
    }
    sqlElement(list, null);
  }

  private void sqlElement(List<XNode> list, String requiredDatabaseId) {
    for (XNode context : list) {
      String databaseId = context.getStringAttribute("databaseId");
      String id = context.getStringAttribute("id");
      id = builderAssistant.applyCurrentNamespace(id, false);
      if (databaseIdMatchesCurrent(id, databaseId, requiredDatabaseId)) {
        sqlFragments.put(id, context);
      }
    }
  }

  private boolean databaseIdMatchesCurrent(String id, String databaseId, String requiredDatabaseId) {
    if (requiredDatabaseId != null) {
      if (!requiredDatabaseId.equals(databaseId)) {
        return false;
      }
    } else {
      if (databaseId != null) {
        return false;
      }
      // skip this fragment if there is a previous one with a not null databaseId
      if (this.sqlFragments.containsKey(id)) {
        XNode context = this.sqlFragments.get(id);
        if (context.getStringAttribute("databaseId") != null) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * <arg column="username" javaType="String"/>
   * <result property="subject" column="post_subject"/>
   * <collection property="posts" javaType="ArrayList" column="id" ofType="Post" select="selectPostsForBlog"/>
   * <discriminator javaType="int" column="vehicle_type">
   * <association property="author" javaType="Author" resultSet="authors" column="author_id" foreignColumn="id">
   *
   * @param context 获取每个字节点的所有属性
   * @param resultType
   * @param flags
   * @return
   * @throws Exception
   */
  private ResultMapping buildResultMappingFromContext(XNode context, Class<?> resultType, List<ResultFlag> flags) throws Exception {
    String property;
    if (flags.contains(ResultFlag.CONSTRUCTOR)) {
      //如果是构造函数形式，获取name属性的值,也就是java的构造方法形参的名字
      property = context.getStringAttribute("name");
    } else {
      //如果不是构造函数形式，获取property属性的值,也就是java的字段
      property = context.getStringAttribute("property");
    }
    //数据库中的列名，或者是列的别名。
    String column = context.getStringAttribute("column");
    //一个 Java 类的完全限定名，或一个类型别名,java bean可以自动推断，hashmap则必须手动指定
    String javaType = context.getStringAttribute("javaType");
    //JDBC 类型，只需要对可能存在空值的列指定这个类型。
    String jdbcType = context.getStringAttribute("jdbcType");
    //嵌套的select，用于加载复杂类型属性的映射语句的 ID
    String nestedSelect = context.getStringAttribute("select");
    //嵌套的ResultMap ID，结果映射的 ID，可以将嵌套的结果集映射到一个合适的对象树中。
    //它可以作为使用额外 select 语句的替代方案。
    // 它可以将多表连接操作的结果映射成一个单一的 ResultSet。这样的 ResultSet 有部分数据是重复的。
    // 为了将结果集正确地映射到嵌套的对象树中, MyBatis 允许你“串联”结果映射，以便解决嵌套结果集的问题。
    String nestedResultMap = context.getStringAttribute("resultMap",
        processNestedResultMappings(context, Collections.emptyList(), resultType));
    //只有association和collection有这个属性。 默认情况下，在至少一个被映射到属性的列不为空时，子对象才会被创建。
    // 你可以在这个属性上指定非空的列来改变默认行为，指定后，Mybatis 将只在这些列非空时才创建一个子对象。
    // 可以使用逗号分隔来指定多个列。默认值：未设置（unset）。
    String notNullColumn = context.getStringAttribute("notNullColumn");
    //当连接多个表时，你可能会不得不使用列别名来避免在 ResultSet 中产生重复的列名。
    // 指定 columnPrefix 列名前缀允许你将带有这些前缀的列映射到一个外部的结果映射中。
    String columnPrefix = context.getStringAttribute("columnPrefix");
    //读取typeHandler
    String typeHandler = context.getStringAttribute("typeHandler");
    //获取 resultSet，指定用于加载复杂类型的结果集名字。主要针对的是不使用连接的情况，多个sql语句，返回多个结果
    String resultSet = context.getStringAttribute("resultSet");
    //指定外键对应的列名，指定的列将与父类型中 column 的给出的列进行匹配。
    String foreignColumn = context.getStringAttribute("foreignColumn");
    //可选的。有效值为 lazy 和 eager。 指定属性后，将在映射中忽略全局配置参数 lazyLoadingEnabled，使用属性的值。
    boolean lazy = "lazy".equals(context.getStringAttribute("fetchType", configuration.isLazyLoadingEnabled() ? "lazy" : "eager"));
    //获取javaType的class
    Class<?> javaTypeClass = resolveClass(javaType);
    //获取typeHandler的class
    Class<? extends TypeHandler<?>> typeHandlerClass = resolveClass(typeHandler);
    //获取JdbcType的枚举
    JdbcType jdbcTypeEnum = resolveJdbcType(jdbcType);
    return builderAssistant.buildResultMapping(resultType, property, column, javaTypeClass, jdbcTypeEnum, nestedSelect, nestedResultMap, notNullColumn, columnPrefix, typeHandlerClass, flags, resultSet, foreignColumn, lazy);
  }

  /**
   * 处理嵌套的ResultMap
   * @param context
   * @param resultMappings
   * @param enclosingType
   * @return
   * @throws Exception
   */
  private String processNestedResultMappings(XNode context, List<ResultMapping> resultMappings, Class<?> enclosingType) throws Exception {
    //如果是1对1，1对多
    if ("association".equals(context.getName())
        || "collection".equals(context.getName())
        || "case".equals(context.getName())) {
      //如果没有select属性
      if (context.getStringAttribute("select") == null) {
        //校验集合是否有set方法
        validateCollection(context, enclosingType);
        //获取嵌套的association或者collection或者case标签下的内容
        ResultMap resultMap = resultMapElement(context, resultMappings, enclosingType);
        return resultMap.getId();
      }
    }
    return null;
  }

  protected void validateCollection(XNode context, Class<?> enclosingType) {
    if ("collection".equals(context.getName()) && context.getStringAttribute("resultMap") == null
        && context.getStringAttribute("javaType") == null) {
      MetaClass metaResultType = MetaClass.forClass(enclosingType, configuration.getReflectorFactory());
      //获取property，判断类中是否有该属性的set方法，如果没有就抛错吧
      String property = context.getStringAttribute("property");
      if (!metaResultType.hasSetter(property)) {
        throw new BuilderException(
          "Ambiguous collection type for property '" + property + "'. You must specify 'javaType' or 'resultMap'.");
      }
    }
  }

  private void bindMapperForNamespace() {
    String namespace = builderAssistant.getCurrentNamespace();
    if (namespace != null) {
      Class<?> boundType = null;
      try {
        boundType = Resources.classForName(namespace);
      } catch (ClassNotFoundException e) {
        //ignore, bound type is not required
      }
      if (boundType != null) {
        if (!configuration.hasMapper(boundType)) {
          // Spring may not know the real resource name so we set a flag
          // to prevent loading again this resource from the mapper interface
          // look at MapperAnnotationBuilder#loadXmlResource
          configuration.addLoadedResource("namespace:" + namespace);
          configuration.addMapper(boundType);
        }
      }
    }
  }

}
