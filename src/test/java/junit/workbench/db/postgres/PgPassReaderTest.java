/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.io.File;
import java.io.IOException;

import workbench.TestUtil;
import workbench.WbTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class PgPassReaderTest
	extends WbTestCase
{

	public PgPassReaderTest()
	{
		super("PgPassReaderTest");
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	@Test
	public void parseUrl()
	{
		PgPassReader reader = new PgPassReader("jdbc:postgresql://localhost/postgres", "arthur");
		String result = reader.getHost();
		assertEquals("localhost", reader.getHost());
		assertEquals("postgres", reader.getDatabase());

		reader = new PgPassReader("jdbc:postgresql://192.193.0.0.1/postgres", "arthur");
		result = reader.getHost();
		assertEquals("192.193.0.0.1", result);
		assertEquals("postgres", reader.getDatabase());

		reader = new PgPassReader("jdbc:postgresql:postgres", "arthur");
		result = reader.getHost();
		assertEquals("localhost", result);
		assertEquals("postgres", reader.getDatabase());

		reader = new PgPassReader("jdbc:postgresql://hostname:5432/pgdb", "arthur");
		assertEquals("hostname", reader.getHost());
		assertEquals("pgdb", reader.getDatabase());
		assertEquals("5432", reader.getPort());
	}

	@Test
	public void testGetPassword()
		throws IOException
	{
		TestUtil util = getTestUtil();
		String dir = util.getBaseDir();
		File pgpass = new File(dir, "pgpass.conf");

		TestUtil.writeFile(pgpass,
			"localhost:*:*:arthur:secret\n" +
			"localhost:*:testdb:peter:password\n" +
			"localhost:*:*:peter:default\n" +
			"prodserver:5432:stage:peter:5432#\n" +
			"prodserver:5432:qa:peter:none\n" +
			"prodserver:1234:*:peter:1234#\n" +
			"", "ISO-8859-1");

		PgPassReader reader = new PgPassReader("jdbc:postgresql://localhost/postgres", "arthur");
		String pwd = reader.getPasswordFromFile(pgpass);
		assertEquals("secret", pwd);

		reader = new PgPassReader("jdbc:postgresql://localhost/testdb", "peter");
		pwd = reader.getPasswordFromFile(pgpass);
		assertEquals("password", pwd);

		reader = new PgPassReader("jdbc:postgresql://prodserver:1234/live-db", "peter");
		pwd = reader.getPasswordFromFile(pgpass);
		assertEquals("1234#", pwd);

		reader = new PgPassReader("jdbc:postgresql://prodserver:5432/stage", "peter");
		pwd = reader.getPasswordFromFile(pgpass);
		assertEquals("5432#", pwd);
	}

}
