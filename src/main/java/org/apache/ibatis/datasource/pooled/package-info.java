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
 * Simple single-thread pooled datasource
 * 这种数据源的实现利用“池”的概念将 JDBC 连接对象组织起来，
 * 避免了创建新的连接实例时所必需的初始化和认证时间。 这是一
 * 种使得并发 Web 应用快速响应请求的流行处理方式。
 */
package org.apache.ibatis.datasource.pooled;
