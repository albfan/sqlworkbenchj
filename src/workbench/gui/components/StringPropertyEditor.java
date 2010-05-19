/*
 * StringPropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.EventQueue;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;


/**
 *
 * @author  Thomas Kellerer
 */
public class StringPropertyEditor
	extends JTextField
	implements DocumentListener, SimplePropertyEditor, FocusListener
{
	private Object source;
	private Method setter;
	private Method getter;

	// "dirty" flag, if this is true, the target object
	// has not been updated to reflect the state of this editor
	private boolean changed;

	private boolean immediateUpdate;
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

		this.getDocument().removeDocumentListener(this);

		if (aSource == null)
		{
			this.setText("");
			this.getter = null;
			this.setter = null;
			return;
		}

		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);

		if (initialText != null)
		{
			this.setText(initialText);
		}

		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, (Class[])null);

			name = "set" + propertyName;
			Class[] parms = {String.class};
			this.setter = cls.getMethod(name, parms);

			if (initialText == null)
			{
				String value = (String)this.getter.invoke(this.source, (Object[])null);
				this.setText(value);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("StringPropertyEditor.setSourceObject()", "Error during init", e);
		}
		this.getDocument().addDocumentListener(this);
	}

	public void applyChanges()
	{
		if (!this.changed) return;
		if (this.source == null) return;
		if (this.setter == null) return;
		Object[] args = new Object[1];
		args[0] = this.getText();
		try
		{
			this.setter.invoke(this.source, args);
			this.changed = false;
		}
		catch (Exception e)
		{
			LogMgr.logError("StringPropertyEditor.setSourceObject()", "Error when applying changes", e);
		}
	}

	public boolean isChanged() { return this.changed; }

	public void changedUpdate(DocumentEvent e) { documentChanged(); 	}
	public void insertUpdate(DocumentEvent e) { documentChanged(); }
	public void removeUpdate(DocumentEvent e) { documentChanged(); }

	private void documentChanged()
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
				firePropertyChange(propName, null, getText());
			}
		});
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
		//System.out.println(e.getOppositeComponent().getClass().getName());
		// When the popup menu for copy & paste is used, the oppositeComponent()
		// is the RootPane. In this case we don't want to chage the selection
		if (!(e.getOppositeComponent() instanceof javax.swing.JRootPane))
		{
			this.selectAll();
		}
	}

	/** Invoked when a component loses the keyboard focus.
	 *
	 */
	public void focusLost(FocusEvent e)
	{
		if (!this.immediateUpdate) this.applyChanges();
	}
}

