/*
 * Created on 06.12.2003
 * 
 */
package workbench.resource;

import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

/**
 * A class which wraps keyStroke and can be serialized 
 * using the XMLDecode/XMLEncoder classes
 * @author workbench@kellerer.org
 *
 */
public class StoreableKeyStroke
{
	private KeyStroke key = null;
	private int keyCode;
	private int modifier;
	
	private boolean keyCodeSet = false;
	private boolean modifierSet = false;
	
	public StoreableKeyStroke()
	{
	}

	public StoreableKeyStroke(KeyStroke aKey)
	{
		this.key = aKey;
	}
	
	public KeyStroke getKeyStroke()
	{
		return this.key;
	}
	
	public int getKeyCode()
	{
		if (this.key != null) return this.key.getKeyCode();
		return 0;
	}
	
	public void setKeyCode(int c)
	{
		this.keyCode = c;
		this.keyCodeSet = true;
		this.createKeyStroke();
	}
	
	private void createKeyStroke()
	{
		if (this.keyCodeSet && this.modifierSet)
		{	
			this.key = KeyStroke.getKeyStroke(this.keyCode, this.modifier);
		}
	}
	
	public void setKeyModifier(int mod)
	{
		this.modifier = mod;
		this.modifierSet = true;
		this.createKeyStroke();
	}
	
	public int getKeyModifier()
	{
		if (this.key != null) return this.key.getModifiers();
		return 0;
	}

	public boolean equals(Object other)
	{
		if (other != null && other instanceof StoreableKeyStroke)
		{
			KeyStroke otherKey = ((StoreableKeyStroke)other).key;
			if (this.key == null && otherKey == null) return true;
			if (this.key == null && otherKey != null) return false;
			if (this.key != null && otherKey == null) return false;
			return this.key.equals(otherKey);
		}
		else if (other instanceof KeyStroke)
		{
			if (this.key == null && other == null) return true; 
			return this.key.equals((KeyStroke)other);
		}
		return false;
	}

	public String toString()
	{
		if (this.key == null) return "";
		
		int modifier = this.key.getModifiers();
		int code = this.key.getKeyCode();
		
		if (modifier == 0)
			return KeyEvent.getKeyText(code);
		else
			return KeyEvent.getKeyModifiersText(modifier) + "-" + KeyEvent.getKeyText(code);
	}
}
