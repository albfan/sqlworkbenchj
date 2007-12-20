/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.interfaces.ObjectDropper;
import workbench.log.LogMgr;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class ColumnDropper
	implements ObjectDropper
{
	private WbConnection conn;
	private List<ColumnIdentifier> columns;
	private TableIdentifier table;
	private boolean cancelDrop = false;
	private Statement currentStatement;
	
	public ColumnDropper()
	{
	}
	
	public ColumnDropper(WbConnection db, TableIdentifier tbl, List<ColumnIdentifier> toDrop)
	{
		this.conn = db;
		this.columns = toDrop;
		this.table = tbl;
	}

	public boolean supportsCascade()
	{
		return false;
	}

	public void setCascade(boolean flag)
	{
	}
	
	public void cancel()
		throws SQLException
	{
		cancelDrop = true;
		if (this.currentStatement != null)
		{
			this.currentStatement.cancel();
		}
	}

	public void setConnection(WbConnection con)
	{
		this.conn = con;
	}
	
	public void setObjectTable(TableIdentifier tbl)
	{
		this.table = tbl;
	}
	
	public void setObjects(List<DbObject> toDrop)
	{
		this.columns = new ArrayList<ColumnIdentifier>();
		if (toDrop == null) return;
		for (DbObject dbo : toDrop)
		{
			if (dbo instanceof ColumnIdentifier)
			{
				columns.add((ColumnIdentifier)dbo);
			}
		}
	}
	
	public void dropObjects()
		throws SQLException
	{
		if (this.conn == null) return;
		if (this.table == null) return;
		if (this.columns == null || this.columns.size() == 0) return;
		
		List<String> statements = getSql();
		
		try
		{
			this.currentStatement = this.conn.createStatement();
			
			for (String sql : statements)
			{
				if (cancelDrop) break;
				LogMgr.logDebug("ColumnDropper.dropObjects()", "Using sql: " + sql);
				this.currentStatement.executeUpdate(sql);
			}
			
			if (conn.shouldCommitDDL())
			{
				if (cancelDrop)
				{
					conn.rollback();
				}
				else
				{
					conn.commit();
				}
			}
		}
		catch (SQLException e)
		{
			if (conn.shouldCommitDDL())
			{
				conn.rollback();
			}
			throw e;
		}
		finally
		{
			SqlUtil.closeStatement(currentStatement);
			currentStatement = null;
		}
	}

	private List<String> getSql()
	{
		String multiSql = conn.getDbSettings().getDropMultipleColumnSql();
		String singleSql = conn.getDbSettings().getDropSingleColumnSql();
		List<String> result = new ArrayList<String>(columns.size());

		if (this.columns.size() == 1 || StringUtil.isEmptyString(multiSql))
		{
			singleSql = StringUtil.replace(singleSql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(conn));
			
			for (ColumnIdentifier col : columns)
			{
				result.add(StringUtil.replace(singleSql, MetaDataSqlManager.COLUMN_NAME_PLACEHOLDER, col.getColumnName(this.conn)));
			}
		}
		else
		{
			multiSql = StringUtil.replace(multiSql, MetaDataSqlManager.TABLE_NAME_PLACEHOLDER, table.getTableExpression(conn));
			
			StringBuilder cols = new StringBuilder(columns.size());
			int nr = 0;
			for (ColumnIdentifier col : columns)
			{
				if (nr > 0) cols.append(", ");
				cols.append(col.getColumnName(this.conn));
				nr ++;
			}
			result.add(StringUtil.replace(multiSql, MetaDataSqlManager.COLUMN_LIST_PLACEHOLDER, cols.toString()));
		}
		
		return result;
	}
}
