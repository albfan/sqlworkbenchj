/*
 * ProfileFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;
/*
 * ProfileFilter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.resource.ResourceMgr;


/**
 * @author support@sql-workbench.net
 */
public class ProfileFilter
	extends JPanel
	implements ActionListener, PropertyChangeListener
{
	private ArrayList groups;
	private ProfileListModel data;
	private JComboBox combo = new JComboBox();
	
	public ProfileFilter(ProfileListModel model)
	{
		data = model;
		readGroups();
		this.setLayout(new BorderLayout(2,0));
		JLabel l = new JLabel(ResourceMgr.getString("LblFilterProfiles"));
		String tip = ResourceMgr.getDescription("LblFilterProfiles");
		l.setToolTipText(tip);
		combo.setToolTipText(tip);
		this.add(l, BorderLayout.WEST);
		this.add(combo, BorderLayout.CENTER);
		ConnectionMgr.getInstance().addProfileGroupChangeListener(this);
	}

	public void setGroupFilter(String group)
	{
		if (group == null)
		{
			this.combo.setSelectedIndex(0);
		}
		else
		{
			this.combo.setSelectedItem(group);
		}
	}
	
	public String getCurrentGroup()
	{
		String group = (String)combo.getSelectedItem();
		if (group == null || group.equals("*"))
		{
			return null;
		}
		return group;
	}
	
	public void actionPerformed(ActionEvent e)
	{
//		if (e.getSource() == this.combo)
//		{
//			String group = getCurrentGroup();
//			data.setGroupFilter(group);
//		}
	}

	public void done()
	{
		ConnectionMgr.getInstance().removeProfileGroupChangeListener(this);
	}
	
	public void readGroups()
	{
		this.combo.removeActionListener(this);
		String current = getCurrentGroup();
		Collection groups = ConnectionMgr.getInstance().getProfileGroups();
		DefaultComboBoxModel cmodel = new DefaultComboBoxModel();
		cmodel.addElement("*");
		Iterator itr = groups.iterator();
		while (itr.hasNext())
		{
			cmodel.addElement(itr.next());
		}
		this.combo.setModel(cmodel);
		this.setGroupFilter(current);
		this.combo.addActionListener(this);
	}
	
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals(ConnectionProfile.PROPERTY_PROFILE_GROUP) && evt.getSource() instanceof ConnectionProfile)
		{
			readGroups();
		}
	}
	
}
