/*
 * RendererSetup.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
  int nullFontStyle;

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
      nullFontStyle = GuiSettings.getDisplayNullFontStyle();
		}
		else
		{
			alternateBackground = null;
			useAlternatingColors = false;
			nullColor = null;
			modifiedColor = null;
			nullString = null;
      nullFontStyle = 0;
		}
	}

	public static RendererSetup getBaseSetup()
	{
		RendererSetup setup = new RendererSetup(true);
		setup.nullColor = null;
		setup.nullString = null;
    setup.nullFontStyle = 0;
		return setup;
	}

	public void setModifiedColor(Color color)
	{
		this.modifiedColor = color;
	}


}
