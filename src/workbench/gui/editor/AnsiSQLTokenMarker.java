/*
 * AnsiSQLTokenMarker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import java.util.Collection;

import workbench.sql.syntax.SqlKeywordHelper;
import workbench.sql.wbcommands.CommandTester;

/**
 * @author Thomas Kellerer
 */
public class AnsiSQLTokenMarker
	extends SQLTokenMarker
{
	public AnsiSQLTokenMarker()
	{
		super();
		initKeywordMap();
	}

	public void addOperators(Collection<String> operators)
	{
		this.addKeywordList(operators, Token.KEYWORD1);
	}

	public void addDatatypes(Collection<String> types)
	{
		this.addKeywordList(types, Token.DATATYPE);
	}

	public void addSqlKeyWords(Collection<String> keywords)
	{
		this.addKeywordList(keywords, Token.KEYWORD1);
	}

	public void addSqlFunctions(Collection<String> functions)
	{
		this.addKeywordList(functions, Token.KEYWORD3);
	}

	private void addKeywordList(Collection<String> words, byte anId)
	{
		if (words == null) return;

		for (String keyword : words)
		{
			if (!keywords.containsKey(keyword))
			{
				keywords.add(keyword.toUpperCase().trim(),anId);
			}
		}
	}

	public void setIsMicrosoft(boolean flag)
	{
		isMicrosoft = flag;
	}

	public void setIsMySQL(boolean flag)
	{
		isMySql = flag;
	}

	public void initKeywordMap()
	{
		keywords = new KeywordMap(true, 80);
		addKeywords();
		addDataTypes();
		addSystemFunctions();
		addOperators();
	}

	private void addKeywords()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getKeywords(), Token.KEYWORD1);

		CommandTester tester = new CommandTester();
		for (String verb : tester.getCommands())
		{
			keywords.add(verb, Token.KEYWORD2);
		}
	}

	private void addDataTypes()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getDataTypes(), Token.DATATYPE);
	}

	private void addSystemFunctions()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getSqlFunctions(), Token.KEYWORD3);
	}

	private void addOperators()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getOperators(), Token.OPERATOR);
	}
}