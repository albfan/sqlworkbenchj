/*
 * DmlStatement.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
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

import workbench.db.WbConnection;
import workbench.util.SqlUtil;
import workbench.util.StrBuffer;

/**
 *
 * @author  info@sql-workbench.net
 */
public class DmlStatement
{
	private String sql;
	private List values;
	private boolean usePrepared = true;
	
	private String chrFunc;
	private String concatString;
	private String concatFunction;
	/**
	 *	Create a new DmlStatement with the given SQL template string
	 *	that has no parameters.
	 */
	public DmlStatement(String aStatement)
		throws IllegalArgumentException
	{
		this(aStatement, null);
	}
	/**
	 *	Create a new DmlStatement with the given SQL template string
	 *	and the given values.
	 *
	 *	The SQL string is expected to contain a ? for each value
	 *	passed in aValueList. The SQL statement will be executed
	 *	using a prepared statement.
	 */
	public DmlStatement(String aStatement, List aValueList)
		//throws IllegalArgumentException
	{
		if (aStatement == null) throw new NullPointerException();

		/*
		String verb = SqlUtil.getSqlVerb(aStatement);
		if (! ("insert".equalsIgnoreCase(verb) ||
		       "update".equalsIgnoreCase(verb) ||
					 "delete".equalsIgnoreCase(verb)))
		{
			throw new IllegalArgumentException("Only UPDATE, DELETE, INSERT allowed");
		}
		*/
		
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

	public int execute(WbConnection aWbConn)
		throws SQLException
	{
		return this.execute(aWbConn.getSqlConnection());
	}

	/**
	 * Execute the statement.
	 * If setUsePreparedStatement(false) is called before
	 * calling execute(), the statement generated {@link #getExecutableStatement() }
	 * will be executed directly. Otherwise a prepared statement will be used.
	 * @param the Connection to be used
	 * @return the number of rows affected
	 */
	public int execute(Connection aConnection)
		throws SQLException
	{
		int rows;
		rows = this.executePrepared(aConnection);

		return rows;
	}

	private int executePrepared(Connection aConnection)
		throws SQLException
	{
		PreparedStatement stmt = aConnection.prepareStatement(this.sql);
		for (int i=0; i < this.values.size(); i++)
		{
			Object value = this.values.get(i);
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
	
	public String getExecutableStatement()
	{
		return this.getExecutableStatement((String)null);
	}
	
	public String getExecutableStatement(Connection aConn)
	{
		String dbproduct = null;
		if (aConn != null) 
		{
			try
			{
				dbproduct = aConn.getMetaData().getDatabaseProductName();
			}
			catch (Exception e)
			{
				dbproduct = null;
			}
		}
		return this.getExecutableStatement(dbproduct);	
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
		if (this.values.size() > 0)
		{
			DbDateFormatter dateFormat = SqlSyntaxFormatter.getDateLiteralFormatter(dbproduct);
			
			StrBuffer result = new StrBuffer(this.sql.length() + this.values.size() * 10);
			boolean inQuotes = false;
			int parmIndex = 0;
			for (int i = 0; i < this.sql.length(); ++i)
			{
				char c = sql.charAt(i);

				if (c == '\'') inQuotes = !inQuotes;
				if (c == '?' && !inQuotes && parmIndex < this.values.size())
				{
					Object v = this.values.get(parmIndex);
					String literal = SqlSyntaxFormatter.getDefaultLiteral(v, dateFormat);
					if (this.chrFunc != null && v instanceof String)
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
		boolean useFunc = (this.concatFunction != null);
		
		if (!useFunc && this.concatString == null) this.concatString = "||";
		StrBuffer result = new StrBuffer();
		boolean funcAppended = false;
		boolean quotePending = false;
		
		int len = aValue.length();
		for (int i=0; i < len; i++)
		{
			char c = aValue.charAt(i);
			if (c < 32)
			{
				if (useFunc)
				{
					if (!funcAppended)
					{
						StrBuffer temp = new StrBuffer(concatFunction);
						temp.append('(');
						temp.append(result);
						result = temp;
						funcAppended = true;
					}
					if (quotePending)
					{
						result.append(",\'");
					}
					result.append('\'');
					result.append(',');
					result.append(this.chrFunc);
					result.append('(');
					result.append(Integer.toString((int)c));
					result.append(')');
					quotePending = true;
				}
				else
				{
					result.append('\'');
					result.append(this.concatString);
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
					if (useFunc) result.append(',');
					result.append('\'');
				}
				result.append(c);
				quotePending = false;
			}
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

	public static void main(String args[])
	{
		try
		{
			char c = '\t';
			System.out.println(Integer.toString((int)c));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
