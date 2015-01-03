/*
 * FilteredPropertiesTest.java
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
package workbench.util;

import workbench.interfaces.PropertyStorage;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class FilteredPropertiesTest
{

	@Test
	public void testCopyTo()
	{
		PropertyStorage old = new WbProperties();
		old.setProperty("dbexplorer1.prop1", "first");
		old.setProperty("dbexplorer1.prop2", "second");
		old.setProperty("panel2.prop1", "third");

		FilteredProperties instance = new FilteredProperties(old, "dbexplorer1");
		PropertyStorage target = new WbProperties();

		instance.copyTo(target, "dbexplorer5");
		assertEquals("first", target.getProperty("dbexplorer5.prop1", ""));
		assertEquals("second", target.getProperty("dbexplorer5.prop2", ""));

		assertEquals("xxxx", target.getProperty("dbexplorer1.prop1", "xxxx"));
		assertEquals("xxxx", target.getProperty("panel2.prop1", "xxxx"));

	}
}
