/*
 * StringPropertyEditor.java
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
import java.lang.reflect.Method;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import workbench.interfaces.SimplePropertyEditor;


/**
 *
 * @author  info@sql-workbench.net
 */
public class StringPropertyEditor 
	extends JTextField 
	implements DocumentListener, SimplePropertyEditor, FocusListener
{
	private Object source;
	private Method setter;
	private Method getter;
	private boolean changed;
	private boolean immediateUpdate = false;
	
	private String propName;
	
	public StringPropertyEditor()
	{
		super();
		this.addFocusListener(this);
		this.addMouseListener(new TextComponentMouseListener());		
	}
	
	public void setSourceObject(Object aSource, String aProperty)
	{
		this.setSourceObject(aSource, aProperty, null);
	}
	public void setSourceObject(Object aSource, String aProperty, String initialText)
	{
		this.source = aSource;
		this.changed = false;
		this.propName = aProperty;
		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);
		
		this.getDocument().removeDocumentListener(this);
		if (initialText != null)
		{
			this.setText(initialText);
		}
		
		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, null);

			name = "set" + propertyName;
			Class[] parms = {String.class};
			this.setter = cls.getMethod(name, parms);

			String value = (String)this.getter.invoke(this.source, null);
			this.setText(value);
		}
		catch (Exception e)
		{
			System.out.println("Error on init");
			e.printStackTrace();
		}
		this.getDocument().addDocumentListener(this);
	}
	
	public void applyChanges()
	{
		if (!this.changed) return;
		if (this.source == null) return;
		if (this.setter == null) return;
		Object args[] = new Object[1];
		args[0] = this.getText();
		try
		{
			this.setter.invoke(this.source, args);
			this.changed = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isChanged() { return this.changed; }
	
	public void changedUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
	}
	
	public void insertUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
	}
	
	public void removeUpdate(DocumentEvent e)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChange(this.propName, null, null);
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

