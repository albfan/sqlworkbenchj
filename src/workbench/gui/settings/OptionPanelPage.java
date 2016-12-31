/*
 * OptionPanelPage.java
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
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.Disposable;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.components.DividerBorder;

import workbench.util.CollectionUtil;
import workbench.util.ExceptionUtil;

/**
 * @author Thomas Kellerer
 */
public class OptionPanelPage
{
  public static final Border PAGE_BORDER = new EmptyBorder(8, 8, 8, 8);
  private static final Set<String> NO_BORDER_PANELS = CollectionUtil.treeSet("LnFOptionsPanel", "ExternalToolsPanel", "FormatterOptionsPanel", "DbExplorerOptionsPanel");

	private String label;
	private String pageClass;
	private JPanel panel;
	private Restoreable options;
  private boolean addBorder;

	public OptionPanelPage(String clz, String key)
	{
		this.label = ResourceMgr.getString(key);
		this.pageClass = "workbench.gui.settings." + clz;
    addBorder = !NO_BORDER_PANELS.contains(clz);
	}

	@Override
	public String toString()
	{
		return this.label;
	}

	public String getLabel()
	{
		return label;
	}

	public JPanel getPanel()
	{
		if (this.panel == null)
		{
			try
			{
				Class clz = Class.forName(this.pageClass);
				JPanel optionPanel = (JPanel)clz.newInstance();
				this.options = (Restoreable)optionPanel;
				this.options.restoreSettings();

				JLabel title = new JLabel(this.label);
				title.setName("pagetitle");
				title.setOpaque(true);
				title.setBackground(Color.WHITE);
				Font f = title.getFont();
				Font f2 = f.deriveFont(Font.BOLD, 12.0f);
				//title.setBorder();
				title.setBorder(new CompoundBorder(DividerBorder.BOTTOM_DIVIDER, new EmptyBorder(4,6,4,6)));
				title.setFont(f2);
				panel = new JPanel(new BorderLayout());
        if (addBorder)
        {
          optionPanel.setBorder(PAGE_BORDER);
        }
				panel.setBorder(BorderFactory.createEtchedBorder());
				panel.add(title, BorderLayout.NORTH);
				panel.add(optionPanel, BorderLayout.CENTER);
			}
			catch (Exception e)
			{
				LogMgr.logError("OptionPanelPage.getPanel()", "Could not create panel", e);
				panel = new JPanel();
				panel.add(new JLabel(ExceptionUtil.getDisplay(e)));
			}
		}
		return this.panel;
	}

	public boolean validateInput()
	{
		if (this.options instanceof ValidatingComponent)
		{
			ValidatingComponent vc = (ValidatingComponent)this.options;
			return vc.validateInput();
		}
		return true;
	}

	public void saveSettings()
	{
		try
		{
			if (this.options != null)
			{
				options.saveSettings();
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OptionPanelPage.restoreSettings()", "Could not save panel settings", e);
		}
	}

	public void dispose()
	{
		if (options instanceof Disposable)
		{
			((Disposable)options).dispose();
		}
	}

}
