/*
 * WbButton.java
 *
 * Created on March 29, 2003, 1:30 AM
 */

package workbench.gui.components;

import javax.swing.Action;
import javax.swing.JButton;

/**
 *
 * @author  thomas
 */
public class WbButton
	extends JButton
{
	
	public WbButton()
	{
		super();
	}
	
	public WbButton(Action a)
	{
		super(a);
	}
	public WbButton(String aText)
	{
		super(aText);
	}
	
	public void setText(String newText)
	{
		int pos = newText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = newText.charAt(pos + 1);
			newText = newText.substring(0, pos) + newText.substring(pos + 1);
			this.setMnemonic((int)mnemonic);
			super.setText(newText);
		}
		else
		{
			super.setText(newText);
		}
	}
}


