/*
 * ObjectSourceSearcherTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.search;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.util.CollectionUtil;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class ObjectSourceSearcherTest
	extends WbTestCase
{

	public ObjectSourceSearcherTest()
	{
		super("ObjectSourceSearcherTest");
	}

	@Test
	public void testSearchObjects()
		throws Exception
	{
		WbConnection con = getTestUtil().getConnection();
		try
		{
			TestUtil.executeScript(con, "create table person (nr integer not null, firstname varchar(100), lastname varchar(50));\n" +
				"alter table person add constraint pk_person primary key (nr);\n" +
				"create table address (id integer not null, person_id integer not null, address_info varchar(100));\n" +
				"alter table address add constraint pk_address primary key (id);\n" +
				"alter table address add constraint fk_address_person foreign key (person_id) references person(nr);\n" +
				"create view v_person_address as select p.firstname as person_first_name, p.lastname, a.address_info from person p \n" +
				"join address a on a.person_id = p.nr;" +
				"commit;\n");

			ObjectSourceSearcher searcher = new ObjectSourceSearcher(con);
			searcher.setTypesToSearch(CollectionUtil.arrayList("table"));
			List<DbObject> result = searcher.searchObjects(CollectionUtil.arrayList("fk_"), false, true, false);
			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("ADDRESS", result.get(0).getObjectName());

			searcher.setTypesToSearch(CollectionUtil.arrayList("table", "view"));
			result = searcher.searchObjects(CollectionUtil.arrayList("person_a"), false, true, false);
			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("V_PERSON_ADDRESS", result.get(0).getObjectName());

			searcher.setTypesToSearch(CollectionUtil.arrayList("table", "view"));
			result = searcher.searchObjects(CollectionUtil.arrayList("person"), false, true, false);
			assertNotNull(result);
			assertEquals(3, result.size());

			searcher.setTypesToSearch(CollectionUtil.arrayList("table", "view"));
			result = searcher.searchObjects(CollectionUtil.arrayList("varchar(50)", "integer"), true, true, false);
			assertNotNull(result);
			assertEquals(1, result.size());

			searcher.setTypesToSearch(CollectionUtil.arrayList("table", "view"));
			result = searcher.searchObjects(CollectionUtil.arrayList("person_first_name"), true, true, false);
			assertNotNull(result);
			assertEquals(1, result.size());
			assertEquals("V_PERSON_ADDRESS", result.get(0).getObjectName());
		}
		finally
		{
			con.disconnect();
		}
	}
}
