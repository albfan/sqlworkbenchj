package workbench.gui.components;

import javax.swing.JButton;
import javax.swing.plaf.SplitPaneUI;

import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class WbSplitPaneDivider
	extends BasicSplitPaneDivider
{
	private String oneTouchTooltip;
	
	public WbSplitPaneDivider(BasicSplitPaneUI ui)
	{
		super(ui);
	}
	
	public void setOneTouchTooltip(String tip)
	{
		this.oneTouchTooltip = tip;
		this.updateTooltip();
	}
	
	protected JButton createLeftOneTouchButton()	
	{
		JButton b = super.createLeftOneTouchButton();
		if (this.oneTouchTooltip != null)
		{
			b.setToolTipText(this.oneTouchTooltip);
		}
		return b;
	}
	
	protected JButton createRightOneTouchButton()
	{
		JButton b = super.createRightOneTouchButton();
		if (this.oneTouchTooltip != null)
		{
			b.setToolTipText(this.oneTouchTooltip);
		}
		return b;
	}
	
	private void updateTooltip()
	{
		if (this.leftButton != null)
		{
			this.leftButton.setToolTipText(this.oneTouchTooltip);
		}
		if (this.rightButton != null)
		{
			this.rightButton.setToolTipText(this.oneTouchTooltip);
		}
	}
}