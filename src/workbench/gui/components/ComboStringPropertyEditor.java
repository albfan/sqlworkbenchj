/*
 * ComboStringPropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

import workbench.interfaces.SimplePropertyEditor;



/**
 *	A property editor for a String property, where the values
 *	for the field can also be selected by a dropdown
 * @author  info@sql-workbench.net
 */
public class ComboStringPropertyEditor 
	extends JComboBox 
	implements ItemListener, SimplePropertyEditor, FocusListener
{
	private Object source;
	private Method setter;
	private Method getter;
	private boolean changed;
	private String propName;
	private boolean immediateUpdate = false;
	
	public ComboStringPropertyEditor()
	{
		super();
		//this.addFocusListener(this);
	}
	
	public void setSourceObject(Object aSource, String aProperty)
	{
		this.source = aSource;
		this.changed = false;
		this.propName = aProperty;
		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);
		
		this.removeItemListener(this);
		
		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, null);

			name = "set" + propertyName;
			Class[] parms = {String.class};

			this.setter = cls.getMethod(name, parms);
			//this.setEditable(true);
			if (this.getModel() != null) 
			{
				this.initData();
			}
		}
		catch (Exception e)
		{
			System.out.println("Error on init");
			e.printStackTrace();
		}
		this.addItemListener(this);
	}
	
	public void setModel(ComboBoxModel m)
	{
		super.setModel(m);
		this.initData();
	}

	private void initData()
	{
		try
		{
			Object value = this.getter.invoke(this.source, null);
			this.setSelectedItem(value);
		}
		catch (Exception e)
		{
		}
	}
	
	private int findObject(Object aValue)
	{
		if (aValue == null) return -1;
		ComboBoxModel model = this.getModel();
		for (int i=0; i < model.getSize(); i++)
		{
			Object element = model.getElementAt(i);
			if (aValue.equals(element)) return i;
		}
		return -1;
	}
	
	public boolean isChanged() { return this.changed; }
	
	public void applyChanges()
	{
		if (!this.changed) return;
		Object args[] = new Object[1];
		args[0] = this.getSelectedItem().toString();
		try
		{
			this.setter.invoke(this.source, args);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/** Invoked when an item has been selected or deselected by the user.
	 * The code written for this method performs the operations
	 * that need to occur when an item is selected (or deselected).
	 *
	 */
	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			this.changed = true;
		}
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
	}
	public void setImmediateUpdate(boolean aFlag)
	{
		this.immediateUpdate = aFlag;
		if (aFlag) this.applyChanges();
	}
	
	public boolean getImmediateUpdate()
	{
		return this.immediateUpdate;
	}
	
	/** Invoked when a component gains the keyboard focus.
	 *
	 */
	public void focusGained(FocusEvent e)
	{
	}
	
	/** Invoked when a component loses the keyboard focus.
	 *
	 */
	public void focusLost(FocusEvent e)
	{
		if (!this.immediateUpdate) this.applyChanges();
	}
	
}

