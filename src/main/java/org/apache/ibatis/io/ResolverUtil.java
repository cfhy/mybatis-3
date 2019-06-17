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
package org.apache.ibatis.io;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;

/**
 * <p>ResolverUtil is used to locate classes that are available in the/a class path and meet
 * arbitrary conditions. The two most common conditions are that a class implements/extends
 * another class, or that is it annotated with a specific annotation. However, through the use
 * of the {@link Test} class it is possible to search using arbitrary conditions.</p>
 *
 * <p>A ClassLoader is used to locate all locations (directories and jar files) in the class
 * path that contain classes within certain packages, and then to load those classes and
 * check them. By default the ClassLoader returned by
 * {@code Thread.currentThread().getContextClassLoader()} is used, but this can be overridden
 * by calling {@link #setClassLoader(ClassLoader)} prior to invoking any of the {@code find()}
 * methods.</p>
 *
 * <p>General searches are initiated by calling the
 * {@link #find(org.apache.ibatis.io.ResolverUtil.Test, String)} ()} method and supplying
 * a package name and a Test instance. This will cause the named package <b>and all sub-packages</b>
 * to be scanned for classes that meet the test. There are also utility methods for the common
 * use cases of scanning multiple packages for extensions of particular classes, or classes
 * annotated with a specific annotation.</p>
 *
 * <p>The standard usage pattern for the ResolverUtil class is as follows:</p>
 *
 * <pre>
 * ResolverUtil&lt;ActionBean&gt; resolver = new ResolverUtil&lt;ActionBean&gt;();
 * resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 * resolver.find(new CustomTest(), pkg1);
 * resolver.find(new CustomTest(), pkg2);
 * Collection&lt;ActionBean&gt; beans = resolver.getClasses();
 * </pre>
 *
 * @author Tim Fennell
 *
 *
 *
 * ResolverUtil用于定位某个类路径中可用的类，并满足任意条件。
 * 两个最常见的条件是类实现或继承另一个类。或者它是用特定注解注解的。
 * 但是，通过使用类，可以使用任意条件进行搜索
 *
 * ClassLoader用于定位类路径中类和包所在位置（目录和jar文件）
 * 然后加载并且检查这些类。默认情况下使用Thread.currentThread().getContextClassLoader()
 * 但是可以通过在调用find()之前调用setClassLoader()覆盖掉默认的ClassLoader
 *
 * 通过调用find()方法来启动常规搜索，该方法定义如下：ResolverUtil<T> find(Test test, String packageName)，
 * 可以看到需要提供Test实例以及包名称，该方法会扫描命名包和所有子包查找符合Test的类。
 * ResolverUtil也提供了工具方法：
 *   使用扫描多个包的情况来扩展特定类或类
 *   使用特定注解进行注解
 *
 * ResolverUtil的通用用法:
 *  ResolverUtil<ActionBean> resolver = new ResolverUtil<ActionBean>();
 *  resolver.findImplementation(ActionBean.class, pkg1, pkg2);
 *  resolver.find(new CustomTest(), pkg1);
 *  resolver.find(new CustomTest(), pkg2);
 *  Collection<ActionBean> beans = resolver.getClasses();
 */
public class ResolverUtil<T> {
  /*
   * An instance of Log to use for logging in this class.
   */
  private static final Log log = LogFactory.getLog(ResolverUtil.class);

  /**
   * A simple interface that specifies how to test classes to determine if they
   * are to be included in the results produced by the ResolverUtil.
   */
  public interface Test {
    /**
     * Will be called repeatedly with candidate classes. Must return True if a class
     * is to be included in the results, false otherwise.
     * Class<?> type 会重复调用。 如果要在结果中包含类，则必须返回True，否则返回false。
     */
    boolean matches(Class<?> type);
  }

  /**
   * A Test that checks to see if each class is assignable to the provided class. Note
   * that this test will match the parent type itself if it is presented for matching.
   * 判断每一个类是否是parent类的实现类（继承或实现），值得注意的是parent它自己也是匹配成功的。
   * 也就是说Object.class.isAssignableFrom(Object.class)返回true
   */
  public static class IsA implements Test {
    private Class<?> parent;

    /** Constructs an IsA test using the supplied Class as the parent class/interface. */
    public IsA(Class<?> parentType) {
      this.parent = parentType;
    }

    /** Returns true if type is assignable to the parent type supplied in the constructor. */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && parent.isAssignableFrom(type);
    }

    @Override
    public String toString() {
      return "is assignable to " + parent.getSimpleName();
    }
  }

  /**
   * A Test that checks to see if each class is annotated with a specific annotation. If it
   * is, then the test returns true, otherwise false.
   *
   * 用于检查每个类是否使用特定注解进行注解。 如果是，则测试返回true，否则返回false。
   */
  public static class AnnotatedWith implements Test {
    private Class<? extends Annotation> annotation;

    /** Constructs an AnnotatedWith test for the specified annotation type. */
    public AnnotatedWith(Class<? extends Annotation> annotation) {
      this.annotation = annotation;
    }

    /** Returns true if the type is annotated with the class provided to the constructor. */
    @Override
    public boolean matches(Class<?> type) {
      return type != null && type.isAnnotationPresent(annotation);
    }

    @Override
    public String toString() {
      return "annotated with @" + annotation.getSimpleName();
    }
  }

  /** The set of matches being accumulated.
   * 存放已经匹配的class
   */
  private Set<Class<? extends T>> matches = new HashSet<>();

  /**
   * The ClassLoader to use when looking for classes. If null then the ClassLoader returned
   * by Thread.currentThread().getContextClassLoader() will be used.
   * 用于寻找classes的ClassLoader，默认使用Thread.currentThread().getContextClassLoader()
   */
  private ClassLoader classloader;

  /**
   * Provides access to the classes discovered so far. If no calls have been made to
   * any of the {@code find()} methods, this set will be empty.
   *
   * @return the set of classes that have been discovered.
   *
   * 如果调用了find()方法，则返回匹配的类，否则返回空的hashSet
   */
  public Set<Class<? extends T>> getClasses() {
    return matches;
  }

  /**
   * Returns the classloader that will be used for scanning for classes. If no explicit
   * ClassLoader has been set by the calling, the context class loader will be used.
   *
   * @return the ClassLoader that will be used to scan for classes
   *
   * 如果classloader为空，则返回Thread.currentThread().getContextClassLoader()
   */
  public ClassLoader getClassLoader() {
    return classloader == null ? Thread.currentThread().getContextClassLoader() : classloader;
  }

  /**
   * Sets an explicit ClassLoader that should be used when scanning for classes. If none
   * is set then the context classloader will be used.
   *
   * @param classloader a ClassLoader to use when scanning for classes
   *
   * 明确的设置一个用来扫描类的ClassLoader
   */
  public void setClassLoader(ClassLoader classloader) {
    this.classloader = classloader;
  }

  /**
   * Attempts to discover classes that are assignable to the type provided. In the case
   * that an interface is provided this method will collect implementations. In the case
   * of a non-interface class, subclasses will be collected.  Accumulated classes can be
   * accessed by calling {@link #getClasses()}.
   *
   * 尝试发现所提供类型的类的实现类。
   * 在提供接口的情况下，该方法将收集实现。在非接口类的情况下，将收集子类。
   * 可以通过调用getClasses()来访问获取的类。
   *
   * @param parent the class of interface to find subclasses or implementations of
   * @param packageNames one or more package names to scan (including subpackages) for classes
   */
  public ResolverUtil<T> findImplementations(Class<?> parent, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new IsA(parent);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * Attempts to discover classes that are annotated with the annotation. Accumulated
   * classes can be accessed by calling {@link #getClasses()}.
   *
   * 尝试发现打了所提供注解的注解类。可以通过调用getClasses()来访问获取的类。
   *
   * @param annotation the annotation that should be present on matching classes
   * @param packageNames one or more package names to scan (including subpackages) for classes
   */
  public ResolverUtil<T> findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
    if (packageNames == null) {
      return this;
    }

    Test test = new AnnotatedWith(annotation);
    for (String pkg : packageNames) {
      find(test, pkg);
    }

    return this;
  }

  /**
   * Scans for classes starting at the package provided and descending into subpackages.
   * Each class is offered up to the Test as it is discovered, and if the Test returns
   * true the class is retained.  Accumulated classes can be fetched by calling
   * {@link #getClasses()}.
   *
   * @param test an instance of {@link Test} that will be used to filter classes
   * @param packageName the name of the package from which to start scanning for
   *        classes, e.g. {@code net.sourceforge.stripes}
   */
  public ResolverUtil<T> find(Test test, String packageName) {
    //获取包路径，也就是把.转为/
    String path = getPackagePath(packageName);
    //读取包下所有的文件
    try {
      List<String> children = VFS.getInstance().list(path);
      for (String child : children) {
        //筛选出class文件
        if (child.endsWith(".class")) {
          addIfMatching(test, child);
        }
      }
    } catch (IOException ioe) {
      log.error("Could not read package: " + packageName, ioe);
    }

    return this;
  }

  /**
   * Converts a Java package name to a path that can be looked up with a call to
   * {@link ClassLoader#getResources(String)}.
   *
   * @param packageName The Java package name to convert to a path
   */
  protected String getPackagePath(String packageName) {
    return packageName == null ? null : packageName.replace('.', '/');
  }

  /**
   * Add the class designated by the fully qualified class name provided to the set of
   * resolved classes if and only if it is approved by the Test supplied.
   *
   * @param test the test used to determine if the class matches
   * @param fqn the fully qualified name of a class
   */
  @SuppressWarnings("unchecked")
  protected void addIfMatching(Test test, String fqn) {
    try {
      //把路径转化为包
      String externalName = fqn.substring(0, fqn.indexOf('.')).replace('/', '.');
      ClassLoader loader = getClassLoader();
      if (log.isDebugEnabled()) {
        log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
      }
      Class<?> type = loader.loadClass(externalName);
      //如果满足匹配条件，则添加到matches
      if (test.matches(type)) {
        matches.add((Class<T>) type);
      }
    } catch (Throwable t) {
      log.warn("Could not examine class '" + fqn + "'" + " due to a " +
          t.getClass().getName() + " with message: " + t.getMessage());
    }
  }
}
