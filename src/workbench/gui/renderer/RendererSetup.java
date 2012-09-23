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

	// These are package visible for performance reasons
	// those values are accessed from within the renderers where every nanosecond counts
	final Color alternateBackground;
	final boolean useAlternatingColors;
	Color nullColor;
	Color modifiedColor;
	String nullString;

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
			nullString = GuiSettings.getDisplayNullString();
		}
		else
		{
			alternateBackground = null;
			useAlternatingColors = false;
			nullColor = null;
			modifiedColor = null;
			nullString = null;
		}
	}

	public static RendererSetup getBaseSetup()
	{
		RendererSetup setup = new RendererSetup(true);
		setup.nullColor = null;
		setup.nullString = null;
		return setup;
	}

	public void setModifiedColor(Color color)
	{
		this.modifiedColor = color;
	}


}
