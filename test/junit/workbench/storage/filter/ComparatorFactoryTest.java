/*
 * ComparatorFactoryTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
