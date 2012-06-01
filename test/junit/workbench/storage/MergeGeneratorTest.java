/*
 * MergeGeneratorTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.util.Collection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class MergeGeneratorTest
{
	public MergeGeneratorTest()
	{
	}

	@Test
	public void testFactory()
	{
		Collection<String> types = MergeGenerator.Factory.getSupportedTypes();
		assertNotNull(types);
		for (String type : types)
		{
			MergeGenerator gen = MergeGenerator.Factory.createGenerator(type);
			assertNotNull(gen);
		}
	}

}
