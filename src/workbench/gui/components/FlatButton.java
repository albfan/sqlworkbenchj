/*
 * FlatButton.java
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

import java.awt.Insets;

import javax.swing.Action;
import javax.swing.Icon;

import workbench.WbManager;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class FlatButton
	extends WbButton
{

	public static final Insets SMALL_MARGIN = new Insets(3,5,3,5);
	public static final Insets LARGER_MARGIN = new Insets(5,7,5,7);
	private boolean useDefaultMargin;
	private Insets customInsets;
	private String enableMsgKey;

	public FlatButton()
	{
		super();
		init();
	}

	public FlatButton(Action action)
	{
		super(action);
		init();
	}

	public FlatButton(Icon icon)
	{
		super(icon);
		init();
	}

	public FlatButton(String label)
	{
		super(label);
		init();
	}

	public void showMessageOnEnable(String resourceKey)
	{
		this.enableMsgKey = resourceKey;
	}

	private void init()
	{
		// WbManager.getInstance() can be null if this component
		// is created e.g. in the GUI designer of NetBeans
		if (WbManager.getInstance() == null) return;
		if (WbManager.getInstance().isWindowsClassic())
		{
			setFlatLook();
		}
	}

	public void setFlatLook()
	{
		this.setBorder(WbSwingUtilities.FLAT_BUTTON_BORDER);
	}

	public void setUseDefaultMargin(boolean useDefaultMargin)
	{
		this.useDefaultMargin = useDefaultMargin;
	}

	public void setCustomInsets(Insets insets)
	{
		this.customInsets = insets;
	}

	@Override
	public Insets getInsets()
	{
		if (useDefaultMargin)
		{
			return super.getInsets();
		}
		return customInsets == null ? SMALL_MARGIN : customInsets;
	}

	@Override
	public Insets getMargin()
	{
		if (useDefaultMargin)
		{
			return super.getMargin();
		}
		return customInsets == null ? SMALL_MARGIN : customInsets;
	}

	@Override
	public void setEnabled(boolean flag)
	{
		boolean wasEnabled = this.isEnabled();
		super.setEnabled(flag);
		if (flag && !wasEnabled && this.enableMsgKey != null)
		{
			WbSwingUtilities.showToolTip(this, "<html><p style=\"margin:4px\">" + ResourceMgr.getString(enableMsgKey) + "</p></html>");
		}
	}
}
