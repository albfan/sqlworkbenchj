/*
 * DbMetadata.java
 *
 * Created on 16. Juli 2002, 13:09
 */

package workbench.db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import workbench.exception.WbException;
import workbench.gui.components.ResultSetTableModel;
import workbench.storage.DataStore;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class DbMetadata
{
	private DatabaseMetaData metaData;
	private List tableListColumns;
	private WbConnection dbConnection;
	//private List tableDefinitionColumns;
	//private List procListColumns;
	
	/** Creates a new instance of DbMetadata */
	public DbMetadata(WbConnection aConnection)
		throws SQLException
	{
		Connection c = aConnection.getSqlConnection();
		this.dbConnection = aConnection;
		this.metaData = c.getMetaData();
		String[] cols = {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE"};
		this.tableListColumns = Collections.unmodifiableList(Arrays.asList(cols));
		//cols = new String[] {"COLUMN_NAME", "TYPE_NAME","COLUMN_SIZE","COLUMN_DEF","IS_NULLABLE"};
		//this.tableDefinitionColumns = Collections.unmodifiableList(Arrays.asList(cols));
		//cols = new String[] {"PROCEDURE_CAT", "PROCEDURE_NAME", "PROCEDURE_TYPE"};
		//this.procListColumns = Collections.unmodifiableList(Arrays.asList(cols));
	}

	public List getTableListColumns() { return this.tableListColumns; }
	//public List getTableDefinitionColumns() { return this.tableDefinitionColumns; }
	//public List getProcListColumns() { return this.procListColumns; }
	
	public ResultSet getTableDefinition(String aTablename)
		throws SQLException
	{
		ResultSet result = this.metaData.getColumns(null, null, this.adjustTablename(aTablename), "%");
		return result;
	}

	public String adjustTablename(String aTable)
	{
		try
		{
			if (this.metaData.storesUpperCaseIdentifiers())
			{
				return aTable.toUpperCase();
			}
			else if (this.metaData.storesLowerCaseIdentifiers())
			{
				return aTable.toLowerCase();
			}
		}
		catch (Exception e)
		{
		}
		return aTable;
	}
	
	public ResultSet getTables()
		throws SQLException
	{
		ResultSet result = this.metaData.getTables(null, null, null, null);
		return result;
	}

	public boolean storesUpperCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesUpperCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}
	
	public ResultSetTableModel getListOfProcedures()
		throws SQLException
	{
		return new ResultSetTableModel(this.getProcedures());
	}
	
	public DataStore getProcedures()
		throws SQLException
	{
		String catalog = this.dbConnection.getSqlConnection().getCatalog();
		ResultSet rs = this.metaData.getProcedures(catalog, null, "%");
		String[] cols = new String[] {"CATALOG", "PROCEDURE_NAME", "PROCEDURE_TYPE"};
		final int types[] = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] = {50,50,50};
		
		DataStore ds = new DataStore(cols, types);
		ds.setColumnSizes(sizes);
		
		while (rs.next())
		{
			String cat = rs.getString("PROCEDURE_CAT");
			String name = rs.getString("PROCEDURE_NAME");
			String type = rs.getString("PROCEDURE_TYPE");
			int row = ds.addRow();
			
			if (cat == null)
				ds.setNull(row, 0);
			else
				ds.setValue(row, 0, cat);
			
			ds.setValue(row, 1, name);
			if (type == null)
				ds.setNull(row, 2);
			else
				ds.setValue(row, 2, type);
		}
		rs.close();
		return ds;
	}
	
	public boolean storesLowerCaseIdentifiers()
	{
		try
		{
			return this.metaData.storesLowerCaseIdentifiers();
		}
		catch (SQLException e)
		{
			return false;
		}
	}
	
	public ResultSetTableModel getListOfTables()
		throws SQLException, WbException
	{
		return this.getListOfTables(this.getTables());
	}
	
	public ResultSetTableModel getListOfTables(ResultSet rs)
		throws SQLException, WbException
	{
		DataStore ds = new DataStore(rs, this.tableListColumns);
		int[] sizes = new int[this.tableListColumns.size()];
		sizes[0] = 8;
		sizes[1] = 8;
		sizes[2] = 30;
		sizes[3] = 30;
		ds.setColumnSizes(sizes);
		return new ResultSetTableModel(ds);
	}

	public ResultSetTableModel getTableDefinitionModel(String aCatalog, String aSchema, String aTable)
		throws SQLException, WbException
	{
		return new ResultSetTableModel(this.getTableDefinitionInformation(aCatalog, aSchema, aTable));
	}
	public ResultSetTableModel getTableDefinitionModel(String aTable)
		throws SQLException, WbException
	{
		return new ResultSetTableModel(this.getTableDefinitionInformation(null, null, aTable));
	}
	
	public DataStore getTableDefinitionInformation(String aTable)
		throws SQLException, WbException
	{
		return this.getTableDefinitionInformation(null, null, aTable);
	}
	
	public DataStore getTableDefinitionInformation(String aCatalog, String aSchema, String aTable)
		throws SQLException, WbException
	{
		final String cols[] = {"COLUMN_NAME", "TYPE_NAME", "PK", "NULLABLE", "DEFAULT", "REMARKS"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {20, 18, 5, 8, 10, 50};
		DataStore ds = new DataStore(cols, types);
		ds.setColumnSizes(sizes);
		ResultSet keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, this.adjustTablename(aTable));
		ArrayList keys = new ArrayList();
		while (keysRs.next())
		{
			keys.add(keysRs.getString("COLUMN_NAME").toLowerCase());
		}
		keysRs.close();
		
		ResultSet rs = this.metaData.getColumns(aCatalog, aSchema, this.adjustTablename(aTable), "%");
		while (rs.next())
		{
			int row = ds.addRow();
			String colName = rs.getString("COLUMN_NAME");
			ds.setValue(row, 0, colName);
			int sqlType = rs.getInt("DATA_TYPE");
			String typeName = rs.getString("TYPE_NAME");
			int size = rs.getInt("COLUMN_SIZE");
			int digits = rs.getInt("DECIMAL_DIGITS");
			String rem = rs.getString("REMARKS");
			String def = rs.getString("COLUMN_DEF");
			String nul = rs.getString("IS_NULLABLE");
			
			switch (sqlType)
			{
				case Types.VARCHAR:
				case Types.CHAR:
					typeName = typeName + "(" + size + ")";
					break;
				case Types.DECIMAL:
				case Types.DOUBLE:
				case Types.NUMERIC:
				case Types.FLOAT:
					if (typeName.indexOf('(') == -1)
					{
						typeName = typeName + "(" + size + "," + digits + ")";
					}
					break;
			}
			ds.setValue(row, 1, typeName);
			if (keys.contains(colName.toLowerCase()))
				ds.setValue(row, 2, "YES");
			else
				ds.setValue(row, 2, "NO");
			
			ds.setValue(row, 3, nul);

			if (def != null) ds.setValue(row, 4, def);
			else ds.setNull(row, 4);
			
			if (rem != null) ds.setValue(row, 5, rem);
			else ds.setNull(row, 5);
		}
		rs.close();
		return ds;
	}
	
	public ResultSetTableModel getTableIndexes(String aTable)
		throws SQLException
	{
		return new ResultSetTableModel(this.getTableIndexInformation(null, null, aTable));
	}
	
	public ResultSetTableModel getTableIndexes(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		return new ResultSetTableModel(this.getTableIndexInformation(aCatalog, aSchema, aTable));
	}
	
	public DataStore getTableIndexInformation(String aCatalog, String aSchema, String aTable)
		throws SQLException
	{
		String[] cols = {"INDEX_NAME", "UNIQUE", "PK", "DEFINITION"};
		final int types[] =   {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR};
		final int sizes[] =   {40, 10, 10, 100};

		ResultSet keysRs = this.metaData.getPrimaryKeys(aCatalog, aSchema, this.adjustTablename(aTable));
		String pkName = null;
		while (keysRs.next())
		{
			pkName = keysRs.getString("PK_NAME");
		}
		keysRs.close();
		HashMap idxInfo = new HashMap();
		DataStore idxData = new DataStore(cols, types);
		ResultSet idxRs = this.metaData.getIndexInfo(aCatalog, aSchema, this.adjustTablename(aTable), false, true);
		while (idxRs.next())
		{
			boolean unique = idxRs.getBoolean("NON_UNIQUE");
			String indexName = idxRs.getString("INDEX_NAME");
			if (idxRs.wasNull()) continue;
			if (indexName == null) continue;
			String colName = idxRs.getString("COLUMN_NAME");
			String dir = idxRs.getString("ASC_OR_DESC");
			ArrayList colInfo = (ArrayList)idxInfo.get(indexName);
			if (colInfo == null)
			{
				colInfo = new ArrayList(10);
				idxInfo.put(indexName, colInfo);
				if (unique)
					colInfo.add("YES");
				else
					colInfo.add("NO");
			}
			if (dir != null)
				colInfo.add(colName + " " + dir);
			else
				colInfo.add(colName);
		}
		idxRs.close();
		Iterator itr = idxInfo.entrySet().iterator();
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			ArrayList colist = (ArrayList)entry.getValue();
			String index = (String)entry.getKey();
			int row = idxData.addRow();
			if (colist != null && colist.size() > 1)
			{
				idxData.setValue(row, 0, index);
					
				String unique = (String)colist.get(0);
				idxData.setValue(row, 1, unique);
				StringBuffer def = new StringBuffer(100);
				for (int i=1; i < colist.size(); i++)
				{
					if (i > 1) def.append(", ");
					def.append((String)colist.get(i));
				}
				if (pkName != null && pkName.equalsIgnoreCase(index)) 
				{
					idxData.setValue(row, 2, "YES");
				}
				else
				{
					idxData.setValue(row, 2, "NO");
				}
				idxData.setValue(row, 3, def.toString());
			}
		}
		return idxData; 
	}
	
}
