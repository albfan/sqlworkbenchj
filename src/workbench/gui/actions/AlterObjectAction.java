/*
 * AlterObjectAction
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.actions;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.DbObjectChanger;
import workbench.db.DbSettings;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;
import workbench.gui.dbobjects.RunScriptPanel;
import workbench.interfaces.Reloadable;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class AlterObjectAction
	extends WbAction
	implements TableModelListener
{

	private WbTable tableList;
	private WbConnection dbConnection;
	private Reloadable client;

	public AlterObjectAction(WbTable tables)
	{
		tableList = tables;
		initMenuDefinition("MnuTxtAlterObjects");
		tableList.addTableModelListener(this);
		checkEnabled();
	}

	public void setClient(Reloadable reload)
	{
		client = reload;
	}
	
	public void setConnection(WbConnection con)
	{
		dbConnection = con;
		checkEnabled();
	}
	
	private void checkEnabled()
	{
		DataStore ds = (tableList != null ? tableList.getDataStore() : null);
		boolean modified = (ds != null ? ds.isModified() : false);
		setEnabled(dbConnection != null && modified && canRenameChangedTypes());
	}

	private boolean canRenameChangedTypes()
	{
		if (dbConnection == null) return false;
		DbSettings db = dbConnection.getDbSettings();
		if (db == null) return false;

		DataStore ds = (tableList != null ? tableList.getDataStore() : null);
		if (ds == null) return false;
		
		List<String> types = CollectionUtil.arrayList();
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			if (ds.isRowModified(row))
			{
				String type = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
				types.add(type);
			}
		}

		for (String type : types)
		{
			if (db.getRenameObjectSql(type) != null) return true;
		}
		return false;
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		Window w = SwingUtilities.getWindowAncestor(tableList);
		Frame parent = null;
		if (w instanceof Frame)
		{
			parent = (Frame) w;
		}
		String alterScript = getScript();
		if (alterScript == null)
		{
			String msg = ResourceMgr.getString("MsgNoAlterAvailable");
			WbSwingUtilities.showErrorMessage(parent, msg);
		}

		RunScriptPanel panel = new RunScriptPanel(dbConnection, alterScript);
		panel.openWindow(parent, ResourceMgr.getString("TxtAlterTable"));

		if (panel.isSuccess())
		{
			if (client != null)
			{
				client.reload();
			}
		}
	}

	private String getScript()
	{
		DataStore ds = tableList.getDataStore();
		DbObjectChanger renamer = new DbObjectChanger(dbConnection);

		Map<DbObject, DbObject> changed = new HashMap<DbObject, DbObject>();

		for (int row = 0; row < ds.getRowCount(); row++)
		{
			DbObject oldObject = getOldDefintion(row);
			DbObject newObject = getCurrentDefinition(row);
			changed.put(oldObject, newObject);
		}

		return renamer.getAlterScript(changed);
	}

	private TableIdentifier getCurrentDefinition(int row)
	{
		if (tableList == null)
		{
			return null;
		}
		DataStore ds = tableList.getDataStore();

		String name = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		String catalog = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		String type = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		String comment = ds.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS);
		TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
		tbl.setType(type);
		tbl.setNeverAdjustCase(true);
		tbl.setComment(comment);
		return tbl;
	}

	private TableIdentifier getOldDefintion(int row)
	{
		if (tableList == null)
		{
			return null;
		}
		DataStore ds = tableList.getDataStore();

		String name = (String) ds.getOriginalValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME);
		String schema = (String) ds.getOriginalValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA);
		String catalog = (String) ds.getOriginalValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG);
		String type = (String) ds.getOriginalValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
		String comment = (String) ds.getOriginalValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS);
		TableIdentifier tbl = new TableIdentifier(catalog, schema, name);
		tbl.setType(type);
		tbl.setNeverAdjustCase(true);
		tbl.setComment(comment);
		return tbl;
	}

	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (e.getType() == TableModelEvent.UPDATE)
		{
			checkEnabled();
		}
	}
}
