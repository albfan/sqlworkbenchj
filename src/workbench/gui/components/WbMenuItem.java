/*
 * Created on 11. August 2002, 15:02
 */
package workbench.gui.components;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbMenuItem 
	extends JMenuItem
{

	public WbMenuItem()
	{
		super();
	}
	
	public WbMenuItem(String aText)
	{
		super(aText);
	}

	public WbMenuItem(Action anAction)
	{
		super(anAction);
	}

	public WbMenuItem(String text, int mnemonic) 	
	{
		super(text, mnemonic);
	}
	
	public WbMenuItem(Icon icon) 
	{
		super(icon);
	}
	public WbMenuItem(String text, Icon icon) 
	{
		super(text, icon);
	}
	
	public void setText(String aText)
	{
		int pos = aText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = aText.charAt(pos + 1);
			aText = aText.substring(0, pos) + aText.substring(pos + 1);
			this.setMnemonic((int)mnemonic);
		}
		super.setText(aText);
	}	
}
