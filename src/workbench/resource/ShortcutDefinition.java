/*
 * ShortcutDefinition.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import workbench.util.WbPersistence;

/**
 * @author info@sql-workbench.net
 *
 */
public class ShortcutDefinition
{
	private String actionClass;
	private StoreableKeyStroke currentKey;
	private StoreableKeyStroke defaultKey;
	private StoreableKeyStroke alternateKey;
	private boolean shortcutRemoved;
	
	public ShortcutDefinition()
	{
	}
	
	public boolean isMappedTo(KeyStroke key)
	{
		if (key == null) return false;
		KeyStroke mykey = this.getActiveKeyStroke();
		if (mykey != null)
		{
			return mykey.equals(key);
		}
		return false;
	}
	
	public ShortcutDefinition(String aClass)
	{
		this.setActionClass(aClass);
	}

	public void setShortcutRemoved(boolean aFlag)
	{
		this.shortcutRemoved = aFlag;
		if (aFlag)
		{
			this.clearKeyStroke();
		}
	}
	
	public boolean getShortcutRemoved() 
	{
		return this.shortcutRemoved;
	}
	
	/**
	 *	Clears the currently defined shortcut.
	 *	If a default shortcut is defined, this will make this Definition "customized"
	 *  as the default shortcut is "overwritten" with nothing
	 */
	public void clearKeyStroke()
	{
		this.currentKey = null;
	}

	/**
	 * Get the default (storeable) KeyStroke. 
	 * This is only here to statisfy the XMLEncoder, so that the whole thing can be saved 
	 */
	public KeyStroke getDefaultKeyStroke()
	{
		if (this.defaultKey != null) return this.defaultKey.getKeyStroke();
		return null;
	}
	
	/**
	 * Set the default (storeable) alternate KeyStroke. 
	 * This is only here to statisfy the XMLEncoder, so that the whole thing can be saved 
	 * @param aKey
	 */
	public void setAlternateKey(StoreableKeyStroke aKey)
	{
		this.alternateKey = aKey;
	}
	
	/**
	 * Get the default (storeable) alternate KeyStroke. 
	 * This is only here to statisfy the XMLEncoder, so that the whole thing can be saved 
	 */
	public KeyStroke getAlternateKeyStroke()
	{
		if (this.alternateKey != null) return this.alternateKey.getKeyStroke();
		return null;
	}
	
	/**
	 * Set the current/active (storeable) KeyStroke. 
	 * This is only here to statisfy the XMLEncoder, so that the whole thing can be saved 
	 * @param aKey
	 */
	public void setCurrentKey(StoreableKeyStroke aKey)
	{
		this.currentKey = aKey;
	}
	
	/**
	 * Get the current/active (storeable) KeyStroke. 
	 * This is only here to statisfy the XMLEncoder, so that the whole thing can be saved 
	 */
	public StoreableKeyStroke getCurrentKey()
	{
		return currentKey;
	}
	
	public KeyStroke getCurrentKeyStroke()
	{
		if (this.currentKey == null) return null;
		return this.currentKey.getKeyStroke();
	}

	/**
	 * Assign a KeyStroke to this action.
	 * @param aKey
	 */
	public void assignKey(KeyStroke aKey)
	{
		this.assignKey(new StoreableKeyStroke(aKey));
	}

	public void assignKey(StoreableKeyStroke aKey)
	{
		this.currentKey = aKey;
		this.shortcutRemoved = false;
	}
	
	/**
	 * Assign a default key for this action class.
	 * This method is called assign so that the XMLEncoder does not consider
	 * reading or writing this "property" as KeyStrokes cannot be serialized 
	 * to XML
	 * @param aKey
	 */
	public void assignDefaultKey(KeyStroke aKey)
	{
		this.defaultKey = new StoreableKeyStroke(aKey);
		//if (this.currentKey == null) this.currentKey = this.defaultKey;
	}

	/**
	 * Return the current default key. This is the "matching" 
	 * getter for the assignDefaultKey() method.
	 * @return KeyStroke
	 */
	public StoreableKeyStroke getDefaultKey()
	{
		return this.defaultKey;
	}
	
	
	/**
	 * Assign an alternate key for this action class.
	 * This method is called assign so that the XMLEncoder does not consider
	 * reading or writing this "property" as KeyStrokes cannot be serialized 
	 * to XML
	 * @param aKey
	 */
	public void assignAlternateKey(KeyStroke aKey)
	{
		this.alternateKey = new StoreableKeyStroke(aKey);
	}

	/**
	 * Return the defined alternate key as a KeyStroke.
	 * If no alternate key is define null is returned
	 * @return the alternate keystroke
	 */
	public StoreableKeyStroke getAlternateKey()
	{
		return this.alternateKey;
	}
	
	public boolean hasDefault() { return this.defaultKey != null; }
	
	/**
	 * Return if the this shortcut definition is customized.
	 * It's customized if a default exists, and currently no shortcut is defined.
	 * Or if a shortcut is defined, that is different to the default.
	 * @return true if this shortcut definition differs from the default.
	 */
	public boolean isCustomized() 
	{ 
		if (this.defaultKey == null && this.currentKey == null) return false;
		if (this.defaultKey == null && this.currentKey != null) return true;
		if (this.defaultKey != null && this.currentKey == null) return this.shortcutRemoved;
		
		return ( !this.currentKey.equals(this.defaultKey) );  
	}
	
	/**
	 * Restores the default mapping for this shortcut.
	 * After a call to resetToDefault() isCustomized() will return false
	 */
	public void resetToDefault()
	{
		this.shortcutRemoved = false;
		this.currentKey = null;
	}
	
	/**
	 * 	Returns the active KeyStroke.
	 * 	This is either the current or the default, or null if the shortcut 
	 *  has been removed completely. 
	 * @return the keystroke that is active
	 */
	public KeyStroke getActiveKeyStroke()
	{
		if (this.shortcutRemoved) return null;
		if (this.currentKey != null) return this.currentKey.getKeyStroke();
		if (this.defaultKey != null) return this.defaultKey.getKeyStroke();
		return null;
	}
	
	public StoreableKeyStroke getActiveKey()
	{
		if (this.shortcutRemoved) return null;
		if (this.currentKey != null) return this.currentKey;
		return this.defaultKey;
	}
	
	public String getActionClass() 
	{
		return this.actionClass;
	}
	
	public void setActionClass(String aClass)
	{
		if (aClass == null) throw new IllegalArgumentException("ClassName cannot be null");
		this.actionClass = aClass;
	}

	public String toString()
	{
		StringBuffer result = new StringBuffer(50);
		result.append(this.actionClass);
		StoreableKeyStroke active = (this.currentKey != null) ? this.currentKey : this.defaultKey;
		if (active != null)
		{	
			result.append(" [");
			result.append(active.toString());
			result.append("]");
		}
		else
		{
			result.append("[no shortcut]");
		}
		if(isCustomized())
			result.append(" (customized)");
		
		return result.toString();
	}
	
	public static void main(String[] args)
	{
		ShortcutDefinition def = new ShortcutDefinition();
		def.setActionClass(workbench.gui.actions.AddTabAction.class.getName());
		def.assignDefaultKey(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
		def.assignKey(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
		System.out.println("def before=" + def);
		WbPersistence.writeObject(def, "d:/temp/Shortcut.xml");
		
		//def = (ShortcutDefinition)WbPersistence.readObject("d:/temp/Shortcut.xml");
		//def.assignDefaultKey(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_MASK));
		//System.out.println("def after=" + def);
	}

}
