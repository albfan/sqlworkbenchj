/*
 * LikeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.*;

public class LikeTest
	extends TestCase
{
	
	public LikeTest(String name)
	{
		super(name);
	}
	
	public void testExpression()
	{
		Like pattern = new Like("%COMPANYUSERROLE%", true);
		boolean result = pattern.like("vuo_companyuserrole");
		assertEquals("Pattern not recognized", true, result);
		
		pattern = new Like("%COMPANYUSERROLE%", false);
		result = pattern.like("vuo_companyuserrole");
		assertEquals("Pattern not recognized", false, result);
		
	}
	
}
