/*
 * NameUtilTest.java
 *
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
    assertEquals("camel_case_to_snake", NameUtil.camelCaseToSnake("camelCaseToSnake"));
    assertEquals("foo_bar", NameUtil.camelCaseToSnake("FooBar"));
    assertEquals("foobar", NameUtil.camelCaseToSnake("FOOBAR"));
    assertEquals("foobar", NameUtil.camelCaseToSnake("foobar"));
    assertEquals("foo_bar", NameUtil.camelCaseToSnake("foo-bar"));
    assertEquals("foo_bar20", NameUtil.camelCaseToSnake("foo-bar20"));
    assertEquals("some_things_are_stupid", NameUtil.camelCaseToSnake("some things Are Stupid"));
  }
  
  @Test
  public void cleanupIdentifier()
  {
    assertEquals("foobar", NameUtil.cleanupIdentifier("foo bar ", "true"));
    assertEquals("foobar", NameUtil.cleanupIdentifier("FooBar", "true"));
    assertEquals("FooBar", NameUtil.cleanupIdentifier("-:FooBar", "false"));
  }

}
