/*
 * ParameterDefinitionTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ParameterDefinitionTest
	extends WbTestCase
{

	public ParameterDefinitionTest()
	{
		super("ParameterDefinitionTest");
	}

	@Test
	public void testParameter()
	{
		ParameterDefinition varcharDef = new ParameterDefinition(1, Types.VARCHAR);
		varcharDef.setParameterName("FIRSTNAME");
		assertTrue(varcharDef.isValueValid("Test"));
		assertTrue(varcharDef.isValueValid("5"));
		assertEquals(1, varcharDef.getIndex());
		assertEquals(Types.VARCHAR, varcharDef.getType());

		ParameterDefinition intDef = new ParameterDefinition(5, Types.INTEGER);
		intDef.setParameterName("PERSON_ID");
		assertFalse(intDef.isValueValid("Test"));
		assertTrue(intDef.isValueValid("5"));
		assertEquals(5, intDef.getIndex());
		assertEquals(Types.INTEGER, intDef.getType());

		ParameterDefinition dateDef = new ParameterDefinition(6, Types.DATE);
		dateDef.setParameterName("HIRE_DATE");
		assertTrue(dateDef.isValueValid("2010-01-01"));
		assertFalse(dateDef.isValueValid("12345"));
		assertEquals(6, dateDef.getIndex());
		assertEquals(Types.DATE, dateDef.getType());
	}

}
