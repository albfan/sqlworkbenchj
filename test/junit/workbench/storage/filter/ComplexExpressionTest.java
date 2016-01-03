/*
 * ComplexExpressionTest.java
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
package workbench.storage.filter;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class ComplexExpressionTest
{
	@Test
	public void testOrExpression()
	{
		ComplexExpression expr = new OrExpression();
		expr.addColumnExpression("firstname", new ContainsComparator(), "pho", true);
		expr.addColumnExpression("lastname", new ContainsComparator(), "ble", true);

		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		values.put("spaceship", null);
		assertTrue(expr.evaluate(values));
	}

	@Test
	public void testAndExpression()
		throws Exception
	{
		ComplexExpression expr = new AndExpression();
		expr.addColumnExpression("firstname", new StringEqualsComparator(), "Zaphod");
		expr.addColumnExpression("lastname", new StartsWithComparator(), "Bee");
		expr.addColumnExpression("age", new GreaterOrEqualComparator(), new Integer(42));

		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		assertTrue(expr.evaluate(values));

		values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(40));
		assertFalse(expr.evaluate(values));

		values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", null);
		values.put("age", new Integer(40));

		expr = new AndExpression();
		expr.addColumnExpression("lastname", new IsNullComparator(), null);
		assertTrue(expr.evaluate(values));
	}

}
