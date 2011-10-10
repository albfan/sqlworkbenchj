/*
 * RendererColors.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.renderer;

import java.awt.Color;
import workbench.resource.GuiSettings;

/**
 *
 * @author Thomas Kellerer
 */
public class RendererSetup
{

	Color alternateBackground;
	boolean useAlternatingColors;
	Color nullColor;
	Color modifiedColor;

	public RendererSetup()
	{
		this(true);
	}

	public RendererSetup(boolean useDefaults)
	{
		if (useDefaults)
		{
			alternateBackground = GuiSettings.getAlternateRowColor();
			useAlternatingColors = GuiSettings.getUseAlternateRowColor();
			nullColor = GuiSettings.getNullColor();
			modifiedColor = null;
		}
		else
		{
			alternateBackground = null;
			useAlternatingColors = false;
			nullColor = null;
			modifiedColor = null;
		}
	}

	public void setAlternateBackground(Color backgroundColor)
	{
		this.alternateBackground = backgroundColor;
	}

	public void setModifiedColor(Color color)
	{
		this.modifiedColor = color;
	}

	public void setNullColor(Color color)
	{
		this.nullColor = color;
	}

	public void setUseAlternatingColors(boolean flag)
	{
		this.useAlternatingColors = flag;
	}

}
