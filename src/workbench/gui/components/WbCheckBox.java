/*
 * WbCheckBox.java
 *
 * Created on November 15, 2003, 2:35 PM
 */

package workbench.gui.components;

import javax.swing.JCheckBox;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbCheckBox
	extends JCheckBox
{
	
	/** Creates a new instance of WbCheckBox */
	public WbCheckBox()
	{
	}
	
	public WbCheckBox(String text)
	{
		super(text);
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
