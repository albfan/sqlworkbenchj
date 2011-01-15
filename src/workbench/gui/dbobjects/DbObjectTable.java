/*
 * DbObjectTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;
import javax.swing.ListSelectionModel;
import workbench.gui.components.WbTable;

/**
 * @author Thomas Kellerer
 */
public class DbObjectTable
	extends WbTable
{
	public DbObjectTable()
	{
		super(true, false, false);
		setAutoResizeMode(AUTO_RESIZE_OFF);
		setCellSelectionEnabled(false);
		setColumnSelectionAllowed(false);
		setRowSelectionAllowed(true);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	}
}
