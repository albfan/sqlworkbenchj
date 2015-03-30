/*
 * ComparatorFactoryTest.java
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

import workbench.util.ClassFinder;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class ComparatorFactoryTest
{

	public ComparatorFactoryTest()
	{
	}

	@Test
	public void testFactory()
		throws Exception
	{
		List<Class> classList = ClassFinder.getClasses("workbench.storage.filter");

		int count = 0;
		for (Class clz : classList)
		{
			if (!clz.isInterface() && ColumnComparator.class.isAssignableFrom(clz))
			{
				//System.out.println(clz.getName()  + " is a comparator");
				count ++;
			}
		}

		// Make sure all comparators are returned by the factory
		ComparatorFactory factory = new ComparatorFactory();
		List<ColumnComparator> comps = factory.getAvailableComparators();
		assertNotNull(comps);
		assertEquals("Not all comparators regisgered!", count, comps.size());

		ColumnComparator comp = factory.findEqualityComparatorFor(String.class);
		assertTrue(comp instanceof StringEqualsComparator);

		comp = factory.findEqualityComparatorFor(Date.class);
		assertTrue(comp instanceof DateEqualsComparator);

		comp = factory.findEqualityComparatorFor(Boolean.class);
		assertTrue(comp instanceof BooleanEqualsComparator);

	}

}
