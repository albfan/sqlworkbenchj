package workbench.gui.components;

import javax.swing.JToolTip;

public class MultiLineToolTip
	extends JToolTip
{
	private static final MultiLineToolTipUI SHARED_UI = new MultiLineToolTipUI();
	
	public MultiLineToolTip()
	{
    setOpaque(true);
		updateUI();
	}
	
	public void updateUI()
	{
		setUI(SHARED_UI);
	}
	
}

