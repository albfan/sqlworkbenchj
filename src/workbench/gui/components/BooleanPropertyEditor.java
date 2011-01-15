/*
 * BooleanPropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.EventQueue;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;
import javax.swing.JCheckBox;
import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;


/**
 *
 * @author  Thomas Kellerer
 */
public class BooleanPropertyEditor
	extends JCheckBox
	implements ItemListener, SimplePropertyEditor
{
	private Object source;
	private Method setter;
	private Method getter;
	private boolean changed;
	private String propName;
	private boolean immediateUpdate;

	public void setSourceObject(Object aSource, String aProperty)
	{
		this.source = aSource;
		this.changed = false;
		this.propName = aProperty;
		this.removeItemListener(this);

		if (aSource == null)
		{
			this.getter = null;
			this.setter = null;
			return;
		}

		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);

		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			try
			{
				this.getter = cls.getMethod(name, (Class[])null);
			}
			catch (NoSuchMethodException e)
			{
				this.getter = null;
			}
			if (this.getter == null)
			{
				name = "is" + propertyName;
				this.getter = cls.getMethod(name, (Class[])null);
			}

			name = "set" + propertyName;
			Class[] parms = {boolean.class};
			this.setter = cls.getMethod(name, parms);

			Boolean value = (Boolean)this.getter.invoke(this.source, (Object[])null);
			this.setSelected(value.booleanValue());
			this.addItemListener(this);
		}
		catch (Exception e)
		{
			LogMgr.logError("BooleanPropertyEditor.setSourceObject()", "Error during init", e);
		}
	}

	public void itemStateChanged(ItemEvent evt)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				firePropertyChange(propName, null, Boolean.valueOf(isSelected()));
			}
		});
	}

	public void applyChanges()
	{
		if (!this.changed) return;
		if (this.setter == null) return;
		Object[] args = new Object[1];
		args[0] = Boolean.valueOf(this.isSelected());
		try
		{
			this.setter.invoke(this.source, args);
			this.changed = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("BooleanPropertyEditor.setSourceObject()", "Error when applying changes", e);
		}
	}

	public boolean isChanged()
	{
		return this.changed;
	}

	public void setImmediateUpdate(boolean aFlag)
	{
		this.immediateUpdate = aFlag;
	}

	public boolean getImmediateUpdate()
	{
		return this.immediateUpdate;
	}

}
