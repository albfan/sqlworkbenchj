/*
 * DropDbObjectAction.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.event.ActionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import workbench.gui.components.WbMenuItem;
import workbench.gui.components.WbTable;

/**
 * @author support@sql-workbench.net
 */
public class DropDbItem 
	extends WbMenuItem
	implements ListSelectionListener
{
	private WbTable client;
	
	public DropDbItem(WbTable objects, ActionListener l)
	{
		super();
		setMenuTextByKey("MnuTxtDropDbObject");
		setEnabled(false);
		client = objects;
		client.getSelectionModel().addListSelectionListener(this);
		addActionListener(l);
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		this.setEnabled(client.getSelectedRowCount() > 0);
	}
}
