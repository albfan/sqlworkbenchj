/*
 * PropertiesCopierTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
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
