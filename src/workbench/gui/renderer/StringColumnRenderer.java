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
		if (aValue == null)
		{
			displayValue[0] = StringUtil.EMPTY_STRING;
			displayValue[1] = null;
		}
		else
		{
			// this is the tooltip
			displayValue[1] = null;
			
			try
			{
				displayValue[0] = (String)aValue;
			}
			catch (ClassCastException e)
			{
				displayValue[0] = aValue.toString();
			}
			if (displayValue[0].length() > 0) displayValue[1] = displayValue[0];
		}
	}

}
