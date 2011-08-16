/*
 * StringColumnRenderer.java
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


/**
 * A renderer to display character data.
 * <br/>
 * This is basically a ToolTipRenderer, but for performance
 * reasons we are assuming the values are all of type string.
 * So we can use a type cast in the getDisplay() method
 * instead of toString() which is much faster when no exceptions
 * are thrown.
 *
 * @author  Thomas Kellerer
 */
public class StringColumnRenderer
	extends ToolTipRenderer
{

	@Override
	public void prepareDisplay(Object aValue)
	{
		try
		{
			this.displayValue = (String)aValue;
		}
		catch (Throwable e)
		{
			displayValue = (aValue == null ? null : aValue.toString());
		}
		setTooltip(displayValue);
	}

}
