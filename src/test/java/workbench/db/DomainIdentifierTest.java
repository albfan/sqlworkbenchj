/*
 * DomainIdentifierTest.java
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
package workbench.db;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DomainIdentifierTest
{

	@Test
	public void testDomain()
		throws Exception
	{
		DomainIdentifier domain = new DomainIdentifier("public", "pagila", "year");
		domain.setCheckConstraint("CHECK (VALUE >= 1901 AND VALUE <= 2155)");
		domain.setDataType("integer");
		domain.setDefaultValue(null);
		domain.setNullable(false);
		String source = domain.getSummary();
		assertEquals("integer NOT NULL CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);

		domain.setNullable(true);
		source = domain.getSummary();
		assertEquals("integer CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);

		domain.setDefaultValue("2009");
		domain.setNullable(false);
		source = domain.getSummary();
		assertEquals("integer NOT NULL DEFAULT 2009 CHECK (VALUE >= 1901 AND VALUE <= 2155);", source);
	}

}
