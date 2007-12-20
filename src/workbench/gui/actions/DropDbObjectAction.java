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
import java.util.List;
import javax.swing.JFrame;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.db.DbObject;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dbobjects.DbObjectList;
import workbench.gui.dbobjects.ObjectDropperUI;
import workbench.interfaces.ObjectDropper;
import workbench.interfaces.Reloadable;
/**
 * @author support@sql-workbench.net
 */
public class DropDbObjectAction 
	extends WbAction
	implements ListSelectionListener
{
	private DbObjectList source;
	private ListSelectionModel selection;
	private ObjectDropper dropper;
	private Reloadable data;
	private boolean available = true;
	
	public DropDbObjectAction(DbObjectList client, ListSelectionModel list, Reloadable r)
	{
		this("MnuTxtDropDbObject", client, list, r);
	}
	
	public DropDbObjectAction(String labelKey, DbObjectList client, ListSelectionModel list, Reloadable r)
	{
		this.initMenuDefinition(labelKey);
		this.source = client;
		this.selection = list;
		this.data = r;
		setEnabled(false);
		list.addListSelectionListener(this);
	}
	
	@Override
	public void executeAction(ActionEvent e)
	{
		dropObjects();
	}

	public void setAvailable(boolean flag)
	{
		this.available = flag;
		if (!available) this.setEnabled(false);
	}
	
	public void setDropper(ObjectDropper drop)
	{
		this.dropper = drop;
	}
	
	private void dropObjects()
	{
		if (!WbSwingUtilities.checkConnection(source.getComponent(), source.getConnection())) return;
		
		List<DbObject> objects = source.getSelectedObjects();
		if (objects == null || objects.size() == 0) return;

		ObjectDropperUI dropperUI = new ObjectDropperUI(dropper);
		dropperUI.setObjects(objects);
		dropperUI.setObjectTable(source.getObjectTable());
		dropperUI.setConnection(source.getConnection());
		
		JFrame f = (JFrame)SwingUtilities.getWindowAncestor(source.getComponent());
		dropperUI.showDialog(f);
		
		if (!dropperUI.dialogWasCancelled() && data != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					data.reload();
				}
			});
		}
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		setEnabled(this.available && this.selection.getMinSelectionIndex() >= 0);
	}
	
}
