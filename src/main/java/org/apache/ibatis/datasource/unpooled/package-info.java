/**
 *    Copyright 2009-2015 the original author or authors.
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
/**
 * Hyper-simple Datasource.
 * 这个数据源的实现只是每次被请求时打开和关闭连接。虽然有点慢，
 * 但对于在数据库连接可用性方面没有太高要求的简单应用程序来说，是一个很好的选择。
 * 不同的数据库在性能方面的表现也是不一样的，对于某些数据库来说，
 * 使用连接池并不重要，这个配置就很适合这种情形。
 */
package org.apache.ibatis.datasource.unpooled;
