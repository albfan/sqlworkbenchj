/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.dependency;

import java.util.List;

import workbench.db.DbObject;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public interface DependencyReader
{
  /**
   * Return a list of objects that the base object uses.
   *
   *
   * @param base  the base object to check for
   * @return a list of objects that depend on <tt>base</tt> or on which <tt>base</tt> depends on.
   */
  List<DbObject> getUsedObjects(WbConnection connection, DbObject base);

  /**
   * Return a list of objects that are using the base object.
   *
   * @param base  the base object to check for
   * @return a list of objects that depend on <tt>base</tt> or on which <tt>base</tt> depends on.
   */
  List<DbObject> getUsedBy(WbConnection connection, DbObject base);

  boolean supportsDependencies(String objectType);
}
