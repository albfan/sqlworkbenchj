/*
 * StatementParametersTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.sql.preparedstatement;

import java.sql.Types;
import java.util.List;
import workbench.WbTestCase;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementParametersTest
	extends WbTestCase
{

	public StatementParametersTest()
	{
		super("StatementParametersTest");
	}

	@Test
	public void testParameters()
	{
		List<ParameterDefinition> defs = CollectionUtil.arrayList();
		ParameterDefinition one = new ParameterDefinition(1, Types.VARCHAR);
		one.setParameterName("FIRSTNAME");
		defs.add(one);

		ParameterDefinition two = new ParameterDefinition(2, Types.INTEGER);
		two.setParameterName("PERSON_ID");
		defs.add(two);

		StatementParameters params = new StatementParameters(defs);
		assertEquals(2, params.getParameterCount());

		assertEquals("FIRSTNAME", params.getParameterName(0));
		assertEquals(Types.VARCHAR, params.getParameterType(0));

		assertEquals("PERSON_ID", params.getParameterName(1));
		assertEquals(Types.INTEGER, params.getParameterType(1));

		params.setParameterValue(0, "Arthur");
		params.setParameterValue(1, "42");
		assertEquals("Arthur", params.getParameterValue(0));
		assertEquals(Integer.valueOf(42), params.getParameterValue(1));
	}


}
