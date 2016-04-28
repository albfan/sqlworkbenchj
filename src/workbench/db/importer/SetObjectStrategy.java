/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

/**
 *
 * An enum that defines how setObject() should be used for a PreparedStatement.
 *
 * @author Thomas Kellerer
 */
public enum SetObjectStrategy
{
  /**
   * Never use the type information.
   * i.e. always use PreparedStatement.setObject(int, Object)
   */
  Never,

  /**
   * Only use it for types different then {@link java.sql.Types.OTHER}.
   * i.e. only use PreparedStatement.setObject(int, Object, int) if the type is different then Types.OTHER
   */
  KnownTypes,
  
  /**
   * Always use the type information.
   * i.e. always use PreparedStatement.setObject(int, Object, int)
   */
  Always
}
