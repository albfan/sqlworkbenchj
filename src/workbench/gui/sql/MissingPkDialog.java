/*
 * MissingPkDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.Component;
import java.awt.Window;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import workbench.db.ColumnIdentifier;
import workbench.gui.components.ValidatingDialog;
import workbench.resource.ResourceMgr;

/**
 * @author support@sql-workbench.net
 */
public class MissingPkDialog
{
	private List<ColumnIdentifier> columns;
	public MissingPkDialog(List<ColumnIdentifier> cols)
	{
		this.columns = cols;
	}
	
	public boolean checkContinue(Component caller)
	{
		if (this.columns == null || this.columns.size() == 0) return true;
		StringBuilder msg = new StringBuilder(100);

		msg.append("<html>");
		msg.append("<p style=\"padding:3px;background:white\">");
		msg.append(ResourceMgr.getString("TxtMissingPk1"));
		msg.append("</p><p style=\"margin-top:5px;background:white\">");
		for (ColumnIdentifier col : columns)
		{
			msg.append("&nbsp;&raquo;&nbsp;<tt>");
			msg.append(col.getColumnName());
			msg.append("</tt><br>");
		}
		msg.append("</p><br><center><b style=\"color:red\">");
		msg.append(ResourceMgr.getString("TxtMissingPk2"));
		msg.append("</b></center><br><b><center>");
		msg.append(ResourceMgr.getString("TxtMissingPk3"));
		msg.append("</b></center><br><br></html>");

		Window parent = SwingUtilities.getWindowAncestor(caller);
		boolean ok = ValidatingDialog.showConfirmDialog(parent, new JLabel(msg.toString()), ResourceMgr.getString("TxtMissingPkTitle"), 1);
		return ok;
		
	}
}
