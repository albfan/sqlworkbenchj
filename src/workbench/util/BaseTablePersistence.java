/*
 * AbstractTablePersistence.java
 *
 * Created on 1. November 2002, 00:15
 */

package workbench.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author  workbench@kellerer.org
 */
public class BaseTablePersistence
{
	protected Connection connection;
	protected PreparedStatement insert;
	protected PreparedStatement delete;
	protected PreparedStatement update;
	protected PreparedStatement select;

	protected ArrayList columns = new ArrayList();
	protected ArrayList pkColumns = new ArrayList();
	private String tablename;
	protected String valueObjectClass;

	protected BaseTablePersistence()
	{
	}

	protected BaseValueObject getPkValueObject()
	{
		BaseValueObject result =  this.createValueObject();
		for (int i=0; i < this.pkColumns.size(); i++)
		{
			result.addColumn((String)this.pkColumns.get(i));
		}
		return result;
	}

	protected BaseValueObject getValueObject()
	{
		BaseValueObject result = this.createValueObject();
		for (int i=0; i < columns.size(); i++)
		{
			result.addColumn((String)this.columns.get(i));
		}
		for (int i=0; i < this.pkColumns.size(); i++)
		{
			result.addColumn((String)this.pkColumns.get(i));
		}
		return result;
	}

	private BaseValueObject createValueObject()
	{
		BaseValueObject result =  null;
		if (this.valueObjectClass == null)
		{
			result = new BaseValueObject();
		}
		else
		{
			try
			{
				Class clz = Class.forName(this.valueObjectClass);
				result = (BaseValueObject)clz.newInstance();
			}
			catch (Exception cnf)
			{
				result = new BaseValueObject();
			}
		}
		return result;
	}

	public String getTablename() { return this.tablename; }

	protected void setTablename(String aTablename)
	{
		if (aTablename == null || aTablename.trim().length() == 0)
			throw new IllegalArgumentException("Tablename may not be null");
		this.tablename = aTablename;
	}

	public void setConnection(Connection aConn)
	{
		this.connection = aConn;
	}


	public int insertRow(BaseValueObject data)
		throws SQLException
	{
		if (this.insert == null)
		{
			StringBuffer sql = new StringBuffer(2000);
			sql.append("INSERT INTO ");
			sql.append(tablename);
			sql.append(" (");
			StringBuffer values = new StringBuffer(200);
			values.append(" VALUES (");
			int colCount = this.columns.size();
			for (int i=0; i < colCount; i++)
			{
				String col = (String)this.columns.get(i);
				if (i > 0)
				{
					sql.append(',');
					values.append(',');
				}
				sql.append(col);
				values.append('?');
			}
			values.append(')');
			sql.append(')');
			sql.append(values);
			this.insert = this.connection.prepareStatement(sql.toString());
		}

		int count = this.columns.size();
		for (int i=0; i < count; i++)
		{
			String col = (String)this.columns.get(i);
			Object value = data.getColumnValue(col);
			if  (value == null)
			{
				this.insert.setNull(i+1, Types.OTHER);
			}
			else
			{
				this.insert.setObject(i+1, value);
			}
		}
		int updateCount = this.insert.executeUpdate();
		return updateCount;
	}

	public int deleteRow(BaseValueObject data)
		throws SQLException
	{
		if (this.delete == null)
		{
			StringBuffer sql = new StringBuffer(2000);
			sql.append("DELETE FROM ");
			sql.append(tablename);
			sql.append(" WHERE ");
			int count = this.pkColumns.size();
			for (int i=0; i < count; i++)
			{
				if (i > 0)
				{
					sql.append(" AND ");
				}
				String col = (String)this.pkColumns.get(i);
				sql.append(col);
				sql.append(" = ?");
			}
			this.delete = this.connection.prepareStatement(sql.toString());
			System.out.println(sql);
		}
		int count = this.pkColumns.size();
		for (int i=0; i < count; i++)
		{
			String col = (String)this.pkColumns.get(i);
			Object value = data.getColumnValue(col);
			// we do not need to check for null values
			// because the PK columns may never be null
			this.delete.setObject(i+1, value);
		}
		int updateCount = this.delete.executeUpdate();
		return updateCount;
	}

	public int updateRow(BaseValueObject newValues, BaseValueObject oldValues)
		throws SQLException
	{
		if (this.update == null)
		{
			StringBuffer sql = new StringBuffer(2000);
			sql.append("UPDATE ");
			sql.append(tablename);
			sql.append(" SET ");

			for (int i=0; i < this.columns.size(); i++)
			{
				String col = (String)this.columns.get(i);

				if (i > 0) sql.append(", ");

				sql.append(col);
				sql.append(" = ?");
			}
			sql.append(" WHERE ");
			int count = this.pkColumns.size();
			for (int i=0; i < count; i++)
			{
				if (i > 0)
				{
					sql.append(" AND ");
				}
				String col = (String)this.pkColumns.get(i);
				sql.append(col);
				sql.append(" = ?");
			}
			this.update = this.connection.prepareStatement(sql.toString());
			System.out.println(sql);
		}
		int valuecount = this.columns.size();
		for (int i=0; i < valuecount; i++)
		{
			String col = (String)this.columns.get(i);
			Object value = newValues.getColumnValue(col);
			if (value == null)
			{
				this.update.setNull(i+1, Types.OTHER);
			}
			else
			{
				this.update.setObject(i+1, value);
			}
		}

		int pkcount = this.pkColumns.size();
		for (int i=0; i < pkcount; i++)
		{
			String col = (String)this.pkColumns.get(i);
			Object value = oldValues.getColumnValue(col);
			// we do not need to check for null values
			// because the PK columns may never be null
			this.update.setObject(i + 1 + valuecount, value);
		}

		int updateCount = this.update.executeUpdate();
		return updateCount;
	}

	public BaseValueObject selectRow(BaseValueObject data)
		throws SQLException
	{
		if (this.select == null)
		{
			StringBuffer sql = new StringBuffer(2000);
			sql.append("SELECT ");
			for (int i=0; i < this.columns.size(); i++)
			{
				String col = (String)this.columns.get(i);

				if (i > 0 ) sql.append(", ");

				sql.append(col);
			}
			sql.append(" FROM ");
			sql.append(this.tablename);
			sql.append(" WHERE ");
			int count = this.pkColumns.size();
			for (int i=0; i < count; i++)
			{
				if (i > 0)
				{
					sql.append(" AND ");
				}
				String col = (String)this.pkColumns.get(i);
				sql.append(col);
				sql.append(" = ?");
			}
			this.select = this.connection.prepareStatement(sql.toString());
			System.out.println(sql);
		}
		int pkcount = this.pkColumns.size();
		for (int i=0; i < pkcount; i++)
		{
			String col = (String)this.pkColumns.get(i);

			Object value = data.getColumnValue(col);
			if (value == null)
			{
				this.select.setNull(i+1, Types.OTHER);
			}
			else
			{
				this.select.setObject(i+1, value);
			}
		}

		ResultSet rs = this.select.executeQuery();
		BaseValueObject result = null;
		if (rs.next())
		{
			result = this.getValueObject();
			for (int i=0; i < this.columns.size(); i++)
			{
				String col = (String)this.columns.get(i);
				Object value = rs.getObject(col);
				result.setColumnValue(col, value);
			}
		}
		rs.close();
		return result;
	}

	protected void addColumn(String aColumn)
	{
		if (!this.columns.contains(aColumn))
		{
			this.columns.add(aColumn);
		}
	}

	protected void addPkColumn(String aColumn)
	{
		if (!this.pkColumns.contains(aColumn))
		{
			this.pkColumns.add(aColumn);
		}
	}


	private static Connection getConnection()
		throws SQLException, ClassNotFoundException
	{
		Connection con;
		Class.forName("org.hsqldb.jdbcDriver");
		//Class.forName("com.inet.tds.TdsDriver");
		//Class.forName("oracle.jdbc.OracleDriver");
		//Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
		//con = DriverManager.getConnection("jdbc:inetdae:demsqlvisa02:1433?database=visa_cpl_test", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:inetdae:reosqlpro08:1433?database=visa", "visa", "savivisa");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@DEMRDB34:1521:SBL1", "sadmin", "sadmin");
		//con = DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:oracle:oci8:@oradb", "auto", "auto");
		//con = DriverManager.getConnection("jdbc:odbc:Patsy");
		//con = DriverManager.getConnection("jdbc:hsqldb:d:\\daten\\db\\hsql\\test", "sa", null);
		con = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost", "sa", null);

		return con;
	}

	public static void main (String args[])
	{
		Connection con = null;
		try
		{
			con = getConnection();
			ArrayList pks = new ArrayList();
			pks.add("nr");
			BaseTablePersistence b = new BaseTablePersistence();
			b.setTablename("test");
			b.addPkColumn("nr");
			b.addColumn("nr");
			b.addColumn("name");
			b.setConnection(con);

			BaseValueObject newValue = b.getValueObject();
			newValue.setColumnValue("name", "third row");
			newValue.setColumnValue("nr", new Integer(3));

			BaseValueObject oldValue = b.getPkValueObject();
			oldValue.setColumnValue("nr", new Integer(1));

			int count = 0;
			//count = b.insertRow(newValue);
			//count = b.deleteRow(oldValue);
			//count = b.updateRow(newValue, oldValue);
			BaseValueObject data = b.selectRow(oldValue);
			System.out.println("data=" + data);
			con.commit();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { con.close(); } catch (Throwable th) {}
		}
	}


}
