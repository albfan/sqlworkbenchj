package workbench.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;

/**
 *  Base class for handling the database access for a single table.
 *	Although this class can be used directly it should be subclassed to allow
 *	for customized factory methods of the ValueObject
 */
public class BaseTablePersistence
{
	protected Connection connection;
	protected String insertSql;
	protected String deleteSql;
	protected String updateSql;
	protected String selectSql;
	
	protected ArrayList columns = new ArrayList();
	protected ArrayList pkColumns = new ArrayList();
	
	private String tablename;
	
	/**
	 *	Contains the class name for the concrete ValueObject 
	 *	which is used for any descdentant of this class.
	 */
	protected String valueObjectClass;

	protected BaseTablePersistence()
	{
	}

	/**
	 *	Returns a configured BaseValueObject for this table.
	 *	The value object will contain only the primary key columns 
	 *	which were defined for this table.
	 *	
	 *	If the variable valueObjectClass is defined, an instance of that
	 *	class will be created. This functionality is used by the generated 
	 *	descendants of this class.
	 *
	 *	@return	BaseValueObject with all columns for this table
	 *
	 *	@see #getValueObject()
	 *	@see #createValueObject()
	 */
	protected BaseValueObject getPkValueObject()
	{
		BaseValueObject result =  this.createValueObject();
		for (int i=0; i < this.pkColumns.size(); i++)
		{
			result.addColumn((String)this.pkColumns.get(i));
		}
		return result;
	}

	/**
	 *	Returns a configured BaseValueObject for this table
	 *	The value object will contain all columns which were defined for
	 *	this table.
	 *	
	 *	If the variable valueObjectClass is defined, an instance of that
	 *	class will be created. This functionality is used by the generated 
	 *	descendants of this class
	 *
	 *	@return	BaseValueObject with all columns for this table
	 *
	 *	@see #getPkValueObject()
	 *	@see #createValueObject()
	 */
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

	/**
	 *	Factory method for creating a ValueObject.
	 *	If valueObjectClass is defined an instance of 
	 *	that class is created (if that fails, an instance
	 *	of BaseValueObject will be created instead)
	 *
	 *	@return an empty BaseValueObject
	 *
	 *	@see #getPkValueObject()
	 *	@see #getValueObject()
	 */
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

	/**	
	 *	Returns the tablename for this persistence class.
	 *
	 *	@return the name of the database table
	 */
	public String getTablename() { return this.tablename; }

	/**
	 *	Define the table for which this class is responsible.
	 *	In an environment where catalog or schema are required
	 *	it is assumed that the passed tablename already contains
	 *	that information (e.g. myschema.mytable)
	 *
	 *	@param aTablename the tableanme to be set
	 */
	protected void setTablename(String aTablename)
	{
		if (aTablename == null || aTablename.trim().length() == 0)
			throw new IllegalArgumentException("Tablename may not be null");
		this.tablename = aTablename;
	}

	/**
	 *	Define the connection on which this persistence class operates.
	 *
	 *	@param aConn the database connection to be used.
	 */
	public void setConnection(Connection aConn)
	{
		this.connection = aConn;
	}

	/**
	 *	Inserts a new row into the database based on the data
	 *	provided in the given value object.
	 *	It is assumed that the list of columns inside the given 
	 *	ValueObject is always the same. This is guaranteed if 
	 *	the factory method for creating the value object is used.
	 *	
	 *	@param data the column data to be inserted
	 *
	 *	@see #getPkValueObject()
	 *	@see #getValueObject()
	 */
	public int insertRow(BaseValueObject data)
		throws SQLException
	{
		if (data == null) throw new IllegalArgumentException("The ValueObject might not be null");
		
		if (this.insertSql == null || data.getColumnCount() != this.columns.size())
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
			this.insertSql = sql.toString();
		}
		PreparedStatement stmt = this.connection.prepareStatement(this.insertSql);		
		int count = this.columns.size();
		for (int i=0; i < count; i++)
		{
			String col = (String)this.columns.get(i);
			Object value = data.getColumnValue(col);
			if  (value == null)
			{
				stmt.setNull(i+1, Types.OTHER);
			}
			else
			{
				stmt.setObject(i+1, value);
			}
		}
		int updateCount = stmt.executeUpdate();
		stmt.close();
		return updateCount;
	}

	/**
	 *	Deletes a row from the database based on the data
	 *	provided in the given value object.
	 *	
	 *	Only the values for columns defined as primary key columns
	 *	for this table are used from the ValueObject. If the ValueObject
	 *	contains more columns they are ignored.
	 *	
	 *	@param data the ValueObject with the primary key information
	 *
	 *	@see #getPkValueObject()
	 */
	public int deleteRow(BaseValueObject data)
		throws SQLException
	{
		if (data == null) throw new IllegalArgumentException("The ValueObject might not be null");
		
		// re-initialize the sql if the given value object has a different
		// number of columns then we expect
		if (this.deleteSql == null || 
			  (data.getColumnCount() != this.columns.size() &&
				 data.getColumnCount() != this.pkColumns.size()))
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
			this.deleteSql = sql.toString(); 
			System.out.println(sql);
		}
		PreparedStatement stmt = this.connection.prepareStatement(this.deleteSql);
		int count = this.pkColumns.size();
		for (int i=0; i < count; i++)
		{
			String col = (String)this.pkColumns.get(i);
			Object value = data.getColumnValue(col);
			// we do not need to check for null values
			// because the PK columns may never be null
			stmt.setObject(i+1, value);
		}
		int updateCount = stmt.executeUpdate();
		stmt.close();
		return updateCount;
	}

	/**
	 *	Update a record in the database with the given values in <code>newValues</code>.
	 *	All defined columns for this table will be included in the update statement.
	 *	
	 *	The <code>pkValues</code> ValueObject has to contain the primary key values
	 *	which identify the record to be updated. 
	 *
	 *	@param newValues		the new values for the record
	 *	@param pkValues			the primary key values to identify the record
	 *
	 *	@see #getPkValueObject()
	 *	@see #getValueObject()
	 */
	public int updateRow(BaseValueObject newValues, BaseValueObject pkValues)
		throws SQLException
	{
		if (newValues == null) throw new IllegalArgumentException("The new ValueObject might not be null");
		if (pkValues == null) throw new IllegalArgumentException("The PK ValueObject might not be null");
		
		if (this.updateSql == null || 
			  newValues.getColumnCount() != this.columns.size() ||
				pkValues.getColumnCount() != this.pkColumns.size())
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
			this.updateSql = sql.toString();
			System.out.println(sql);
		}
		PreparedStatement stmt = this.connection.prepareStatement(this.updateSql);
		int valuecount = this.columns.size();
		for (int i=0; i < valuecount; i++)
		{
			String col = (String)this.columns.get(i);
			Object value = newValues.getColumnValue(col);
			if (value == null)
			{
				stmt.setNull(i+1, Types.OTHER);
			}
			else
			{
				stmt.setObject(i+1, value);
			}
		}

		int pkcount = this.pkColumns.size();
		for (int i=0; i < pkcount; i++)
		{
			String col = (String)this.pkColumns.get(i);
			Object value = pkValues.getColumnValue(col);
			// we do not need to check for null values
			// because the PK columns may never be null
			stmt.setObject(i + 1 + valuecount, value);
		}

		int updateCount = stmt.executeUpdate();
		stmt.close();
		return updateCount;
	}

	/**
	 *	Returns a row from the database identified by its primary key.	
	 *	Returns a ValueObject which contains all columns from the defined
	 *	table by retrieving a row identified by the primary key values
	 *	defined in the given ValueObject.
	 *
	 *	Only the primary key columns of the given ValueObject are used.
	 *	Any other column contained in the ValueObject is ignored.
	 *
	 *	@param data the value object with the primary key information.
	 *
	 *	@see #getPkValueObject()
	 */
	 public BaseValueObject selectRow(BaseValueObject data)
		throws SQLException
	{
		if (data == null) throw new IllegalArgumentException("The ValueObject might not be null");
		
		// re-initialize the sql if the given value object has a different number of columns then we expect
		if (this.selectSql == null || 
			  (data.getColumnCount() != this.columns.size() &&
				 data.getColumnCount() != this.pkColumns.size()))
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
			this.selectSql = sql.toString();
			System.out.println(sql);
		}
		PreparedStatement stmt = this.connection.prepareStatement(this.selectSql);
		int pkcount = this.pkColumns.size();
		for (int i=0; i < pkcount; i++)
		{
			String col = (String)this.pkColumns.get(i);

			Object value = data.getColumnValue(col);
			if (value == null)
			{
				stmt.setNull(i+1, Types.OTHER);
			}
			else
			{
				stmt.setObject(i+1, value);
			}
		}

		ResultSet rs = stmt.executeQuery();
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
		stmt.close();
		return result;
	}

	/**
	 *	Add a column to the list of columns for this table persistence.
	 *	
	 *	@param aColumn the name of the column
	 */
	protected void addColumn(String aColumn)
	{
		if (!this.columns.contains(aColumn))
		{
			this.columns.add(aColumn);
		}
	}

	/**
	 *	Add a column to the list of primary key columns for this table.
	 *	
	 *	@param aColumn the name of the primary key column
	 */
	protected void addPkColumn(String aColumn)
	{
		if (!this.pkColumns.contains(aColumn))
		{
			this.pkColumns.add(aColumn);
		}
	}

}
