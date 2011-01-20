/*
 * CompletionListRenderer
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.completion;

import java.awt.Component;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import workbench.db.ColumnIdentifier;
import workbench.db.DbObject;
import workbench.util.SqlUtil;

/**
 * A ListCellRenderer for the completion popup.
 *
 * If a ColumnIdentifier is displayed, and that is a PK column, the name will be displayed in bold face.
 *
 * For instances of DbObject, the object's comment (remark) is shown as a tooltip.
 * 
 * @author Thomas Kellerer
 */
public class CompletionListRenderer
	extends DefaultListCellRenderer
{

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
	{
		Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		if (value instanceof ColumnIdentifier)
		{
			ColumnIdentifier col = (ColumnIdentifier)value;
			String colname = SqlUtil.removeQuoting(col.getColumnName());
			if (col.isPkColumn())
			{
				setText("<html><b>" + colname + "</b></html>");
			}
			else
			{
				setText(colname);
			}
		}
		if (value instanceof DbObject)
		{
			DbObject dbo = (DbObject)value;
			String comment = dbo.getComment();
			if (comment == null || comment.isEmpty())
			{
				setToolTipText(null);
			}
			else
			{
				setToolTipText(comment);
			}

		}
		return c;
	}

}
