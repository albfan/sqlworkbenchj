/*
 * Created on 28. August 2002, 23:40
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import workbench.interfaces.SimplePropertyEditor;

/**
 *
 * @author  workbench@kellerer.org
 */
public class NumberPropertyEditor 
	extends NumberField
	implements DocumentListener, SimplePropertyEditor
{
	private Object source;
	private Method setter;
	private Method getter;
	private String propName;
	private boolean changed;
	
	public NumberPropertyEditor()
	{
		super();
	}
	
	public void setSourceObject(Object aSource, String aProperty)
	{
		this.source = aSource;
		this.changed = false;
		this.propName = aProperty;
		String propertyName = Character.toUpperCase(aProperty.charAt(0)) + aProperty.substring(1);
		Document doc = this.getDocument();
		if (doc != null) doc.removeDocumentListener(this);
		try
		{
			String name = "get" + propertyName;
			Class cls = aSource.getClass();
			this.getter = cls.getMethod(name, null);

			name = "set" + propertyName;
			Class[] parms = {int.class};
			this.setter = cls.getMethod(name, parms);
			
			String value = this.getter.invoke(this.source, null).toString();
			super.setText(value);
		}
		catch (Exception e)
		{
			System.out.println("Error on init");
			e.printStackTrace();
		}
		doc = this.getDocument();
		if (doc != null) doc.addDocumentListener(this);
	}
	
	public void applyChanges()
	{
		if (this.setter == null) return;
		if (!this.changed) return;
		
		Object args[] = new Object[1];
		args[0] = this.getText();
		try
		{
			this.setter.invoke(this.source, args);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void changedUpdate(DocumentEvent e)
	{
		this.changed = true;
	}
	
	public void insertUpdate(DocumentEvent e)
	{
		this.changed = true;
	}
	
	public void removeUpdate(DocumentEvent e)
	{
		this.changed = true;
	}
	
	public boolean isChanged()
	{
		return this.changed;
	}

}

