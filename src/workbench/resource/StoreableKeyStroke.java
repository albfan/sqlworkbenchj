/*
 * StoreableKeyStroke.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 * A class which wraps keyStroke and can be serialized 
 * using the XMLDecode/XMLEncoder classes
 * @author support@sql-workbench.net
 *
 */
public class StoreableKeyStroke
{
	private int keyCode;
	private int modifier;
	
	private boolean keyCodeSet = false;
	private boolean modifierSet = false;
	
	public StoreableKeyStroke()
	{
	}

	public StoreableKeyStroke(KeyStroke aKey)
	{
		this.keyCode = aKey.getKeyCode();
		this.modifier = aKey.getModifiers();
		this.modifierSet = true;
		this.keyCodeSet = true;
	}
	
	public KeyStroke getKeyStroke()
	{
		if (keyCodeSet || modifierSet)
		{
			return KeyStroke.getKeyStroke(this.keyCode, this.modifier);
		}
		return null;
	}
	
	public int getKeyCode()
	{
		KeyStroke theKey = getKeyStroke();
		if (theKey != null) return theKey.getKeyCode();
		return 0;
	}
	
	public void setKeyCode(int c)
	{
		this.keyCode = c;
		this.keyCodeSet = true;
	}
	
	public void setKeyModifier(int mod)
	{
		this.modifier = mod;
		this.modifierSet = true;
	}
	
	public int getKeyModifier()
	{
		KeyStroke theKey = getKeyStroke();
		if (theKey != null) return theKey.getModifiers();
		return 0;
	}

	public boolean equals(Object other)
	{
		KeyStroke thisKey = getKeyStroke();
		if (other instanceof StoreableKeyStroke)
		{
			KeyStroke otherKey = ((StoreableKeyStroke)other).getKeyStroke();
			if (thisKey == null && otherKey == null) return true;
			if (thisKey == null && otherKey != null) return false;
			if (thisKey != null && otherKey == null) return false;
			return thisKey.equals(otherKey);
		}
		else if (other instanceof KeyStroke)
		{
			if (thisKey == null && other == null) return true; 
			return thisKey.equals((KeyStroke)other);
		}
		return false;
	}

	public String toString()
	{
		KeyStroke thisKey = getKeyStroke();
		if (thisKey == null) return "";
		
		int mod = thisKey.getModifiers();
		int code = thisKey.getKeyCode();
		
		String modText = KeyEvent.getKeyModifiersText(mod);
		if (modText.length() == 0)
			return KeyEvent.getKeyText(code);
		else
			return  modText + "-" + KeyEvent.getKeyText(code);
	}
}
