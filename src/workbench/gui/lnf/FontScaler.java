/*
 * FontScaler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.awt.Font;
import java.awt.Toolkit;

import workbench.log.LogMgr;
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
		dpi = Toolkit.getDefaultToolkit().getScreenResolution();
		defaultDPI = Settings.getInstance().getIntProperty("workbench.gui.desktop.defaultdpi", 96);
		if (dpi == defaultDPI)
		{
			scaleFont = false;
			scaleFactor = 1.0f;
		}
		else
		{
			scaleFont = true;
			scaleFactor = ((float)dpi / (float)defaultDPI);
		}
	}

	public void logSettings()
	{
		LogMgr.logInfo("FontScaler.logSettings()", "Current DPI: "  + dpi + ", Default DPI: " + defaultDPI + ", scale factor: " + scaleFactor);
	}

	public float getScaleFactor()
	{
		return scaleFactor;
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
