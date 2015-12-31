/*
 * FocusIndicator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.components;

import java.awt.Color;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import workbench.resource.Settings;

/**
 * A class that draws a border around the currently focused component
 *
 * @author Thomas Kellerer
 */
public class FocusIndicator
	implements FocusListener
{
	private Border focusBorder;
	private Border noFocusBorder;
	private Border originalBorder;
	private JComponent focusClient;
	private JComponent borderClient;
	private Color borderColor;

	public FocusIndicator(JComponent focusToCheck, JComponent client)
	{
		focusClient = focusToCheck;
		focusClient.addFocusListener(this);
		borderClient = client;
		borderColor = Settings.getInstance().getColor("workbench.gui.focusindicator.bordercolor", Color.YELLOW.brighter());
		initBorder();
	}

	private void initBorder()
	{
		if (noFocusBorder == null && focusBorder == null && originalBorder == null)
		{
			originalBorder = borderClient.getBorder();
			noFocusBorder = new CompoundBorder(new EmptyBorder(1,1,1,1), originalBorder);
			borderClient.setBorder(noFocusBorder);
			focusBorder = new CompoundBorder(new LineBorder(borderColor, 1), originalBorder);
		}
	}

	public void dispose()
	{
		if (this.focusClient != null)
		{
			focusClient.removeFocusListener(this);
		}

		if (this.borderClient != null && originalBorder != null)
		{
			this.borderClient.setBorder(originalBorder);
		}
	}

	@Override
	public void focusGained(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			initBorder();
			this.borderClient.setBorder(focusBorder);
		}
	}

	@Override
	public void focusLost(FocusEvent e)
	{
		if (this.borderClient != null)
		{
			initBorder();
			this.borderClient.setBorder(noFocusBorder);
		}
	}

}
