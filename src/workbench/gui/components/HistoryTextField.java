/*
 * HistoryTextField.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import workbench.interfaces.PropertyStorage;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class HistoryTextField
	extends JComboBox
{
	private String propName;
	private List values;
	private int maxHistorySize = 25;
		
	public HistoryTextField(String prop)
	{
		super();
		setEditable(true);
		this.propName = prop;
		this.maxHistorySize = Settings.getInstance().getIntProperty("workbench.history." + propName + ".size", 25);
	}
	
	public String getText()
	{
		Object item = getSelectedItem();
		if (item == null) item = getEditor().getItem();
		if (item == null) return "";
		return (String)item;
	}
	
	public void setText(String s)
	{
//		this.getEditor().setItem(s);
		this.setSelectedItem(s);
	}

	public void saveSettings(PropertyStorage props, String prefix)
	{
		props.setProperty(prefix + "history", StringUtil.listToString(values, ';'));
		props.setProperty(prefix + "lastvalue", this.getText());
	}
	
	public void restoreSettings(PropertyStorage props, String prefix)
	{
		String s = props.getProperty(prefix + "history", "");
		if (StringUtil.isEmptyString(s))
			this.values = new ArrayList();
		else
			this.values = StringUtil.stringToList(s, ";", true, true);
		this.updateModel();
		String lastValue = props.getProperty(prefix + "lastvalue", null);
		if (lastValue != null) this.setText(lastValue);
	}
	
	public void restoreSettings()
	{
		restoreSettings(Settings.getInstance(), "workbench.quickfilter." + propName + ".");
	}

	public void saveSettings()
	{
		saveSettings(Settings.getInstance(), "workbench.quickfilter." + propName + ".");
	}
	
	public void addToHistory(String s)
	{
		if (StringUtil.isEmptyString(s)) return;
		s = s.trim();	
		Object item = getSelectedItem();
		int index = values.indexOf(s);
		if (index > -1)
		{
			this.values.remove(index);
		}
		else
		{
			while (this.values.size() >= this.maxHistorySize)
			{
				this.values.remove(values.size() - 1);
			}
		}
		this.values.add(0,s);
		this.updateModel();
		setSelectedItem(item);
	}
	
	private void updateModel()
	{
		DefaultComboBoxModel model = new DefaultComboBoxModel(this.values.toArray());
		setModel(model);
	}
}
