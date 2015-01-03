/*
 * PropertiesCopierTest.java
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

import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PropertiesCopierTest
{

	public PropertiesCopierTest()
	{
	}

	@Test
	public void testSystemProps()
	{
		Properties source = new Properties();
		source.setProperty("workbench.test.prop1", "one");
		source.setProperty("workbench.test.prop2", "two");
		PropertiesCopier copier = new PropertiesCopier();
		copier.copyToSystem(source);
		assertEquals("one", System.getProperty("workbench.test.prop1"));
		assertEquals("two", System.getProperty("workbench.test.prop2"));
		copier.removeFromSystem(source);
		assertNull(System.getProperty("workbench.test.prop1"));
		assertNull(System.getProperty("workbench.test.prop2"));
	}

	@Test
	public void testCopy()
	{
		Properties source = new Properties();
		source.setProperty("workbench.test.prop1", "one");
		source.setProperty("workbench.test.prop2", "two");
		Properties target = new Properties();
		PropertiesCopier copier = new PropertiesCopier();
		copier.copy(source, target);
		assertEquals("one", target.getProperty("workbench.test.prop1"));
		assertEquals("two", target.getProperty("workbench.test.prop2"));
		copier.remove(source, target);
		assertEquals(0, target.size());
	}
}
