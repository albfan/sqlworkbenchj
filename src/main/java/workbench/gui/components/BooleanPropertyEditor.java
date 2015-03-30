/*
 * BooleanPropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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

	@Override
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

	@Override
	public void itemStateChanged(ItemEvent evt)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				firePropertyChange(propName, null, Boolean.valueOf(isSelected()));
			}
		});
	}

	@Override
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

	@Override
	public boolean isChanged()
	{
		return this.changed;
	}

	@Override
	public void setImmediateUpdate(boolean aFlag)
	{
		this.immediateUpdate = aFlag;
	}

	@Override
	public boolean getImmediateUpdate()
	{
		return this.immediateUpdate;
	}

}
