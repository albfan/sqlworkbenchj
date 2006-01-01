/*
 * DmlStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import workbench.db.ColumnIdentifier;

import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 * A class to execute a SQL Statement and to create the statement
 * from a given list of values.
 * @author  support@sql-workbench.net
 */
public class DmlStatement
{
	private String sql;
	private List values;
	private boolean usePrepared = true;
	private SqlLiteralFormatter literalFormatter;
	private String chrFunc;
	private String concatString;
	private String concatFunction;
	
	/**
	 *	Create a new DmlStatement with the given SQL template string
	 *	and the given values.
	 *
	 *	The SQL string is expected to contain a ? for each value
	 *	passed in aValueList. The SQL statement will be executed
	 *	using a prepared statement.
	 */
	public DmlStatement(String aStatement, List aValueList, SqlLiteralFormatter formatter)
	{
		if (aStatement == null) throw new NullPointerException();
		this.setLiteralFormatter(formatter);
		int count = this.countParameters(aStatement);
		if (count > 0 && aValueList != null && count != aValueList.size())
		{
			throw new IllegalArgumentException("Number of parameter tokens does not match number of parameters passed.");
		}

		this.sql = aStatement;

		if (aValueList == null)
		{
			this.values = Collections.EMPTY_LIST;
		}
		else
		{
			this.values = aValueList;
		}
	}

	/**
	 * Execute the statement as a prepared statement
	 * @param aConnection the Connection to be used
	 * @return the number of rows affected
	 */
	public int execute(WbConnection aConnection)
		throws SQLException
	{
		PreparedStatement stmt = aConnection.getSqlConnection().prepareStatement(this.sql);
		for (int i=0; i < this.values.size(); i++)
		{
			ColumnData data = (ColumnData)this.values.get(i);
			Object value = data.getValue();
			if (value instanceof NullValue)
			{
				NullValue nv = (NullValue)value;
				stmt.setNull(i+1, nv.getType());
			}
			else if (value instanceof OracleLongType)
			{
				OracleLongType longValue = (OracleLongType)value;
				Reader in = new StringReader(longValue.getValue());
				stmt.setCharacterStream(1, in, longValue.getLength());
			}
			else
			{
				stmt.setObject(i + 1, value);
			}
		}
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows;
	}

	/**
	 *	Returns true if a prepared statement is used
	 *	to send the data to the database.
	 */
	public boolean getUsePreparedStatement()
	{
		return this.usePrepared;
	}
	
	public void setConcatString(String concat)
	{
		if (concat == null) return;
		this.concatString = concat;
		this.concatFunction = null;
	}
	
	public void setConcatFunction(String func)
	{
		if (func == null) return;
		this.concatFunction = func;
		this.concatString = null;
	}
	
	public void setChrFunction(String aFunc)
	{
		this.chrFunc = aFunc;
	}
	
	/**
	 *	Returns a "real" SQL Statement which can be executed
	 *	directly. The statement contains the parameter values
	 *	as literals. No placeholders are used.
	 *	This statement is executed after setUsePreparedStatement(false) is called
	 */
	public String getExecutableStatement(String dbproduct)
	{
		if (this.literalFormatter == null) this.literalFormatter = new SqlLiteralFormatter();
		
		if (this.values.size() > 0)
		{
			DbDateFormatter dateFormat = this.literalFormatter.getDateLiteralFormatter();
			
			StrBuffer result = new StrBuffer(this.sql.length() + this.values.size() * 10);
			boolean inQuotes = false;
			int parmIndex = 0;
			for (int i = 0; i < this.sql.length(); ++i)
			{
				char c = sql.charAt(i);

				if (c == '\'') inQuotes = !inQuotes;
				if (c == '?' && !inQuotes && parmIndex < this.values.size())
				{
					ColumnData data = (ColumnData)this.values.get(parmIndex);
					String literal = this.literalFormatter.getDefaultLiteral(data, dateFormat);
					if (this.chrFunc != null && SqlUtil.isCharacterType(data.getIdentifier().getDataType()))
					{
						literal = this.createInsertString(literal);
					}
					result.append(literal);
					parmIndex ++;
				}
				else
				{
					result.append(c);
				}
			}
			return result.toString();
		}
		else
		{
			return this.sql;
		}
	}

	private String createInsertString(String aValue)
	{
		if (aValue == null) return null;
		if (this.chrFunc == null) return aValue;
		boolean useConcatFunc = (this.concatFunction != null);
		
		if (!useConcatFunc && this.concatString == null) this.concatString = "||";
		StrBuffer result = new StrBuffer();
		boolean funcAppended = false;
		boolean quotePending = false;
		
		char last = 0;
		
		int len = aValue.length();
		for (int i=0; i < len; i++)
		{
			char c = aValue.charAt(i);
			if (c < 32)
			{
				if (useConcatFunc)
				{
					if (!funcAppended)
					{
						StrBuffer temp = new StrBuffer(concatFunction);
						temp.append('(');
						temp.append(result);
						result = temp;
						funcAppended = true;
					}
					if (quotePending && last >= 32)
					{
						result.append(",\'");
					}
					if (last >= 32) result.append('\'');
					result.append(',');
					result.append(this.chrFunc);
					result.append('(');
					result.append(Integer.toString((int)c));
					result.append(')');
					quotePending = true;
				}
				else
				{
					if (last >= 32) 
					{
						result.append('\'');
						result.append(this.concatString);
					}
					result.append(this.chrFunc);
					result.append('(');
					result.append(Integer.toString((int)c));
					result.append(')');
					result.append(this.concatString);
					quotePending = true;
				}
			}
			else
			{
				if (quotePending)
				{
					if (useConcatFunc) result.append(',');
					result.append('\'');
				}
				result.append(c);
				quotePending = false;
			}
			last = c;
		}
		if (funcAppended)
		{
			result.append(')');
		}
		return result.toString();
	}
	
	private int countParameters(String aSql)
	{
		if (aSql == null) return -1;
		boolean inQuotes = false;
		int count = 0;
		for (int i = 0; i < aSql.length(); i++)
		{
			char c = aSql.charAt(i);

			if (c == '\'') inQuotes = !inQuotes;
			if (c == '?' && !inQuotes)
			{
				count ++;
			}
		}
		return count;
	}

	public String toString()
	{
		return sql;
	}

	public void setLiteralFormatter(SqlLiteralFormatter f)
	{
		this.literalFormatter = f;
	}
	
}
