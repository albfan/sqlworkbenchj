/*
 * PasswordPropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;

import javax.swing.JPasswordField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;

/**
 *
 * @author  Thomas Kellerer
 */
public class PasswordPropertyEditor
	extends JPasswordField
	implements DocumentListener, SimplePropertyEditor, FocusListener
{
	private Object source;
	private Method setter;
	private Method getter;
	private boolean changed;
	private String propName;
	private boolean immediateUpdate;

	public PasswordPropertyEditor()
	{
		super();
		this.addFocusListener(this);
	}

	@Override
	public void setSourceObject(Object aSource, String aProperty)
	{
		this.source = aSource;
		this.changed = false;
		this.propName = aProperty;
		//String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);

		this.getDocument().removeDocumentListener(this);

		try
		{
			String name = "decryptPassword";
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, (Class[])null);

			name = "setPassword";
			Class[] parms = {String.class};
			this.setter = cls.getMethod(name, parms);

			String value = (String)this.getter.invoke(this.source, (Object[])null);
			this.setText(value);
		}
		catch (Exception e)
		{
			LogMgr.logError("PasswordPropertyEditor.setSourceObject()", "Error during init", e);
		}
		this.getDocument().addDocumentListener(this);
	}

	@Override
	public void applyChanges()
	{
		if (!this.changed) return;
		if (this.source == null) return;
		if (this.setter == null) return;

		// getPassword returns a char[] so this needs to be converted to a String
		Object[] args = new Object[] { new String(getPassword()) };

		try
		{
			this.setter.invoke(this.source, args);
			this.changed = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("PasswordPropertyEditor.applyChanges", "Error applying changes", e);
		}
	}

	@Override
	public boolean isChanged()
	{
		return this.changed;
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
	}

	@Override
	public void setImmediateUpdate(boolean aFlag)
	{
		this.immediateUpdate = aFlag;
		if (aFlag) this.applyChanges();
	}

	@Override
	public boolean getImmediateUpdate()
	{
		return this.immediateUpdate;
	}

	/** Invoked when a component gains the keyboard focus.
	 *
	 */
	@Override
	public void focusGained(FocusEvent e)
	{
	}

	/** Invoked when a component loses the keyboard focus.
	 *
	 */
	@Override
	public void focusLost(FocusEvent e)
	{
		if (!this.immediateUpdate) this.applyChanges();
	}

}
