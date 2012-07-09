/*
 * ProcStatusRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import javax.swing.JLabel;
import workbench.db.JdbcProcedureReader;
import workbench.log.LogMgr;

/**
 * Displays the return type of a stored procedure as a readable text.
 * <br/>
 * @see workbench.db.JdbcProcedureReader#convertProcType(int)
 *
 * * @author Thomas Kellerer
 */
public class ProcStatusRenderer
	extends ToolTipRenderer
{

	public ProcStatusRenderer()
	{
		super();
		this.setHorizontalAlignment(JLabel.LEFT);
	}

	@Override
	public void prepareDisplay(Object value)
	{
		try
		{
			Integer status = (Integer)value;
			this.displayValue = JdbcProcedureReader.convertProcType(status.intValue());
		}
		catch (ClassCastException cce)
		{
			LogMgr.logWarning("ProdStatusRenderer.prepareDisplay()", "The current value (" + value + ") is not an Integer!", cce);
			this.displayValue = (value == null ? "" : value.toString());
		}
	}

}
