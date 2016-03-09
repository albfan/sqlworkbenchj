/*
 * ArgumentType.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.util;

/**
 * Available argument types for the ArgumentParser
 *
 * @author Thomas Kellerer
 */
public enum ArgumentType
{
  StringArgument,
  /**
   * Defines a boolean parameter. If this is set
   * the automcompletion for this parameter will
   * show true and false as possible values
   */
  BoolArgument,

  IntegerArgument,

  /**
   * A parameter that selects tables. If this is set
   * the autocompletion for this parameter will
   * show a table list
   */
  TableArgument,

  /**
   * A parameter that accepts a fixed set of values. The
   * values can be registered using ArgumentParser.addArgument(String, List)
   * @see ArgumentParser#addArgument(java.lang.String, java.util.List)
   */
  ListArgument,

  /**
   * A parameter that selects a connection profile. If this is defined
   * the autocompletion for this parameter will show all currently
   * defined connection profiles
   */
  ProfileArgument,

  /**
   * A parameter that may appear more than once. If it is specified several times,
   * the value for this parameter will be returned as a list. If a parameter
   * value contains a (comma separated) list, the elements of that list
   * will be returned individually.
   */
  Repeatable,

  /**
   * A parameter that may appear more than once but is never parsed for list values inside the parameter value.
   */
  RepeatableValue,

  /**
   * A parameter that shows available object types from the database
   */
  ObjectTypeArgument,

  /**
   * A parameter that shows available schemas
   */
  SchemaArgument,

  /**
   * A parameter that shows available catalogs (databases)
   */
  CatalogArgument,

  /**
   * A boolean argument which is treated as true if it's present
   * <tt>-foo</tt> is equivalent to <tt>-foo=true</tt>
   * To set it to false, <tt>-foo=false</tt> is required
   */
  BoolSwitch,

  Filename,

  DirName;
}
