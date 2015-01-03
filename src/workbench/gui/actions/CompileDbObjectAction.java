/*
 * CompileDbObjectAction.java
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
package workbench.gui.actions;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.log.LogMgr;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;

import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectCompilerUI;

/**
 * @author Thomas Kellerer
 */
public class CompileDbObjectAction
	extends WbAction
	implements ListSelectionListener
{
	private JMenuItem menuItem;
	private DbObjectList source;
	private ListSelectionModel selection;

	public CompileDbObjectAction(DbObjectList client, ListSelectionModel list)
	{
		super();
		this.initMenuDefinition("MnuTxtRecompile");
		this.source = client;
		this.selection = list;
		setVisible(false);
		setEnabled(false);
	}

	public void setVisible(boolean flag)
	{
		if (this.menuItem == null)
		{
			menuItem = getMenuItem();
		}
		menuItem.setVisible(flag);
	}

	public void setConnection(WbConnection conn)
	{
		if (conn != null && conn.getMetadata().isOracle())
		{
			this.setVisible(true);
			selection.addListSelectionListener(this);
			checkState();
		}
		else
		{
			selection.removeListSelectionListener(this);
			this.setVisible(false);
			this.setEnabled(false);
		}
	}

	@Override
	public void executeAction(ActionEvent e)
	{
		compileObjects();
	}

	private void compileObjects()
	{
		if (!WbSwingUtilities.isConnectionIdle(source.getComponent(), source.getConnection()))
		{
			return;
		}

		List<DbObject> objects = getSelectedObjects();
		if (objects == null || objects.isEmpty())
		{
			return;
		}

		try
		{
			ObjectCompilerUI compilerUI = new ObjectCompilerUI(objects, this.source.getConnection());
			compilerUI.show(SwingUtilities.getWindowAncestor(source.getComponent()));
		}
		catch (SQLException e)
		{
			LogMgr.logError("ProcedureListPanel.compileObjects()", "Error initializing ObjectCompilerUI", e);
		}
	}

	private List<DbObject> getSelectedObjects()
	{
		List<? extends DbObject> selected = this.source.getSelectedObjects();
		if (selected == null || selected.isEmpty())
		{
			return null;
		}

		List<String> catalogs = new ArrayList<>();
		List<DbObject> objects = new ArrayList<>();
		for (DbObject dbo : selected)
		{
			if (!OracleObjectCompiler.canCompile(dbo))
			{
				// next selected element
				continue;
			}

			if (dbo instanceof ProcedureDefinition)
			{
				ProcedureDefinition pd = (ProcedureDefinition) dbo;
				if (pd.isOraclePackage())
				{
					// keep only one package-procedure per catalog
					if (!catalogs.contains(pd.getCatalog()))
					{
						catalogs.add(pd.getCatalog());
					}
					else
					{
						// a stored procedure was already added for the catalog
						continue;
					}
				}
			}
			objects.add(dbo);
		}

		return objects;
	}

	private void checkState()
	{
		List<DbObject> selected = getSelectedObjects();
		this.setEnabled(selected != null && selected.size() > 0);
	}

	@Override
	public void valueChanged(ListSelectionEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				checkState();
			}
		});
	}
}
