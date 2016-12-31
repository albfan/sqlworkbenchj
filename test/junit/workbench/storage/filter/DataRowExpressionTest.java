/*
 * DataRowExpressionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DataRowExpressionTest
{

	@Test
	public void testEvaluate()
		throws Exception
	{
		DataRowExpression expr = new DataRowExpression(new ContainsComparator(), "Zapho");
		expr.setIgnoreCase(true);
		assertTrue(expr.isIgnoreCase());

		Map<String, Object> values = new HashMap<String, Object>();
		values.put("firstname", "zaphod");
		values.put("lastname", "Beeblebrox");
		values.put("age", new Integer(43));
		values.put("spaceship", null);

		assertTrue(expr.evaluate(values));

		expr.setIgnoreCase(false);
		assertFalse(expr.isIgnoreCase());

		assertFalse(expr.evaluate(values));

		expr = new DataRowExpression(new ContainsComparator(), "Arthur");
		expr.setIgnoreCase(true);
		assertFalse(expr.evaluate(values));
		expr.setIgnoreCase(false);
		assertFalse(expr.evaluate(values));
	}

}
