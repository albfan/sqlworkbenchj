/*
 * ClobColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.sql.Clob;

/**
 * Renderer for CLOB datatype...
 * @author  workbench@kellerer.org
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
