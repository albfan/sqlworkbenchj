/*
 * Created on 24. Juli 2002, 21:55
 */
package workbench.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import workbench.db.WbConnection;
import workbench.exception.InvalidStatementException;
import workbench.util.SqlUtil;

/**
 *
 * @author  sql.workbench@freenet.de
 */
class DmlStatement
{
	private String sql;
	private List values;
	private boolean usePrepared = true;

	/**
	 *	Create a new DmlStatement with the given SQL template string 
	 *	that has no parameters.
	 */
	public DmlStatement(String aStatement)
		throws InvalidStatementException
	{
		this(aStatement, null, true);
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
		throws InvalidStatementException
	{
		this(aStatement, aValueList, true);
	}
	
	public DmlStatement(String aStatement, List aValueList, boolean aFlag)
		throws InvalidStatementException
	{
		if (aStatement == null) throw new NullPointerException();
		
		String verb = SqlUtil.getSqlVerb(aStatement);
		if (! ("insert".equalsIgnoreCase(verb) ||
		       "update".equalsIgnoreCase(verb) ||
					 "delete".equalsIgnoreCase(verb)))
		{
			throw new InvalidStatementException("Only UPDATE, DELETE, INSERT allowed");
		}
		
		int count = this.countParameters(aStatement);
		if (count > 0 && aValueList != null && count != aValueList.size())
		{
			throw new InvalidStatementException("Number of parameter tokens does not match number of parameters passed.");
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
		this.setUsePreparedStatement(aFlag);
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
		
		if (this.usePrepared)
		{
			rows = this.executePrepared(aConnection);
		}
		else
		{
			rows = this.executeDirect(aConnection);
		}
		return rows;
	}

	private int executePrepared(Connection aConnection)
		throws SQLException
	{
		PreparedStatement stmt = aConnection.prepareStatement(this.sql);
		for (int i=0; i < this.values.size(); i++)
		{
			stmt.setObject(i + 1, this.values.get(i));
		}
		int rows = stmt.executeUpdate();
		stmt.close();
		return rows;
	}
	
	/** Execute the DML statement without a prepared statement.
	 * The statement created by {@link #getExecutableStatement() }
	 * will be executed
	 * @param aConnection
	 * @throws SQLException
	 * @return number of rows affected
	 */
	private int executeDirect(Connection aConnection)
		throws SQLException
	{
		Statement stmt = aConnection.createStatement();
		int rows = stmt.executeUpdate(this.getExecutableStatement());
		stmt.close();
		return rows;
	}
	
	/**
	 *	Set the usage of prepared statements.
	 */
	public void setUsePreparedStatement(boolean aFlag)
	{
		//this.usePrepared = aFlag;
		this.usePrepared = true;
	}
	
	/**
	 *	Returns true if a prepared statement is used
	 *	to send the data to the database.
	 */
	public boolean getUsePreparedStatement()
	{
		return this.usePrepared;
	}
	
	/**
	 *	Returns a "real" SQL Statement which can be executed 
	 *	directly. The statement contains the parameter values
	 *	as literals. No placeholders are used. 
	 *	This statement is executed after setUsePreparedStatement(false) is called
	 */
	public String getExecutableStatement()
	{
		if (this.values.size() > 0)
		{
			StringBuffer result = new StringBuffer(this.sql.length() + this.values.size() * 10);
			boolean inQuotes = false;
			int parmIndex = 0;
			for (int i = 0; i < this.sql.length(); ++i)
			{
				char c = sql.charAt(i);

				if (c == '\'') inQuotes = !inQuotes;
				if (c == '?' && !inQuotes && parmIndex < this.values.size())
				{
					result.append(SqlUtil.getLiteral(this.values.get(parmIndex)));
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
		return this.getExecutableStatement();
	}
	
	public static void main(String args[])
	{
		try
		{
			Class.forName("oracle.jdbc.OracleDriver");
			Connection con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "test", "test");
			try
			{
				String sql = "insert into test (nr, name, datum) values (?,'test?',?)";
				ArrayList values = new ArrayList();
				values.add(new Integer(4));
				values.add(new java.sql.Date(new java.util.Date().getTime()));
				DmlStatement dml = new DmlStatement(sql, values);
				//int rows = dml.execute(con);
				System.out.println(dml.getExecutableStatement());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			con.commit();
			con.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
