/*
 *    Copyright 2009-2011 The MyBatis Team
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
package com.ibatis.sqlmap;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import com.testdomain.*;

import java.util.List;
import java.util.Properties;

/*
 * @author Jeff Butler
 */
public class ResultObjectFactoryImpl implements ObjectFactory {

  /*
   *
   */
  public ResultObjectFactoryImpl() {
    super();
  }

  /* (non-Javadoc)
   * @see com.ibatis.sqlmap.engine.mapping.result.ResultObjectFactory#createInstance(java.lang.String, java.lang.Class)
   */
  public Object create(Class clazz) {

    Object obj = null;

    if (clazz.equals(IItem.class)) {
      obj = new IItemImpl();
    } else if (clazz.equals((ISupplier.class))) {
      obj = new ISupplierImpl();
    } else if (clazz.equals((ISupplierKey.class))) {
      obj = new ISupplierKeyImpl();
    }

    return obj;
  }

  public Object create(Class type, List<Class> constructorArgTypes, List<Object> constructorArgs) {
    return create(type);
  }

  public void setProperties(Properties properties) {
  }
}
