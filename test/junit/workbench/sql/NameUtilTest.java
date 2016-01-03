/*
 * NameUtilTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.sql;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class NameUtilTest
{

  public NameUtilTest()
  {
  }

  @Test
  public void testCamelCaseToSnake()
  {
    assertEquals("camel_case_to_snake", NameUtil.camelCaseToSnakeLower("camelCaseToSnake"));
    assertEquals("foo_bar", NameUtil.camelCaseToSnakeLower("FooBar"));
    assertEquals("foobar", NameUtil.camelCaseToSnakeLower("FOOBAR"));
    assertEquals("foobar", NameUtil.camelCaseToSnakeLower("foobar"));
    assertEquals("foo_bar", NameUtil.camelCaseToSnakeLower("foo-bar"));
    assertEquals("FOO_BAR", NameUtil.camelCaseToSnakeUpper("foo-bar"));
    assertEquals("foo_bar20", NameUtil.camelCaseToSnakeLower("foo-bar20"));
    assertEquals("some_things_are_stupid", NameUtil.camelCaseToSnakeLower("some things Are Stupid"));
    assertEquals("SOME_THINGS_ARE_STUPID", NameUtil.camelCaseToSnakeUpper("some things Are Stupid"));
  }

  @Test
  public void cleanupIdentifier()
  {
    assertEquals("foobar", NameUtil.cleanupIdentifier("foo bar ", "true"));
    assertEquals("foobar", NameUtil.cleanupIdentifier("FooBar", "true"));
    assertEquals("FooBar", NameUtil.cleanupIdentifier("-:FooBar", "false"));
  }

}
