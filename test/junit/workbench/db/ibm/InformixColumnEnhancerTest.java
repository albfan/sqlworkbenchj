/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013 Thomas Kellerer.
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

package workbench.db.ibm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author Thomas Kellerer
 */
public class InformixColumnEnhancerTest
{

	public InformixColumnEnhancerTest()
	{
	}

	@Test
	public void testGetQualifier()
	{
		InformixColumnEnhancer enhancer = new InformixColumnEnhancer();
		assertEquals("YEAR TO MINUTE", enhancer.getQualifier(3080));
		assertEquals("YEAR TO SECOND", enhancer.getQualifier(3594));
		assertEquals("HOUR TO SECOND", enhancer.getQualifier(1642));
		assertEquals("MONTH TO DAY", enhancer.getQualifier(1060));
		assertEquals("YEAR TO DAY", enhancer.getQualifier(2052));
	}

}
