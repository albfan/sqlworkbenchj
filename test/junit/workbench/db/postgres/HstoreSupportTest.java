/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class HstoreSupportTest
{

  public HstoreSupportTest()
  {
  }

  @Test
  public void testParse()
  {
    Map<String, String> data = new HashMap<>();
    data.put("key", "value");
    Map<String, String> result = HstoreSupport.parseLiteral("key=>value");
    assertEquals(data, result);

    data.clear();
    data.put("some key", "some value");
    data.put("no value", null);
    String literal = HstoreSupport.getDisplay(data);
    result = HstoreSupport.parseLiteral(literal);
    assertEquals(data, result);

    result = HstoreSupport.parseLiteral(null);
    assertNull(result);

    result = HstoreSupport.parseLiteral("\"foo\"=>\"bar\"");
    assertEquals(1, result.size());
    assertEquals("bar", result.get("foo"));

    result = HstoreSupport.parseLiteral("\"foo\"=>\"bar\\\"value\"");
    assertEquals(1, result.size());
    assertEquals("bar\"value", result.get("foo"));

    result = HstoreSupport.parseLiteral("key=>\"null\"");
    assertEquals(1, result.size());
    assertEquals("null", result.get("key"));

    result = HstoreSupport.parseLiteral("key=>null");
    assertEquals(1, result.size());
    assertTrue(result.containsKey("key"));
    assertNull(result.get("key"));
  }

  @Test
  public void testToString()
  {
    Map<String, String> data = new LinkedHashMap<>();
    data.put("foo", "bar");
    data.put("color", "blue");
    String literal = HstoreSupport.getLiteral(data);
    assertEquals("'\"foo\"=>\"bar\", \"color\"=>\"blue\"'::hstore", literal);

    literal = HstoreSupport.getDisplay(data);
    assertEquals("\"foo\"=>\"bar\", \"color\"=>\"blue\"", literal);

    data.clear();
    data.put("content", "foo bar");
    data.put("location", "Peter's House");

    literal = HstoreSupport.getLiteral(data);
    assertEquals("'\"content\"=>\"foo bar\", \"location\"=>\"Peter''s House\"'::hstore", literal);

    literal = HstoreSupport.getDisplay(data);
    assertEquals("\"content\"=>\"foo bar\", \"location\"=>\"Peter's House\"", literal);

    data.clear();
    data.put("somekey", "foo\"bar");
    literal = HstoreSupport.getDisplay(data);
    assertEquals("\"somekey\"=>\"foo\\\"bar\"", literal);
  }

}
