/*
 * IconListCombobox.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.settings;

import javax.swing.*;


/**
 * A dropdown that display the available icons for the "busy" icon.
 * @author Thomas Kellerer
 */
public class IconListCombobox
	extends JComboBox
{
	public IconListCombobox()
	{
		super();
		setRenderer(new ImagePanel());
	}

	public void done()
	{
		ComboBoxModel model = getModel();
		for (int i=0; i < model.getSize(); i++)
		{
			LoadingImage icon = (LoadingImage)model.getElementAt(i);
			icon.dispose();
		}
	}

	public static ComboBoxModel getBusyIcons()
	{
		return getModel(
			"loading-static",
			"loading_box",
			"loading_small",
			"loading",
			"loading_arrows",
			"spinning",
			"loading_snake",
			"loading_spider");
	}

	public static ComboBoxModel getCancelIcons()
	{
		return getModel(
			"cancelling-static",
			"loading_coffee",
			"cancelling",
			"cancelling-spinning",
			"progress_open",
			"loading_clock",
			"loading_blocks",
			"loading_spider");
	}

	private static ComboBoxModel getModel(String ... pictures)
	{
		LoadingImage[] data = new LoadingImage[pictures.length];
		int i=0;
		for (String picture : pictures)
		{
			data[i] = new LoadingImage(picture);
			i++;
		}
		return new DefaultComboBoxModel(data);
	}

}
