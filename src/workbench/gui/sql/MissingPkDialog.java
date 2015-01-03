/*
 * MissingPkDialog.java
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
 * @author Thomas Kellerer
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
		if (this.columns == null || this.columns.isEmpty()) return true;
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
