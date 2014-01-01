/*
 * PgCopyImporterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.db.postgres;


import java.io.BufferedReader;
import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.*;
import workbench.TestUtil;

import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.TextImportOptions;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.QuoteEscapeType;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class PgCopyImporterTest
	extends WbTestCase
{
	public PgCopyImporterTest()
	{
		super("PgCopyImporterTest");
	}

	@Test
	public void testImport()
		throws Exception
	{
		PostgresTestUtil.initTestCase("copy_importer");
		WbConnection conn = PostgresTestUtil.getPostgresConnection();
		if (conn == null) return;

		TestUtil.executeScript(conn,
			"create table foo (id integer, firstname text, lastname text);\n" +
			"commit;\n");

		TestUtil util = getTestUtil();
		File data = new File(util.getBaseDir(), "foo.txt");
		String content = "id|firstname|lastname\n1|Arthur|Dent\n2|Ford|Prefect\n";
		TestUtil.writeFile(data, content, "UTF-8");
		PgCopyImporter copy = new PgCopyImporter(conn);
		copy.setUseDefaultClassloader(true);

		TableDefinition def = conn.getMetadata().getTableDefinition(new TableIdentifier("foo"));
		BufferedReader in = EncodingUtil.createBufferedReader(data, "UTF-8");
		TextImportOptions options = createOptions();
		options.setTextDelimiter("|");
		options.setContainsHeader(true);
		copy.setup(def.getTable(), def.getColumns(), in, options);
		long rows = copy.processStreamData();
		conn.commit();
		assertEquals(2, rows);
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select count(*) from foo");
			if (rs.next())
			{
				rows = rs.getInt(1);
				assertEquals(2, rows);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
			PostgresTestUtil.cleanUpTestCase();
		}
	}

	@Test
	public void testCreateSql()
	{
		PgCopyImporter copy = new PgCopyImporter(null);
		TableIdentifier tbl = new TableIdentifier("foo");
		ColumnIdentifier id = new ColumnIdentifier("id");
		ColumnIdentifier name = new ColumnIdentifier("name");
		List<ColumnIdentifier> cols = CollectionUtil.arrayList(id, name);
		TextImportOptions options = createOptions();

		String sql = copy.createCopyStatement(tbl, cols, options);
		assertEquals("COPY foo (id,name) FROM stdin WITH (format csv, delimiter '|', header true)", sql);
		options.setContainsHeader(false);
		options.setTextDelimiter("\t");
		cols = CollectionUtil.arrayList(name, id);
		sql = copy.createCopyStatement(tbl, cols, options);
		assertEquals("COPY foo (name,id) FROM stdin WITH (format csv, delimiter '\t', header false)", sql);
	}

	private TextImportOptions createOptions()
	{
		return new TextImportOptions()
		{
			private boolean containsHeader = true;
			private String delimiter = "|";
			private String quote = "\"";

			@Override
			public String getTextDelimiter()
			{
				return delimiter;
			}

			@Override
			public void setTextDelimiter(String delim)
			{
				delimiter = delim;
			}

			@Override
			public boolean getContainsHeader()
			{
				return containsHeader;
			}

			@Override
			public void setContainsHeader(boolean flag)
			{
				containsHeader = flag;
			}

			@Override
			public String getTextQuoteChar()
			{
				return quote;
			}

			@Override
			public void setTextQuoteChar(String quote)
			{
				this.quote = quote;
			}

			@Override
			public boolean getDecode()
			{
				return false;
			}

			@Override
			public void setDecode(boolean flag)
			{
			}

			@Override
			public String getDecimalChar()
			{
				return ".";
			}

			@Override
			public void setDecimalChar(String s)
			{
			}

			@Override
			public QuoteEscapeType getQuoteEscaping()
			{
				return QuoteEscapeType.none;
			}

			@Override
			public boolean getQuoteAlways()
			{
				return false;
			}
		};
	}
}
