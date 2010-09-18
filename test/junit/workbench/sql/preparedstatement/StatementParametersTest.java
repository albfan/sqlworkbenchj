/*
 * StatementParametersTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.preparedstatement;

import java.sql.Types;
import java.util.List;
import workbench.WbTestCase;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class StatementParametersTest
	extends WbTestCase
{

	public StatementParametersTest()
	{
		super("StatementParametersTest");
	}

	@Test
	public void testParameters()
	{
		List<ParameterDefinition> defs = CollectionUtil.arrayList();
		ParameterDefinition one = new ParameterDefinition(1, Types.VARCHAR);
		one.setParameterName("FIRSTNAME");
		defs.add(one);

		ParameterDefinition two = new ParameterDefinition(2, Types.INTEGER);
		two.setParameterName("PERSON_ID");
		defs.add(two);

		StatementParameters params = new StatementParameters(defs);
		assertEquals(2, params.getParameterCount());

		assertEquals("FIRSTNAME", params.getParameterName(0));
		assertEquals(Types.VARCHAR, params.getParameterType(0));

		assertEquals("PERSON_ID", params.getParameterName(1));
		assertEquals(Types.INTEGER, params.getParameterType(1));

		params.setParameterValue(0, "Arthur");
		params.setParameterValue(1, "42");
		assertEquals("Arthur", params.getParameterValue(0));
		assertEquals(Integer.valueOf(42), params.getParameterValue(1));
	}


}
