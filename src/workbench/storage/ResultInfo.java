/*
 * ResultMetaData.java
 *
 * Created on August 26, 2004, 10:32 PM
 */

package workbench.storage;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import workbench.db.TableIdentifier;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;

/**
 * A class to cache the meta information of a ResultSet
 * @author  workbench@kellerer.org
 */
public class ResultInfo
{
	// Cached ResultSetMetaData information
	int[] columnTypes;
  int[] columnSizes;
	int[] columnPrecision;
	int[] columnScale;
	String[] columnNames;
	String[] columnClassNames;
	String[] columnTypeNames;
	int colCount;
	int realColumns;
	
	private TableIdentifier updateTable;
	
	public ResultInfo(String[] colNames, int[] colTypes, int[] colSizes)
	{
		this(colNames, colTypes, colSizes, null, null, null);
	}
	
	public ResultInfo(String[] colNames, int[] colTypes, int[] colSizes, int[] columnPrecision, int[] columnScale, String[] columnClasses)
	{
		//if (colNames.length != colTypes.length) throw new IllegalArgumentException("Size of arrays must match");
		//if (colNames.length != colSizes.length) throw new IllegalArgumentException("Size of arrays must match");
		//if (colTypes.length != colSizes.length) throw new IllegalArgumentException("Size of arrays must match");
		
		this.colCount = colNames.length;
		this.columnTypes = colTypes;
		if (colSizes == null)
		{
			this.columnSizes = new int[colCount];
		}
		else
		{
			this.columnSizes = colSizes;
		}
		this.columnNames = colNames;
		this.columnTypeNames = new String[this.columnTypes.length];
		if (columnClasses != null)
		{
			this.columnClassNames = columnClasses;
		}
		else
		{
			this.columnClassNames = new String[columnTypes.length];
			for (int i=0; i < this.columnTypes.length; i++)
			{
				this.columnTypeNames[i] = SqlUtil.getTypeName(this.columnTypes[i]);
				int precision = (columnPrecision != null ? columnPrecision[i] : 0);
				int scale = (columnScale != null ? columnScale[i] : 0);
				this.columnClassNames[i] = SqlUtil.getJavaClass(this.columnTypes[i], precision, scale);
			}
		}
	}

	/** Creates a new instance of ResultMetaData */
	public ResultInfo(ResultSetMetaData metaData) throws SQLException
	{
		this.colCount = metaData.getColumnCount();
		int col = 0;
		this.columnTypes = new int[this.colCount];
		this.columnTypeNames = new String[this.colCount];
		this.columnSizes = new int[this.colCount];
		this.columnNames = new String[this.colCount];
		this.columnClassNames = new String[this.colCount];
		this.columnPrecision = new int[this.colCount];
		this.columnScale = new int[this.colCount];

		for (int i=0; i < this.colCount; i++)
		{
			String name = metaData.getColumnName(i + 1);
			this.columnTypes[i] = metaData.getColumnType(i + 1);

			try
			{
				this.columnTypeNames[i] = metaData.getColumnTypeName(i + 1);
				if ("LONG".equals(this.columnTypeNames[i]))
				{
					this.columnTypes[i] = SqlUtil.LONG_TYPE;
				}
			}
			catch (Exception e)
			{
				this.columnTypeNames[i] = SqlUtil.getTypeName(this.columnTypes[i]);
				LogMgr.logWarning("ResultMetaData.<init>", "The JDBC driver does not support getColumnTypeName() " + e.getMessage());
			}

			try
			{
				this.columnSizes[i] = metaData.getColumnDisplaySize(i + 1);
			}
			catch (Exception e)
			{
				this.columnSizes[i] = 0;
				LogMgr.logWarning("ResultMetaData.<init>", "The JDBC driver does not support getColumnDisplaySize() " + e.getMessage());
			}


			if (name != null && name.trim().length() > 0)
			{
				this.realColumns ++;
				this.columnNames[i] = name;
			}
			else
			{
				this.columnNames[i] = "Col" + (i+1);
			}
			
			try
			{
				this.columnClassNames[i] = metaData.getColumnClassName(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("ResultInfo.<init>", "Error when retrieving class name for column " + i + " (" + e.getClass().getName() + ")");
				this.columnClassNames[i] = "java.lang.Object";
			}

			try
			{
				this.columnPrecision[i] = metaData.getPrecision(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("ResultInfo.<init>", "Error when retrieving precision for column " + i + " (" + e.getClass().getName() + ")");
				this.columnPrecision[i] = 0;
			}

			try
			{
				this.columnScale[i] = metaData.getScale(i + 1);
			}
			catch (Throwable e)
			{
				LogMgr.logWarning("ResultInfo.<init>", "Error when retrieving scale for column " + i + " (" + e.getClass().getName() + ")");
				this.columnScale[i] = 0;
			}
		}
	}

	public void setUpdateTable(TableIdentifier table)
	{
		this.updateTable = table;
	}
	
	public TableIdentifier getUpdateTable()
	{
		return this.updateTable;
	}
	
	public void setColumnSizes(int[] sizes)
	{
		if (sizes == null) return;
		if (sizes.length != this.colCount) return;
		this.columnSizes = sizes;
	}	
	
	public int getColumnType(int i) { return this.columnTypes[i]; }
	public String getColumnClassName(int i) 
	{ 
		if (this.columnClassNames != null) return this.columnClassNames[i]; 
		if (this.columnClassNames[i] != null) return this.columnClassNames[i];
		return this.getColumnClass(i).getName();
	}
	
	public String getColumnName(int i) { return this.columnNames[i]; }
	public String getDbmsTypeName(int i) { return this.columnTypeNames[i]; }
	public int getColumnCount() { return this.colCount; }
	
	public Class getColumnClass(int aColumn)
	{
		int type = this.getColumnType(aColumn);
		switch (type)
		{
			case Types.BIGINT:
			case Types.INTEGER:
				return BigInteger.class;
			case Types.SMALLINT:
				return Integer.class;
			case Types.NUMERIC:
			case Types.DECIMAL:
				return BigDecimal.class;
			case Types.DOUBLE:
				return Double.class;
			case Types.REAL:
			case Types.FLOAT:
				return Float.class;
			case Types.CHAR:
			case Types.VARCHAR:
				return String.class;
			case Types.DATE:
				return java.sql.Date.class;
			case Types.TIMESTAMP:
				return Timestamp.class;
			default:
				return Object.class;
		}
	}
	
}
