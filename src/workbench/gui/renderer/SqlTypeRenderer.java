/*
 * SqlTypeRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;



import javax.swing.JLabel;
import workbench.util.SqlUtil;

/**
 * Displays the integer values from java.sql.Types as readable names.
 *
 * @see workbench.util.SqlUtil#getTypeName(int)
 *
 * @author Thomas Kellerer
 */
public class SqlTypeRenderer
	extends ToolTipRenderer
{

	public SqlTypeRenderer()
	{
		super();
		this.setHorizontalAlignment(JLabel.LEFT);
	}

	@Override
	public void prepareDisplay(Object value)
	{
		if (value != null)
		{
			try
			{
				int type = ((Integer)value).intValue();
				displayValue = SqlUtil.getTypeName(type);
			}
			catch (Exception e)
			{
				displayValue = value.toString();
			}
			setToolTipText(displayValue);
		}
		else
		{
			displayValue = "";
			this.setToolTipText(null);
		}
	}

}
