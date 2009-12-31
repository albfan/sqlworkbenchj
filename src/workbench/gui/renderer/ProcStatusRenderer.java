/*
 * ProcStatusRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import javax.swing.JLabel;
import workbench.db.JdbcProcedureReader;

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

	public void prepareDisplay(Object value)
	{
		Integer status = (Integer)value;
		this.displayValue = JdbcProcedureReader.convertProcType(status.intValue());
	}

}
