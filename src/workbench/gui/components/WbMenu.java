/*
 * Created on 11. August 2002, 15:02
 */
package workbench.gui.components;

import java.awt.Dimension;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 *
 * @author  workbench@kellerer.org
 */
public class WbMenu
	extends JMenu
{

	public WbMenu()
	{
		super();
	}
	
	public WbMenu(String aText)
	{
		super(aText);
	}

	public WbMenu(String aText, boolean b)
	{
		super(aText, b);
	}
	
	public WbMenu(Action anAction)
	{
		super(anAction);
	}

	public void setText(String aText)
	{
		int pos = aText.indexOf('&');
		if (pos > -1)
		{
			char mnemonic = aText.charAt(pos + 1);
			aText = aText.substring(0, pos) + aText.substring(pos + 1);
			this.setMnemonic(mnemonic);
		}
		super.setText(aText);
	}	
	
}
