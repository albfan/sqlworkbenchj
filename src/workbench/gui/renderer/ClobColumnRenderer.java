/*
 * ClobColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.sql.Clob;

/**
 * Renderer for CLOB datatype...
 * @author  info@sql-workbench.net
 */
public class ClobColumnRenderer extends ToolTipRenderer
{
	public ClobColumnRenderer()
	{
		super();
	}

	public void prepareDisplay(Object aValue)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!

		try
		{
			Clob clob = (Clob)aValue;
			long len = clob.length();
			this.displayValue = clob.getSubString(1, (int)len);
		}
		catch (Throwable e)
		{
			displayValue = aValue.toString();
		}
		
		if (this.displayValue.length() > 0) 
		{
			this.tooltip = this.displayValue;
		}
		else
		{
			this.tooltip = null;
		}

	}

}
