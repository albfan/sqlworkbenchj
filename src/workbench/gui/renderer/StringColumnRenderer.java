/*
 * StringColumnRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.renderer;


/**
 * This is basically a ToolTipRenderer, but for performance
 * reasons we are assuming the values are all of type string.
 * So we can use a type cast in the getDisplay() method
 * instead of toString() which is much faster when no exceptions
 * are thrown. 
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class StringColumnRenderer extends ToolTipRenderer
{
	public StringColumnRenderer()
	{
		super();
	}

	public void prepareDisplay(Object aValue)
	{
		// this method will not be called with a null value, so we do not need
		// to check it here!
		
		try
		{
			this.displayValue = (String)aValue;
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
