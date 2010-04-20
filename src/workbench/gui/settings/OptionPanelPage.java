/*
 * OptionPanelPage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.gui.components.DividerBorder;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.util.ExceptionUtil;

/**
 * @author Thomas Kellerer
 */
public class OptionPanelPage
{
	private String label;
	private String pageClass;
	private JPanel panel;
	private Restoreable options;

	public OptionPanelPage(String clz, String key)
	{
		this.label = ResourceMgr.getString(key);
		this.pageClass = "workbench.gui.settings." + clz;
	}

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

}
