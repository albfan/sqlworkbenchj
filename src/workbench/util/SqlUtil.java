package workbench.util;

import java.lang.Character;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.WbManager;
import workbench.db.DbDateFormatter;
import workbench.storage.DataStore;
import workbench.storage.NullValue;

public class SqlUtil
{
	private static Pattern specialCharPattern = Pattern.compile("[$ ]");		

	/** Creates a new instance of SqlUtil */
	private SqlUtil()
	{
	}

	public static String quoteObjectname(String aColname)
	{
		if (aColname == null) return null;
		Matcher m = specialCharPattern.matcher(aColname);
		boolean b = m.find();
		if (!b) return aColname.trim();
		StringBuffer col = new StringBuffer(aColname.length() + 5);
		col.append('"');
		col.append(aColname.trim());
		col.append('"');
		return col.toString();
	}
	
	public static String getSqlVerb(String aStatement)
	{
		StringTokenizer tok = new StringTokenizer(aStatement.trim());
		return tok.nextToken(" \t");
	}

	public static List getCommands(String aScript, String aDelimiter)
	{
		if (aScript == null || aScript.trim().length() == 0) return Collections.EMPTY_LIST;
		ArrayList result = new ArrayList();
		int count, pos, scriptLen, cmdNr, lastPos, delimitLen;
		boolean quoteOn;
		ArrayList emptyList, commands;
		String value, ls_OldDelimit, delimit;
		int oldPos;
		String currChar;

		aScript = aScript.trim();
		// Handle MS SQL GO's
		if (aScript.indexOf("\nGO\n") > -1)
		{
			aScript = StringUtil.replace(aScript, "\nGO\n", ";\n");
		}
		else 
		{
			aScript = StringUtil.replace(aScript, StringUtil.LINE_TERMINATOR + "GO" + StringUtil.LINE_TERMINATOR, ";" + StringUtil.LINE_TERMINATOR);
		}
		
		if (aScript.endsWith("GO"))
		{
			aScript = aScript.substring(0, aScript.length() - 2) + ";";
		}
		pos = aScript.indexOf(aDelimiter);
		if (pos == -1 || pos == aScript.length() - 1)
		{
			result.add(aScript);
			return result;
		}
		quoteOn = false;
		cmdNr = 0;
		scriptLen = aScript.length();
		delimit = aDelimiter.trim().toUpperCase();
		delimitLen = delimit.length();
		lastPos = 0;

		for (pos = 0; pos < scriptLen; pos++)
		{

			currChar = aScript.substring(pos, pos + 1);
			if (currChar.equals("\'") || currChar.equals("\""))
			{
				quoteOn = !quoteOn;
			}

			if (!quoteOn)
			{
				if (delimitLen > 1 && pos + delimitLen < scriptLen)
				{
					currChar = aScript.substring(pos, pos + delimitLen).toUpperCase();
				}

				if ((currChar.equals(delimit) || (pos == scriptLen)))
				{
					if (pos == scriptLen)
					{
						value = aScript.substring(lastPos, pos).trim();
						if (value.substring(value.length() - 1, value.length()).equals(";"))
						{
							value = value.substring(0, value.length() - 2);
						}
					}
					else
					{
						value = aScript.substring(lastPos, pos).trim();
					}
					if (value.length() > 0)
					{
						if (value.endsWith(aDelimiter))
						{
							value = value.substring(0, value.length() - delimitLen);
						}
						
						result.add(value);
					}
					lastPos = pos + delimitLen;
				}
			}

		}
		if (lastPos < pos)
		{
			value = aScript.substring(lastPos).trim();
			if (value.endsWith(aDelimiter))
			{
				value = value.substring(0, value.length() - delimitLen);
			}
			if (makeCleanSql(value, false).length() > 0) 
				result.add(value);
		}
		return result;
	}

	/**
	 * Return the list of tables which are in the FROM list of the given SQL statement.
	 */
	public static List getTables(String aSql)
	{
		boolean inQotes = false;
		boolean fromFound = false;
		String orgSql = makeCleanSql(aSql, false);
		aSql = orgSql.toUpperCase();
		
		final String FROM = " FROM ";
		int fromPos = aSql.indexOf(FROM);
		if (fromPos == -1) return Collections.EMPTY_LIST;
		
		int quotePos = aSql.indexOf('\'');
		int pos;
		if (quotePos != -1 && quotePos < fromPos)
		{
			while (!fromFound)
			{
				pos = skipQuotes(aSql, quotePos + 1);
				fromPos = aSql.indexOf(FROM, pos);
				if (fromPos == -1) break;
				quotePos = aSql.indexOf('\'', pos);
				fromFound = (quotePos == -1 || (quotePos > fromPos));
			}
		}
		if (fromPos == -1) return Collections.EMPTY_LIST;
		int fromEnd = aSql.indexOf(" WHERE ", fromPos);
		if (fromEnd == -1) fromEnd = aSql.indexOf(" ORDER ", fromPos);
		if (fromEnd == -1) fromEnd = aSql.indexOf(" GROUP ", fromPos);
		if (fromEnd == -1) fromEnd = aSql.length();
		String fromList = orgSql.substring(fromPos + FROM.length(), fromEnd);
		StringTokenizer tok = new StringTokenizer(fromList, ",");
		ArrayList result = new ArrayList();
		while (tok.hasMoreTokens())
		{
			String table = tok.nextToken().trim();
			pos = table.indexOf(' ');
			if (pos != -1)
			{
				table = table.substring(0, pos);
			}
			result.add(table);
		}
			
		return result;
	}

	public static String makeCleanSql(String aSql, boolean keepNewlines)
	{
		return makeCleanSql(aSql, keepNewlines, '\'');
	}
	/**
	 *	Replaces all white space characters with ' ' (But not inside
	 *	string literals), removes -- style and Java style comments
	 *	@param String - The sql script to "clean out"
	 *  @param boolean - if true, newline characters (\n) are kept
	 *	@returns String
	 */
	public static String makeCleanSql(String aSql, boolean keepNewlines, char quote)
	{
		aSql = aSql.trim();
		int count = aSql.length();
		if (count == 0) return aSql;
		boolean inComment = false;
		boolean inQuotes = false;
		
		StringBuffer newSql = new StringBuffer(count);
		
		// remove trailing semicolon
		if (aSql.charAt(count - 1) == ';') count --;
		
		for (int i=0; i < count; i++)
		{
			char c = aSql.charAt(i);
			inQuotes = c == quote;
			
			if (!inComment)
			{
				if ( c == '/' && i < count - 1 && aSql.charAt(i+1) == '*' & !inQuotes)
				{
					inComment = true;
					i++;
				}
				else if (c == '-' && i < count - 1 && aSql.charAt(i+1) == '-' && !inQuotes)
				{
					// ignore rest of line for -- style comments
					while (c != '\n' && i < count - 1) 
					{
						i++;
						c = aSql.charAt(i);
					}
				}
				else
				{						
					if (c == '\n' && !keepNewlines)
					{
						newSql.append(' ');
					}
					else if (c < 32 || (c > 126 && c < 145) || c == 255)
					{
						newSql.append(' ');
					}
					else
					{
						newSql.append(c);
					}
				}
			}
			else
			{
				if ( c == '*' && i < count - 1 && aSql.charAt(i+1) == '/')
				{
					inComment = false;
					i++;
				}
			}
		}
		return newSql.toString();
	}
	
	private static final int skipQuotes(String aString, int aStartpos)
	{
		char c = aString.charAt(aStartpos);
		while (c != '\'')
		{
			aStartpos ++;
			c = aString.charAt(aStartpos);
		}
		return aStartpos + 1;
	}

	public static final String getJavaPrimitive(String aClass)
	{
		if (aClass == null) return null;
		int pos = aClass.lastIndexOf('.');
		if (pos >= 0)
		{
			aClass = aClass.substring(pos + 1);
		}
		if (aClass.equals("Integer"))
		{
			return "int";
		}
		else if (aClass.equals("Long"))
		{
			return "long";
		}
		else if (aClass.equals("Boolean"))
		{
			return "boolean";
		}
		else if (aClass.equals("Character"))
		{
			return "char";
		}
		else if (aClass.equals("Float"))
		{
			return "float";
		}
		else if (aClass.equals("Double"))
		{
			return "double";
		}
		return null;
	}
	
	public static final String getJavaClass(int aSqlType, int aSize, int aPrecision)
	{
		if (aSqlType == Types.BIGINT)
			return "java.math.BigInteger";
		else if (aSqlType == Types.BOOLEAN)
			return "Boolean";
		else if (aSqlType == Types.CHAR)
			return "Character";
		else if (aSqlType == Types.DATE)
			return "java.util.Date";
		else if (aSqlType == Types.DECIMAL)
			return getDecimalClass(aSqlType, aSize, aPrecision);
		else if (aSqlType == Types.DOUBLE)
			return getDecimalClass(aSqlType, aSize, aPrecision);
		else if (aSqlType == Types.FLOAT)
			return getDecimalClass(aSqlType, aSize, aPrecision);
		else if (aSqlType == Types.INTEGER)
			return "Integer";
		else if (aSqlType == Types.JAVA_OBJECT)
			return "Object";
		else if (aSqlType == Types.NUMERIC)
			return getDecimalClass(aSqlType, aSize, aPrecision);
		else if (aSqlType == Types.REAL)
			return getDecimalClass(aSqlType, aSize, aPrecision);
		else if (aSqlType == Types.SMALLINT)
			return "Integer";
		else if (aSqlType == Types.TIME)
			return "java.sql.Time";
		else if (aSqlType == Types.TIMESTAMP)
			return "java.sql.Timestamp";
		else if (aSqlType == Types.TINYINT)
			return "Integer";
		else if (aSqlType == Types.VARCHAR)
			return "String";
		else 
			return null;
	}

	private static final String getDecimalClass(int aSqlType, int aSize, int aPrecision)
	{
		if (aPrecision == 0)
		{
			if (aSize < 11)
			{
				return "java.lang.Integer";
			}
			if (aSize >= 11 && aSize < 18)
			{
				return "java.lang.Long";
			}
			else 
			{
				return "java.math.BigInteger";
			}
		}
		else
		{
			if (aSize < 11)
			{
				return "java.lang.Float";
			}
			if (aSize >= 11 && aSize < 18)
			{
				return "java.lang.Double";
			}
			else 
			{
				return "java.math.BigDecimal";
			}
		}
	}
	
	private static String getDoubleClass(int aSqlType, int aSize, int aPrecision)
	{
		return getDecimalClass(aSqlType, aSize, aPrecision);
	}
	private static String getFloatClass(int aSqlType, int aSize, int aPrecision)
	{
		return getDecimalClass(aSqlType, aSize, aPrecision);
	}
	private static String getNumericClass(int aSqlType, int aSize, int aPrecision)
	{
		return getDecimalClass(aSqlType, aSize, aPrecision);
	}

	public static final boolean isNumberType(int aSqlType)
	{
		return (aSqlType == Types.BIGINT ||
				    aSqlType == Types.INTEGER ||
						aSqlType == Types.DECIMAL ||
						aSqlType == Types.DOUBLE ||
					  aSqlType == Types.FLOAT ||
					  aSqlType == Types.NUMERIC ||
					  aSqlType == Types.REAL ||
					  aSqlType == Types.SMALLINT ||
					  aSqlType == Types.TINYINT);
	}
	public static final boolean isDateType(int aSqlType)
	{
		return (aSqlType == Types.DATE ||
						aSqlType == Types.TIMESTAMP);
	}

	public static final String getTypeName(int aSqlType)
	{
		if (aSqlType == Types.ARRAY)
			return "ARRAY";
		else if (aSqlType == Types.BIGINT)
			return "BIGINT";
		else if (aSqlType == Types.BINARY)
			return "BINARY";
		else if (aSqlType == Types.BIT)
			return "BIT";
		else if (aSqlType == Types.BLOB)
			return "BLOB";
		else if (aSqlType == Types.BOOLEAN)
			return "BOOLEAN";
		else if (aSqlType == Types.CHAR)
			return "CHAR";
		else if (aSqlType == Types.CLOB)
			return "CLOB";
		else if (aSqlType == Types.DATALINK)
			return "DATALINK";
		else if (aSqlType == Types.DATE)
			return "DATE";
		else if (aSqlType == Types.DECIMAL)
			return "DECIMAL";
		else if (aSqlType == Types.DISTINCT)
			return "DISTINCT";
		else if (aSqlType == Types.DOUBLE)
			return "DOUBLE";
		else if (aSqlType == Types.FLOAT)
			return "FLOAT";
		else if (aSqlType == Types.INTEGER)
			return "INTEGER";
		else if (aSqlType == Types.JAVA_OBJECT)
			return "JAVA_OBJECT";
		else if (aSqlType == Types.LONGVARBINARY)
			return "LONGVARBINARY";
		else if (aSqlType == Types.LONGVARCHAR)
			return "LONGVARCHAR";
		else if (aSqlType == Types.NULL)
			return "NULL";
		else if (aSqlType == Types.NUMERIC)
			return "NUMERIC";
		else if (aSqlType == Types.OTHER)
			return "OTHER";
		else if (aSqlType == Types.REAL)
			return "REAL";
		else if (aSqlType == Types.REF)
			return "REF";
		else if (aSqlType == Types.SMALLINT)
			return "SMALLINT";
		else if (aSqlType == Types.STRUCT)
			return "STRUCT";
		else if (aSqlType == Types.TIME)
			return "TIME";
		else if (aSqlType == Types.TIMESTAMP)
			return "TIMESTAMP";
		else if (aSqlType == Types.TINYINT)
			return "TINYINT";
		else if (aSqlType == Types.VARBINARY)
			return "VARBINARY";
		else if (aSqlType == Types.VARCHAR)
			return "VARCHAR";
		else 
			return "UNKNOWN";
	}

	public static void main(String args[])
	{
		//String script = "select 'testing'';''', test.spalte1 from test;\r\n                ;\r\nupdate test set blba='xx';";
		/*
		String script = "select 'testing'';''', test.spalte1 from test;";
		List commands = getCommands(script, ";");
		for (int i=0; i < commands.size(); i++)
		{
			System.out.println(commands.get(i).toString());
			System.out.println("-----");
		}
		String sql = "select bp.productid, from visa_bidproduct bp ,visa_config c ,visa_bid b where c.bidid = bp.bidid and   c.configid = bp.configid and  bp.bidid = b.bidid and b.bidref = 'VGB0042304-02'";
		List tables = getTables(sql);
		for (int i=0; i < tables.size(); i++)
		{
			System.out.println("table=" + tables.get(i));
		}
		*/
		String col = "test col";
		Pattern p = Pattern.compile("[$ ]");		
		Matcher m;
		m = p.matcher(col);
		System.out.println("find()=" + m.find());
		System.out.println(quoteObjectname(col));
	}

}
