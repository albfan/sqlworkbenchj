/*
 * BooleanPropertyEditor.java
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.swing.JCheckBox;

import workbench.interfaces.SimplePropertyEditor;
import workbench.log.LogMgr;


/**
 *
 * @author  support@sql-workbench.net
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
      this.changed = false;
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
  
  public boolean getImmediateUpdate()
  { return this.immediateUpdate; }
  
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
