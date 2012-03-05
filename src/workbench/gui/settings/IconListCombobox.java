/*
 * IconListCombobox.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
		LoadingImage[] data = new LoadingImage[6];
		data[0] = new LoadingImage("loading-static");
		data[1] = new LoadingImage("loading_box");
		data[2] = new LoadingImage("loading_small");
		data[3] = new LoadingImage("loading");
		data[4] = new LoadingImage("loading_arrows");
		data[5] = new LoadingImage("spinning");
		return new DefaultComboBoxModel(data);
	}

	public static ComboBoxModel getCancelIcons()
	{
		LoadingImage[] data = new LoadingImage[3];
		data[0] = new LoadingImage("cancelling-static");
		data[1] = new LoadingImage("cancelling");
		data[2] = new LoadingImage("cancelling-spinning");
		return new DefaultComboBoxModel(data);
	}

}
