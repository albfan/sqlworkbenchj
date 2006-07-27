/*
 * ComboStringPropertyEditor.java
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;
import workbench.util.StringUtil;



/**
 *	A property editor for a String property, where the values
 *	for the field can also be selected by a dropdown
 * @author  support@sql-workbench.net
 */
public class ComboStringPropertyEditor 
	extends JComboBox 
	implements ItemListener, SimplePropertyEditor, FocusListener, DocumentListener
{
	private Object source;
	private Method setter;
	private Method getter;
	private boolean changed;
	private boolean immediateUpdate = false;
	private String propName;
	private ActionListener listener;
	public ComboStringPropertyEditor()
	{
		super();
	}
	
	public void setSourceObject(Object aSource, String aProperty)
	{
		this.source = aSource;
		this.propName = aProperty;
		this.changed = false;
		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);
		stopEvents();
		
		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, (Class[])null);

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
		startEvents();
	}
	
	private ActionListener getListener()
	{
		if (listener == null)
		{
			listener = new ActionListener()
			{
				public void actionPerformed(ActionEvent evt)
				{
					changed = true;
					applyChanges();
				}
			};
		}
		return listener;
	}
	
	private void stopEvents()
	{
		this.removeItemListener(this);
		if (this.isEditable())
		{
			removeActionListener(getListener());
			JTextField text = (JTextField)getEditor().getEditorComponent();
			//text.removeFocusListener(this);
			text.getDocument().removeDocumentListener(this);
		}
	}
	
	private void startEvents()
	{
		this.addItemListener(this);
		if (this.isEditable())
		{
			addActionListener(getListener());
			JTextField text = (JTextField)getEditor().getEditorComponent();
			//text.addFocusListener(this);
			text.getDocument().addDocumentListener(this);
		}
	}

	public void setModel(ComboBoxModel m)
	{
		stopEvents();
		super.setModel(m);
		if (this.isEditable())
		{
			this.initData();
		}
		startEvents();
	}

	private void initData()
	{
		if (this.getter == null || this.source == null) return;
		try
		{
			
			Object value = this.getter.invoke(this.source, (Object[])null);
			this.setSelectedItem(value);
		}
		catch (Exception e)
		{
			LogMgr.logError("ComboProperty.intiData", "Error", e);
		}
	}
	
	public boolean isChanged() { return this.changed; }
	
	public void applyChanges()
	{
		if (!this.changed) return;
		Object args[] = new Object[1];
		if (this.isEditable())
		{
			args[0] = this.getEditor().getItem().toString();
		}
		else
		{
			args[0] = this.getSelectedItem().toString();
		}
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
	
	public void focusGained(FocusEvent e)
	{
	}
	
	public void focusLost(FocusEvent e)
	{
		this.applyChanges();
	}
	
	public void changedUpdate(DocumentEvent e) { documentChanged(); 	}
	public void insertUpdate(DocumentEvent e) { documentChanged(); }
	public void removeUpdate(DocumentEvent e) { documentChanged(); }
	
	private void documentChanged()
	{
		this.changed = true;
		this.applyChanges();
	}
}

