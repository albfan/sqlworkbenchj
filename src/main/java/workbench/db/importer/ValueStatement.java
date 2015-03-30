/*
 * ValueStatement.java
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
package workbench.db.importer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.db.WbConnection;

import workbench.sql.lexer.SQLLexer;
import workbench.sql.lexer.SQLLexerFactory;
import workbench.sql.lexer.SQLToken;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ValueStatement
{
	private Map<Integer, Integer> columnIndexMap;
	private PreparedStatement select;
	private String selectSql;

	private Pattern columnReference = Pattern.compile("\\$[0-9]+");

	public ValueStatement(String sql)
	{
		StringBuilder newSql = new StringBuilder(sql.length());
		SQLLexer lexer = SQLLexerFactory.createLexer(sql);
		SQLToken t = lexer.getNextToken(false, true);
		int currentIndex = 1;
		columnIndexMap = new HashMap<>();
		while (t != null)
		{
			String text = t.getText();
			Matcher m = columnReference.matcher(text);

			if (m.matches())
			{
				newSql.append('?');
				columnIndexMap.put(Integer.valueOf(text.substring(1)), currentIndex);
				currentIndex ++;
			}
			else
			{
				newSql.append(t.getText());
			}
			t = lexer.getNextToken(false, true);
		}
		selectSql = newSql.toString();
	}

	public void done()
	{
		if (select != null)
		{
			SqlUtil.closeStatement(select);
			select = null;
		}
	}

	protected String getSelectSQL()
	{
		return selectSql;
	}

	protected int getIndexInStatement(int inputColumnIndex)
	{
		Integer index = columnIndexMap.get(inputColumnIndex);
		if (index == null) return -1;
		return index.intValue();
	}

	public void prepareSelect(WbConnection con)
		throws SQLException
	{
		select = con.getSqlConnection().prepareStatement(selectSql);
	}

	public Object getDatabaseValue(WbConnection con, Map<Integer, Object> columnValues)
		throws SQLException
	{
		if (select == null)
		{
			prepareSelect(con);
		}

		for (Map.Entry<Integer, Object> entry : columnValues.entrySet())
		{
			int index = getIndexInStatement(entry.getKey());
			if (index > -1)
			{
				select.setObject(index, entry.getValue());
			}
		}
		Object result = null;
		ResultSet rs = null;

		try
		{
			rs = select.executeQuery();
			if (rs.next())
			{
				result = rs.getObject(1);
			}
		}
		finally
		{
			SqlUtil.closeResult(rs);
		}
		return result;
	}

	public Set<Integer> getInputColumnIndexes()
	{
		return Collections.unmodifiableSet(columnIndexMap.keySet());
	}
}
