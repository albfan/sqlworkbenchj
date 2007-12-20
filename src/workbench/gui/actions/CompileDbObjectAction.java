/*
 * CompileDbObjectAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import workbench.db.DbObject;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectCompilerUI;
import workbench.log.LogMgr;
/**
 * @author support@sql-workbench.net
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
		if (!WbSwingUtilities.checkConnection(source.getComponent(), source.getConnection())) return;
		
		List<DbObject> objects = getSelectedObjects();
		if (objects == null || objects.size() == 0) return;
		
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
		List<DbObject> selected = this.source.getSelectedObjects();
		if (selected == null || selected.size() == 0) return null;
		
		List<DbObject> objects = new ArrayList<DbObject>(selected.size());
		for (DbObject dbo : selected)
		{
			if (OracleObjectCompiler.canCompile(dbo))
			{
				objects.add(dbo);
			}
		}
		return objects;
	}
	
	private void checkState()
	{
		List<DbObject> selected = getSelectedObjects();
		this.setEnabled(selected != null && selected.size() > 0);
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				checkState();
			}
		});
	}
	
}
