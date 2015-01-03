/*
 * ColumnOrderMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;

/**
 * A class to save and restore the ordering of columns in a JTable
 *
 * @author Thomas Kellerer
 */
public class ColumnOrderMgr
{
	private Map<String, List<String>> columnOrders;

	protected static class LazyInstanceHolder
	{
		protected static final ColumnOrderMgr instance = new ColumnOrderMgr();
	}

	public static ColumnOrderMgr getInstance()
	{
		return LazyInstanceHolder.instance;
	}

	private ColumnOrderMgr()
	{
		 columnOrders = new HashMap<String, List<String>>();
		 load();
	}

	public int getSize()
	{
		return columnOrders.size();
	}

	/**
	 * Restores a saved column order to the given JTable.
	 * <br>
	 * This table should be called on the EDT to make sure the
	 * new column model is initialized correctly for the table.
	 *
	 * @param table
	 */
	public synchronized void restoreColumnOrder(WbTable table)
	{
		String key = getColumnOrderKey(table);
		if (key == null) return;
		List<String> cols = columnOrders.get(key);
		if (cols != null)
		{
			applyColumnOrder(table, cols, true);
		}
	}

	private String getColumnOrderKey(WbTable table)
	{
		if (table == null) return null;
		DataStore ds = table.getDataStore();
		if (ds == null) return null;

		// The JDBC URL is part of the key, so without a connection
		// the column order cannot be stored.
		WbConnection con = ds.getOriginalConnection();
		if (con == null) return null;

		// As this is currently only used in the DbExplorer
		// getUpdateTable() could potentially return null
		// because setUpdateTableToBeUsed() is used in
		// TableDataPanel, therefor if no update table is
		// found, we should get one after calling checkUpdateTable()
		TableIdentifier tbl = ds.getUpdateTable();
		if (tbl == null) ds.checkUpdateTable();
		tbl = ds.getUpdateTable();
		if (tbl == null) return null;

		String url = con.getUrl();
		return getColumnOrderKey(tbl, url);
	}

	public String getColumnOrderKey(TableIdentifier tbl, String url)
	{
		String key = tbl.getTableExpression() + "@" + url;
		return key;
	}


	/**
	 * Check if the column order for the given key is already stored.
	 *
	 * @param tbl the table to check
	 * @return true, if the column order has been saved for the given table
	 */
	public synchronized boolean isOrderSaved(WbTable tbl)
	{
		String key = getColumnOrderKey(tbl);
		return columnOrders.containsKey(key);
	}

	public synchronized boolean isOrderSaved(TableIdentifier tbl, String url)
	{
		String key = getColumnOrderKey(tbl, url);
		return columnOrders.containsKey(key);
	}

	public void resetColumnOrder(WbTable table)
	{
		String key = getColumnOrderKey(table);
		removeColumnOrder(key);

		if (table == null) return;

		TableColumnModel current = table.getColumnModel();
		if (current == null) return;
		if (current.getColumnCount() == 0) return;

		Comparator<TableColumn> comp = new Comparator<TableColumn>()
		{
			@Override
			public int compare(TableColumn o1, TableColumn o2)
			{
				return o1.getModelIndex() - o2.getModelIndex();
			}
		};
		List<TableColumn> old = new ArrayList<TableColumn>(current.getColumnCount());
		for (int i=0; i < current.getColumnCount(); i++)
		{
			old.add(current.getColumn(i));
		}
		Collections.sort(old, comp);
		TableColumnModel model = new DefaultTableColumnModel();
		for (TableColumn col : old)
		{
			model.addColumn(col);
		}
		table.setColumnModel(model);
	}

	public void applyColumnOrder(WbTable table, List<String> newOrder, boolean addMissing)
	{
		if (newOrder == null || newOrder.isEmpty()) return;

		TableColumnModel model = new DefaultTableColumnModel();
		TableColumnModel current = table.getColumnModel();

		for (CharSequence colname : newOrder)
		{
			try
			{
				int c = current.getColumnIndex(colname);
				TableColumn currentCol = current.getColumn(c);
				model.addColumn(currentCol);
			}
			catch (IllegalArgumentException e)
			{
				// getColumnIndex() throws an IllegalArgumentException rather than returning -1
				// so we can safely ignore this here.
				LogMgr.logDebug("ColumnOrderMgr.applyColumnOrder()", "Column '" + colname + "' not found.");
			}
		}

		if (addMissing)
		{
			// Now check if all columns in the table are present in the saved set
			// The missing columns will simply be added to the end of the column model
			for (int i=0; i < current.getColumnCount(); i++)
			{
				TableColumn col = current.getColumn(i);
				String name = col.getIdentifier().toString();
				if (!newOrder.contains(name))
				{
					newOrder.add(name);
					model.addColumn(col);
				}
			}
		}

		table.setColumnModel(model);
	}

	public synchronized void removeColumnOrder(String key)
	{
		if (key == null) return;
		columnOrders.remove(key);
	}

	public List<String> getColumnOrder(WbTable table)
	{
		TableColumnModel current = table.getColumnModel();
		List<String> cols = new ArrayList<String>(current.getColumnCount());
		for (int i=0; i < current.getColumnCount(); i++)
		{
			String name = current.getColumn(i).getIdentifier().toString();
			cols.add(name);
		}
		return cols;
	}

	public synchronized void storeColumnOrder(WbTable table)
	{
		if (table == null) return;
		String key = getColumnOrderKey(table);
		columnOrders.put(key, getColumnOrder(table));
	}

	public synchronized void saveSettings()
		throws IOException
	{
		File f = Settings.getInstance().getColumnOrderStorage();
		if (columnOrders.isEmpty() && f.exists())
		{
			f.delete();
		}
		else if (columnOrders.size() > 0)
		{
			FileOutputStream out = new FileOutputStream(f);
			XMLEncoder e = new XMLEncoder(out);
			try
			{
				e.writeObject(columnOrders);
			}
			finally
			{
				e.close();
			}
		}
	}

	public synchronized void load()
	{
		File f = Settings.getInstance().getColumnOrderStorage();
		if (!f.exists()) return;

		XMLDecoder e = null;
		try
		{
			FileInputStream in = new FileInputStream(f);
			e = new XMLDecoder(in);
			Object result = e.readObject();
			this.columnOrders = (Map)result;
		}
		catch (Exception ex)
		{
			LogMgr.logError("TableColumnSorter.readFromStream()", "Could not read column order definition", ex);
		}
		finally
		{
			if (e != null) e.close();
		}
	}

}
