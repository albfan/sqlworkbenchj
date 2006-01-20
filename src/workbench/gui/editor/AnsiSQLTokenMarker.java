package workbench.gui.editor;

import java.util.Collection;
import java.util.Iterator;
import workbench.sql.wbcommands.WbDefinePk;
import workbench.sql.wbcommands.WbDefineVar;
import workbench.sql.wbcommands.WbListPkDef;
import workbench.sql.wbcommands.WbLoadPkMapping;
import workbench.sql.wbcommands.WbSavePkMapping;
import workbench.sql.wbcommands.WbSelectBlob;

/**
 * ANSI-SQL token marker.
 *
 */
public class AnsiSQLTokenMarker extends SQLTokenMarker
{
	// public members
	public AnsiSQLTokenMarker()
	{
		super();
		initKeywordMap();
	}

	public void setSqlKeyWords(Collection keywords)
	{
		this.addKeywordList(keywords, Token.KEYWORD1);
	}
	
	public void setSqlFunctions(Collection functions)
	{
		this.addKeywordList(functions, Token.KEYWORD3);
	}
	
	private void addKeywordList(Collection words, byte anId)
	{
		if (words == null) return;
		Iterator itr = words.iterator();
		while (itr.hasNext())
		{
			String keyword = (String)itr.next();
			if (!keywords.containsKey(keyword))
			{
				//System.out.println("adding key=" + keyword);
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
		keywords.add("AVG",Token.KEYWORD1);
		keywords.add("ADD",Token.KEYWORD1);
		keywords.add("ALTER",Token.KEYWORD1);
		keywords.add("AS",Token.KEYWORD1);
		keywords.add("ASC",Token.KEYWORD1);
		keywords.add("BEGIN",Token.KEYWORD1);
		keywords.add("BREAK",Token.KEYWORD1);
		keywords.add("BY",Token.KEYWORD1);
		keywords.add("CASE",Token.KEYWORD1);
		keywords.add("CASCADE",Token.KEYWORD1);
		keywords.add("CHECK",Token.KEYWORD1);
		keywords.add("CHECKPOINT",Token.KEYWORD1);
		keywords.add("CLOSE",Token.KEYWORD1);
		keywords.add("CLUSTERED",Token.KEYWORD1);
		keywords.add("COLUMN",Token.KEYWORD1);
		keywords.add("COMMIT",Token.KEYWORD1);
		keywords.add("CONSTRAINT",Token.KEYWORD1);
		keywords.add("CREATE",Token.KEYWORD1);
		keywords.add("CURRENT",Token.KEYWORD1);
		keywords.add("CURRENT_DATE",Token.KEYWORD1);
		keywords.add("CURRENT_TIME",Token.KEYWORD1);
		keywords.add("CURRENT_TIMESTAMP",Token.KEYWORD1);
		keywords.add("CURSOR",Token.KEYWORD1);
		keywords.add("DATABASE",Token.KEYWORD1);
		keywords.add("DECLARE",Token.KEYWORD1);
		keywords.add("DEFAULT",Token.KEYWORD1);
		keywords.add("DELETE",Token.KEYWORD1);
		keywords.add("DENY",Token.KEYWORD1);
		keywords.add("DISTINCT",Token.KEYWORD1);
		keywords.add("DROP",Token.KEYWORD1);
		keywords.add("EXEC",Token.KEYWORD1);
		keywords.add("EXECUTE",Token.KEYWORD1);
		keywords.add("EXIT",Token.KEYWORD1);
		keywords.add("END",Token.KEYWORD1);
		keywords.add("ELSE",Token.KEYWORD1);
		keywords.add("FETCH",Token.KEYWORD1);
		keywords.add("FOR",Token.KEYWORD1);
		keywords.add("FOREIGN",Token.KEYWORD1);
		keywords.add("FROM",Token.KEYWORD1);
		keywords.add("GRANT",Token.KEYWORD1);
		keywords.add("GROUP",Token.KEYWORD1);
		keywords.add("HAVING",Token.KEYWORD1);
		keywords.add("IF",Token.KEYWORD1);
		keywords.add("INDEX",Token.KEYWORD1);
		keywords.add("INNER",Token.KEYWORD1);
		keywords.add("INSERT",Token.KEYWORD1);
		keywords.add("INTO",Token.KEYWORD1);
		keywords.add("IS",Token.KEYWORD1);
		keywords.add("ISOLATION",Token.KEYWORD1);
		keywords.add("KEY",Token.KEYWORD1);
		keywords.add("LEVEL",Token.KEYWORD1);
		keywords.add("MAX",Token.KEYWORD1);
		keywords.add("MIN",Token.KEYWORD1);
		keywords.add("MIRROREXIT",Token.KEYWORD1);
		keywords.add("NATIONAL",Token.KEYWORD1);
		keywords.add("NOCHECK",Token.KEYWORD1);
		keywords.add("OF",Token.KEYWORD1);
		keywords.add("ON",Token.KEYWORD1);
		keywords.add("ORDER",Token.KEYWORD1);
		keywords.add("PREPARE",Token.KEYWORD1);
		keywords.add("PRIMARY",Token.KEYWORD1);
		keywords.add("PRIVILEGES",Token.KEYWORD1);
		keywords.add("PROCEDURE",Token.KEYWORD1);
		keywords.add("FUNCTION",Token.KEYWORD1);
		keywords.add("PACKAGE",Token.KEYWORD1);
		keywords.add("BODY",Token.KEYWORD1);
		keywords.add("REFERENCES",Token.KEYWORD1);
		keywords.add("RESTORE",Token.KEYWORD1);
		keywords.add("RESTRICT",Token.KEYWORD1);
		keywords.add("REVOKE",Token.KEYWORD1);
		keywords.add("ROLLBACK",Token.KEYWORD1);
		keywords.add("SCHEMA",Token.KEYWORD1);
		keywords.add("SELECT",Token.KEYWORD1);
		keywords.add("SET",Token.KEYWORD1);
		keywords.add("TABLE",Token.KEYWORD1);
		keywords.add("TO",Token.KEYWORD1);
		keywords.add("TRANSACTION",Token.KEYWORD1);
		keywords.add("TRIGGER",Token.KEYWORD1);
		keywords.add("TRUNCATE",Token.KEYWORD1);
		keywords.add("UNION",Token.KEYWORD1);
		keywords.add("UNIQUE",Token.KEYWORD1);
		keywords.add("UPDATE",Token.KEYWORD1);
		keywords.add("VALUES",Token.KEYWORD1);
		keywords.add("VARYING",Token.KEYWORD1);
		keywords.add("VIEW",Token.KEYWORD1);
		keywords.add("WHERE",Token.KEYWORD1);
		keywords.add("WHEN",Token.KEYWORD1);
		keywords.add("WITH",Token.KEYWORD1);
		keywords.add("WORK",Token.KEYWORD1);

		// Workbench specific keywords
		keywords.add("DESC",Token.KEYWORD2);
		keywords.add("DESCRIBE",Token.KEYWORD2);
		keywords.add("WBLIST",Token.KEYWORD2);
		keywords.add("WBLISTPROCS",Token.KEYWORD2);
		keywords.add("WBLISTDB",Token.KEYWORD2);
		keywords.add("WBLISTCAT",Token.KEYWORD2);
		keywords.add("ENABLEOUT",Token.KEYWORD2);
		keywords.add("DISABLEOUT",Token.KEYWORD2);
		keywords.add("WBEXPORT",Token.KEYWORD2);
		keywords.add("WBIMPORT",Token.KEYWORD2);
		keywords.add("WBFEEDBACK",Token.KEYWORD2);
		keywords.add("WBINCLUDE",Token.KEYWORD2);
		keywords.add("WBCOPY",Token.KEYWORD2);
		keywords.add(WbDefineVar.DEFINE_LONG.getVerb(),Token.KEYWORD2);
		keywords.add(WbDefineVar.DEFINE_SHORT.getVerb(),Token.KEYWORD2);
		keywords.add("WBVARLIST",Token.KEYWORD2);
		keywords.add("WBVARDELETE",Token.KEYWORD2);
		keywords.add("WBSTARTBATCH",Token.KEYWORD2);
		keywords.add("WBENDBATCH",Token.KEYWORD2);
		keywords.add("WBFEEDBACK",Token.KEYWORD2);
		keywords.add("WBREPORT",Token.KEYWORD2);
		keywords.add("WBSCHEMADIFF",Token.KEYWORD2);
		keywords.add("WBXSLT",Token.KEYWORD2);
		keywords.add(WbSelectBlob.VERB,Token.KEYWORD2);
		keywords.add(WbDefinePk.VERB,Token.KEYWORD2);
		keywords.add(WbListPkDef.VERB,Token.KEYWORD2);
		keywords.add(WbSavePkMapping.VERB,Token.KEYWORD2);
		keywords.add(WbLoadPkMapping.VERB,Token.KEYWORD2);
	}

	private void addDataTypes()
	{
		keywords.add("binary",Token.KEYWORD1);
		keywords.add("bit",Token.KEYWORD1);
		keywords.add("char",Token.KEYWORD1);
		keywords.add("character",Token.KEYWORD1);
		keywords.add("datetime",Token.KEYWORD1);
		keywords.add("date",Token.KEYWORD1);
		keywords.add("decimal",Token.KEYWORD1);
		keywords.add("float",Token.KEYWORD1);
		keywords.add("image",Token.KEYWORD1);
		keywords.add("int",Token.KEYWORD1);
		keywords.add("integer",Token.KEYWORD1);
		keywords.add("money",Token.KEYWORD1);
		//keywords.add("name",Token.KEYWORD1);
		keywords.add("number",Token.KEYWORD1);
		keywords.add("numeric",Token.KEYWORD1);
		keywords.add("nchar",Token.KEYWORD1);
		keywords.add("nvarchar",Token.KEYWORD1);
		keywords.add("ntext",Token.KEYWORD1);
		keywords.add("real",Token.KEYWORD1);
		keywords.add("smalldatetime",Token.KEYWORD1);
		keywords.add("smallint",Token.KEYWORD1);
		keywords.add("smallmoney",Token.KEYWORD1);
		keywords.add("text",Token.KEYWORD1);
		keywords.add("timestamp",Token.KEYWORD1);
		keywords.add("tinyint",Token.KEYWORD1);
		keywords.add("uniqueidentifier",Token.KEYWORD1);
		keywords.add("varbinary",Token.KEYWORD1);
		keywords.add("varchar",Token.KEYWORD1);
		keywords.add("nvarchar",Token.KEYWORD1);
		keywords.add("varchar2",Token.KEYWORD1);
		keywords.add("nvarchar2",Token.KEYWORD1);
		keywords.add("clob",Token.KEYWORD1);
		keywords.add("nclob",Token.KEYWORD1);
	}

	private void addSystemFunctions()
	{
		keywords.add("ABS",Token.KEYWORD3);
		keywords.add("ACOS",Token.KEYWORD3);
		keywords.add("ASIN",Token.KEYWORD3);
		keywords.add("ATAN",Token.KEYWORD3);
		keywords.add("ATN2",Token.KEYWORD3);
		keywords.add("CAST",Token.KEYWORD3);
		keywords.add("CEILING",Token.KEYWORD3);
		keywords.add("COS",Token.KEYWORD3);
		keywords.add("COT",Token.KEYWORD3);
		keywords.add("COUNT", Token.KEYWORD3);
		keywords.add("CURRENT_TIME",Token.KEYWORD3);
		keywords.add("CURRENT_DATE",Token.KEYWORD3);
		keywords.add("CURRENT_TIMESTAMP",Token.KEYWORD3);
		keywords.add("CURRENT_USER",Token.KEYWORD3);
		keywords.add("DATALENGTH",Token.KEYWORD3);
		keywords.add("DATEADD",Token.KEYWORD3);
		keywords.add("DATEDIFF",Token.KEYWORD3);
		keywords.add("DATENAME",Token.KEYWORD3);
		keywords.add("DATEPART",Token.KEYWORD3);
		keywords.add("DAY",Token.KEYWORD3);
		keywords.add("EXP",Token.KEYWORD3);
		keywords.add("FLOOR",Token.KEYWORD3);
		keywords.add("LOG",Token.KEYWORD3);
		keywords.add("MONTH",Token.KEYWORD3);
		keywords.add("RIGHT",Token.KEYWORD3);
		keywords.add("ROUND",Token.KEYWORD3);
		keywords.add("SIN",Token.KEYWORD3);
		keywords.add("SOUNDEX",Token.KEYWORD3);
		keywords.add("SPACE",Token.KEYWORD3);
		keywords.add("SQRT",Token.KEYWORD3);
		keywords.add("SQUARE",Token.KEYWORD3);
		keywords.add("TAN",Token.KEYWORD3);
		keywords.add("UPPER",Token.KEYWORD3);
		keywords.add("USER",Token.KEYWORD3);
		keywords.add("YEAR",Token.KEYWORD3);
	}

	private void addOperators()
	{
		keywords.add("ALL",Token.KEYWORD1);
		keywords.add("AND",Token.KEYWORD1);
		keywords.add("ANY",Token.KEYWORD1);
		keywords.add("BETWEEN",Token.KEYWORD1);
		keywords.add("CROSS",Token.KEYWORD1);
		keywords.add("EXISTS",Token.KEYWORD1);
		keywords.add("IN",Token.KEYWORD1);
		keywords.add("INTERSECT",Token.KEYWORD1);
		keywords.add("JOIN",Token.KEYWORD1);
		keywords.add("LIKE",Token.KEYWORD1);
		keywords.add("NOT",Token.KEYWORD1);
		keywords.add("NULL",Token.KEYWORD1);
		keywords.add("OR",Token.KEYWORD1);
		keywords.add("OUTER",Token.KEYWORD1);
		keywords.add("SOME",Token.KEYWORD1);
	}
}