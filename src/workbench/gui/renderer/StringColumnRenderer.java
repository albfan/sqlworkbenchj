/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;

import javax.swing.SwingConstants;

import workbench.WbManager;
import workbench.util.StringUtil;

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
