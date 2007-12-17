/*
 * AnsiSQLTokenMarker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.editor;

import java.util.Collection;
import workbench.sql.syntax.SqlKeywordHelper;
import workbench.sql.wbcommands.WbCall;
import workbench.sql.wbcommands.WbConfirm;
import workbench.sql.wbcommands.WbCopy;
import workbench.sql.wbcommands.WbDefinePk;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbDisableOraOutput;
import workbench.sql.wbcommands.WbEnableOraOutput;
import workbench.sql.wbcommands.WbEndBatch;
import workbench.sql.wbcommands.WbExport;
import workbench.sql.wbcommands.WbFeedback;
import workbench.sql.wbcommands.WbImport;
import workbench.sql.wbcommands.WbInclude;
import workbench.sql.wbcommands.WbListPkDef;
import workbench.sql.wbcommands.WbListVars;
import workbench.sql.wbcommands.WbLoadPkMapping;
import workbench.sql.wbcommands.WbRemoveVar;
import workbench.sql.wbcommands.WbSavePkMapping;
import workbench.sql.wbcommands.WbSchemaDiff;
import workbench.sql.wbcommands.WbSchemaReport;
import workbench.sql.wbcommands.WbSelectBlob;
import workbench.sql.wbcommands.WbStartBatch;
import workbench.sql.wbcommands.WbXslt;

/**
 * @author support@sql-workbench.net
 */
public class AnsiSQLTokenMarker 
	extends SQLTokenMarker
{
	public AnsiSQLTokenMarker()
	{
		super();
		initKeywordMap();
	}

	public void setSqlKeyWords(Collection<String> keywords)
	{
		this.addKeywordList(keywords, Token.KEYWORD1);
	}

	public void setSqlFunctions(Collection<String> functions)
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

	public void setIsMySQL(boolean flag)
	{
		this.isMySql = flag;
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

		// Workbench specific keywords
		keywords.add("DESC",Token.KEYWORD2);
		keywords.add("DESCRIBE",Token.KEYWORD2);
		keywords.add("WBLIST",Token.KEYWORD2);
		keywords.add("WBLISTPROCS",Token.KEYWORD2);
		keywords.add("WBLISTDB",Token.KEYWORD2);
		keywords.add("WBLISTCAT",Token.KEYWORD2);
		keywords.add(WbConfirm.VERB,Token.KEYWORD2);
		keywords.add(WbEnableOraOutput.VERB,Token.KEYWORD2);
		keywords.add(WbDisableOraOutput.VERB,Token.KEYWORD2);
		keywords.add(WbExport.VERB,Token.KEYWORD2);
		keywords.add(WbImport.VERB,Token.KEYWORD2);
		keywords.add(WbFeedback.VERB,Token.KEYWORD2);
		keywords.add(WbInclude.VERB,Token.KEYWORD2);
		keywords.add(WbCopy.VERB,Token.KEYWORD2);
		keywords.add(WbDefineVar.VERB_DEFINE_SHORT,Token.KEYWORD2);
		keywords.add(WbDefineVar.VERB_DEFINE_LONG,Token.KEYWORD2);
		keywords.add(WbListVars.VERB,Token.KEYWORD2);
		keywords.add(WbRemoveVar.VERB,Token.KEYWORD2);
		keywords.add(WbStartBatch.VERB,Token.KEYWORD2);
		keywords.add(WbEndBatch.VERB,Token.KEYWORD2);
		keywords.add(WbFeedback.VERB,Token.KEYWORD2);
		keywords.add(WbSchemaReport.VERB,Token.KEYWORD2);
		keywords.add(WbSchemaDiff.VERB,Token.KEYWORD2);
		keywords.add(WbXslt.VERB,Token.KEYWORD2);
		keywords.add(WbSelectBlob.VERB,Token.KEYWORD2);
		keywords.add(WbDefinePk.VERB,Token.KEYWORD2);
		keywords.add(WbListPkDef.VERB,Token.KEYWORD2);
		keywords.add(WbSavePkMapping.VERB,Token.KEYWORD2);
		keywords.add(WbLoadPkMapping.VERB,Token.KEYWORD2);
		keywords.add(WbCall.VERB, Token.KEYWORD2);
	}

	private void addDataTypes()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getDataTypes(), Token.KEYWORD1);
	}

	private void addSystemFunctions()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getSystemFunctions(), Token.KEYWORD3);
	}

	private void addOperators()
	{
		SqlKeywordHelper helper = new SqlKeywordHelper();
		addKeywordList(helper.getOperators(), Token.KEYWORD1);
	}
}