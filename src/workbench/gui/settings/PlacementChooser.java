/*
 * PlacementChooser.java
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

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class PlacementChooser
	extends JComboBox
{
	private String placementProperty = "workbench.gui.dbobjects.tabletabs";
	public PlacementChooser()
	{
		super();
		String[] locations = new String[] {
			ResourceMgr.getString("TxtTabTop"),
			ResourceMgr.getString("TxtTabBottom"),
			ResourceMgr.getString("TxtTabLeft"),
			ResourceMgr.getString("TxtTabRight"),
		};
		setModel(new DefaultComboBoxModel(locations));
	}

	public void setProperty(String property)
	{
		placementProperty = property;
		String placement = Settings.getInstance().getProperty(placementProperty, "bottom");
		if ("top".equals(placement))
		{
			setSelectedIndex(0);
		}
		else if ("bottom".equals(placement))
		{
			setSelectedIndex(1);
		}
		if ("left".equals(placement))
		{
			setSelectedIndex(2);
		}
		if ("right".equals(placement))
		{
			setSelectedIndex(3);
		}
	}

	public static int getLocationProperty(String property)
	{
		String tabLocation = Settings.getInstance().getProperty(property, "bottom");
		int location = JTabbedPane.BOTTOM;
		if (tabLocation.equalsIgnoreCase("top"))
		{
			location = JTabbedPane.TOP;
		}
		else if (tabLocation.equalsIgnoreCase("left"))
		{
			location = JTabbedPane.LEFT;
		}
		else if (tabLocation.equalsIgnoreCase("right"))
		{
			location = JTabbedPane.RIGHT;
		}
		return location;
	}
	
	public void saveSelection()
	{
		String placement = getPlacement();
		Settings.getInstance().setProperty(placementProperty, placement);
	}
	
	private String getPlacement()
	{
		int placement = getSelectedIndex();
		switch (placement)
		{
			case 0:
				return "top";
			case 1:
				return "bottom";
			case 2:
				return "left";
			case 3:
				return "right";
		}
		return "bottom";
	}
}
