/*
 * DataCopier.java
 *
 * Created on December 20, 2003, 10:37 AM
 */

package workbench.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import workbench.db.importer.DataImporter;
import workbench.db.importer.RowDataProducer;
import workbench.db.importer.RowDataReceiver;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DataCopier
	implements RowDataProducer
{
	private RowDataReceiver receiver;
	
	private WbConnection sourceConnection;
	private WbConnection targetConnection;
	
	private TableIdentifier sourceTable;
	private TableIdentifier targetTable;
	
	// the columnMap will contain elements of type ColumnIdentifier
	private HashMap columnMap;

	private Statement retrieveStatement;
	private String retrieveSql;
	private boolean keepRunning = true;
	
	/** Creates a new instance of DataCopier */
	public DataCopier()
	{
	}

	public void setDefinition(WbConnection source, WbConnection target, TableIdentifier aSourceTable)
		throws SQLException
	{
		this.setDefinition(source, target, aSourceTable, aSourceTable, null);
	}
	
	public void setDefinition(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable)
		throws SQLException
	{
		this.setDefinition(source, target, aSourceTable, aTargetTable, null);
	}
	
	/**
	 *	Define the source table, the target table and the column mapping 
	 *	for the copy process.
	 *	If the columnMapping is null, the matching columns from both tables are used.
	 *	It is expected that the mapping contains String objects. The key is the name of the
	 *	source column, the mapped value is the name of the target column
	 *	Before calling this method, the source and target connections have to be defined!
	 */
	public void setDefinition(WbConnection source, WbConnection target, TableIdentifier aSourceTable, TableIdentifier aTargetTable, Map columnMapping)
		throws SQLException
	{
		this.sourceConnection = source;
		this.targetConnection = target;
		this.sourceTable = aSourceTable;
		this.targetTable = aTargetTable;
		this.initColumnMapping(columnMapping);
		this.initReceiver();		
	}

	public void start() throws Exception
	{
		ResultSet rs = null;
		try
		{
			this.retrieveStatement = this.sourceConnection.createStatement();
			rs = this.retrieveStatement.executeQuery(this.retrieveSql);
			int colCount = this.columnMap.size();
			Object[] rowData = new Object[colCount];
			while (this.keepRunning && rs.next())
			{
				for (int i=0; i < colCount; i++)
				{
					rowData[i] = rs.getObject(i + 1);
				}
				this.receiver.processRow(rowData);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DataCopier.copy()", "Error when copying data", e);
		}
		finally
		{
			try { rs.close(); } catch (Throwable th) {}
			try { this.retrieveStatement.close(); } catch (Throwable th) {}
		}
	}
	
	public void setReceiver(RowDataReceiver receiver)
	{
		this.receiver = receiver;
		this.initReceiver();
	}
	
	public void cancel()
	{
		this.keepRunning = false;
	}
	
	/*
	 *	Initialize the internal column mapping. 
	 *  This is done by reading the columns of the source and target table.
	 *  The columns which have the same name, will be copied
	 */
	private void readColumnDefinition()
		throws SQLException
	{
		if (!this.hasDefinition()) return;
		List sourceCols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		List targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);
		
		int count = targetCols.size();
		this.columnMap = new HashMap(count);
		LogMgr.logInfo("DataCopier.readColumnDefinition()", "Copying matching columns from " + this.sourceTable + " to " + this.targetTable);

		for (int i=0; i < count; i++)
		{
			ColumnIdentifier column = (ColumnIdentifier)targetCols.get(i);
			// ColumnIdentifier's equals() method checks the name and the data type!
			// so only columns where the name and the data type match, will be copied
			// note that this can be overridden by explicitly defining the column mapping
			if (sourceCols.contains(column))
			{
				LogMgr.logInfo("DataCopier.readColumnDefinition()", "Including column: " + column);
				this.columnMap.put(column, column);
			}
		}
	}

	/**
	 *	Send the definition of the target table to the DataImporter, and creates
	 *	the approriate SELECT statement to retrieve the data from the source
	 */
	private void initReceiver()
	{
		if (this.receiver == null) return;
		if (this.columnMap == null || this.columnMap.size() == 0) return;
		int count = this.columnMap.size();
		String[] cols = new String[count];
		int[] types =  new int[count];
		int col = 0;
		Iterator itr = this.columnMap.entrySet().iterator();
		
		StringBuffer sql = new StringBuffer(200);
		sql.append("SELECT ");
		
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			ColumnIdentifier sid = (ColumnIdentifier)entry.getKey();
			ColumnIdentifier tid = (ColumnIdentifier)entry.getValue();
			if (col > 0)
			{
				sql.append("\n       , ");
			}
			sql.append(sid.getColumnName());
			cols[col] = tid.getColumnName();
			types[col] = tid.getDataType();
			col ++;
		}
		sql.append(" \nFROM ");
		sql.append(this.sourceTable.getTableExpression());
		LogMgr.logDebug("DataCopier.initImporter()", "Using Statement\n" + sql.toString());
		this.retrieveSql = sql.toString();
		this.receiver.setTargetTable(this.targetTable.getTableExpression(), cols, types);
	}
	
	/**
	 *	Initialize the column mapping between source and target table.
	 *	If a mapping is provided, it is used (after checking that the columns
	 *	exist in both tables).
	 *	If no mapping is provided, all matching columns from both tables are copied
	 */
	private void initColumnMapping(Map aMapping)
		throws SQLException
	{
		if (!this.hasDefinition()) return;
		
		// if no mapping is specified, read the matching columns from
		// the source and the target table
		if (aMapping == null || aMapping.size() == 0) 
		{
			this.readColumnDefinition();
			return;
		}
		
		List sourceCols = this.sourceConnection.getMetadata().getTableColumns(this.sourceTable);
		List targetCols = this.targetConnection.getMetadata().getTableColumns(this.targetTable);
		
		Iterator itr = aMapping.entrySet().iterator();
		this.columnMap = new HashMap(targetCols.size());
		
		while (itr.hasNext())
		{
			Map.Entry entry = (Map.Entry)itr.next();
			String sc = (String)entry.getKey();
			String tc = (String)entry.getValue();
			if (sc == null || sc.trim().length() == 0 || tc == null || tc.trim().length() == 0) continue;
			
			// we are creating the Identifier without a type, so that when 
			// comparing the ID's the type will not be considered
			ColumnIdentifier sid = new ColumnIdentifier(sc);
			ColumnIdentifier tid = new ColumnIdentifier(tc);

			// now check if the columns are actually present in the specified tables
			int sidx = sourceCols.indexOf(sid);
			int tidx = targetCols.indexOf(tid);

			if (sidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + sc + " not found in table " + this.sourceTable + ". Ignoring mapping!");
			}
			if (tidx < 0)
			{
				LogMgr.logWarning("DataCopier.initColumnMapping()", "Column " + tc + " not found in table " + this.targetTable + ". Ignoring mapping!");
			}
			
			if (sidx > -1 && tidx > -1)
			{
				LogMgr.logInfo("DataCopier.initColumnMapping()", "Copying " + this.sourceTable + "." + sc + " to " + this.targetTable + "." + tc);
				this.columnMap.put(sourceCols.get(sidx), targetCols.get(tidx));
			}
			
		}
	}
	
	private boolean hasDefinition()
	{
		return (this.sourceConnection != null &&
						this.targetConnection != null &&
						this.sourceTable != null &&
		        this.targetTable != null);
	}
}

