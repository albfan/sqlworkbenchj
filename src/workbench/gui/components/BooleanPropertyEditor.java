/*
 * Created on 28. August 2002, 23:40
 */
package workbench.gui.components;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.JCheckBox;
import workbench.interfaces.SimplePropertyEditor;


/**
 *
 * @author  workbench@kellerer.org
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
	private boolean immediateUpdate = false;
	
	public BooleanPropertyEditor()
	{
		super();
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
			try
			{
				this.getter = cls.getMethod(name, null);
			}
			catch (NoSuchMethodException e)
			{
				this.getter = null;
			}
			if (this.getter == null)
			{
				name = "is" + propertyName;
				this.getter = cls.getMethod(name, null);
			}

			name = "set" + propertyName;
			Class[] parms = {boolean.class};
			this.setter = cls.getMethod(name, parms);
			
			Boolean value = (Boolean)this.getter.invoke(this.source, null);
			this.setSelected(value.booleanValue());
			this.addItemListener(this);
		}
		catch (Exception e)
		{
			System.out.println("Error on init");
			e.printStackTrace();
		}
	}
	
	public void itemStateChanged(ItemEvent evt)
	{
		this.changed = true;
		if (this.immediateUpdate)
		{
			this.applyChanges();
		}
		firePropertyChanged();
	}
	
	public void applyChanges()
	{
		if (!this.changed) return;
		Object args[] = new Object[1];
		args[0] = new Boolean(this.isSelected());
		try
		{
			this.setter.invoke(this.source, args);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public boolean isChanged()
	{
		return this.changed;
	}

	private ArrayList propListeners;
		public void addPropertyChangeListener(PropertyChangeListener l)
	{
		if (this.propListeners == null) this.propListeners = new ArrayList();
		this.propListeners.add(l);
	}

  public void setImmediateUpdate(boolean aFlag)
  {
    this.immediateUpdate = aFlag;
  }
  
  public boolean getImmediateUpdate() { return this.immediateUpdate; }
  
	public void removePropertyChangeListener(PropertyChangeListener l)
	{
		if (this.propListeners == null) return;
		this.propListeners.remove(l);
	}
	
	public void firePropertyChanged()
	{
		if (this.propListeners == null) return;
		PropertyChangeEvent evt = null;
		for (int i=0; i < this.propListeners.size(); i++)
		{
			PropertyChangeListener l = (PropertyChangeListener)this.propListeners.get(i);
			if (l == null) continue;
			if (evt == null)
			{
				evt = new PropertyChangeEvent(this, this.propName, null, null);
			}
			l.propertyChange(evt);
		}
	}
	
}
