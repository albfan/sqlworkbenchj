/*
 * Created on 7. August 2002, 20:33
 */
package workbench.gui.components;

import java.awt.Component;
import javax.swing.JSplitPane;
import javax.swing.plaf.ComponentUI;

/**
 * A JSplitPane which restores the divider size after a UI Change
 * and an updateUI()
 * @author  workbench@kellerer.org
 */
public class WbSplitPane
	extends JSplitPane
{
	
	public WbSplitPane()
	{
		super();
	}
	
	public WbSplitPane(int orientation)
	{
		super(orientation);
	}
	
	public WbSplitPane(int newOrientation, boolean newContinuousLayout) 	
	{
		super(newOrientation, newContinuousLayout);
	}
	
	public WbSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
	}
	public WbSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newLeftComponent, newRightComponent);
	}

	public void updateUI()
	{
		int divider = this.getDividerSize();
		super.updateUI();
		this.setDividerSize(divider);
	}
	
	public void setUI(ComponentUI newUI)
	{
		int divider = this.getDividerSize();
		super.setUI(newUI);
		this.setDividerSize(divider);
	}
	
}
