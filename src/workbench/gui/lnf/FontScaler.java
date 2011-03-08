/*
 * FontScaler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.awt.Font;
import java.awt.Toolkit;
import workbench.resource.Settings;

/**
 * A class to scale fonts according to the DPI settings of the Desktop.
 * The reference (i.e. a scale of 1.0) is assumed to be 96 DPI
 *
 * @author Thomas Kellerer
 */
public class FontScaler
{

	private boolean scaleFont;
	private final int dpi;
	private final int defaultDPI;
	private final float scaleFactor;

	public FontScaler()
	{
		scaleFont = Settings.getInstance().getScaleFonts();
		dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		defaultDPI = Settings.getInstance().getIntProperty("workbench.gui.desktop.defaultdpi", 96);
		if (dpi == defaultDPI)
		{
			scaleFont = false;
			scaleFactor = 1.0f;
		}
		else
		{
			scaleFactor = ((float)dpi / (float)defaultDPI);
		}
	}

	public Font scaleFont(Font baseFont)
	{
		if (!scaleFont) return baseFont;
		if (baseFont == null) return null;

		float oldSize2D = baseFont.getSize2D();
		float newSize2D = oldSize2D * scaleFactor;
		Font scaled = baseFont.deriveFont(newSize2D);
		return scaled;
	}

}
