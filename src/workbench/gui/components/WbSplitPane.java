/*
 * Created on 7. August 2002, 20:33
 */
package workbench.gui.components;

import java.awt.Component;

import javax.swing.JSplitPane;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

import workbench.gui.WbSwingUtilities;

/**
 * A JSplitPane which restores the divider size after a UI Change
 * and an updateUI()
 * @author  workbench@kellerer.org
 */
public class WbSplitPane
	extends JSplitPane
{
	public int DEFAULT_DIVIDER_SIZE = 7;
	public WbSplitPane()
	{
		super();
		this.initDefaults();
	}
	
	public WbSplitPane(int orientation)
	{
		super(orientation);
		this.initDefaults();
	}
	
	public WbSplitPane(int newOrientation, boolean newContinuousLayout) 	
	{
		super(newOrientation, newContinuousLayout);
		this.initDefaults();
	}
	
	public WbSplitPane(int newOrientation, boolean newContinuousLayout, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newContinuousLayout, newLeftComponent, newRightComponent);
		this.initDefaults();
	}
	
	public WbSplitPane(int newOrientation, Component newLeftComponent, Component newRightComponent)
	{
		super(newOrientation, newLeftComponent, newRightComponent);
		this.initDefaults();
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

	private void initDefaults()
	{
		this.setDividerSize(DEFAULT_DIVIDER_SIZE);
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		//this.setDividerBorder(WbSwingUtilities.EMPTY_BORDER);
		this.setContinuousLayout(true);
	}
	
	public Border getDividerBorder()
	{
		Border result = null;
		try
		{		
			int divider = this.getDividerSize();
			BasicSplitPaneUI ui = (BasicSplitPaneUI)this.getUI();
			BasicSplitPaneDivider div = ui.getDivider();
			result = div.getBorder();
		}
		catch (Exception e)
		{
			result = null;
		}
		return result;
	}	
	public void setDividerBorder(Border newBorder)
	{
		try
		{		
			int divider = this.getDividerSize();
			BasicSplitPaneUI ui = (BasicSplitPaneUI)this.getUI();
			BasicSplitPaneDivider div = ui.getDivider();
			div.setBorder(newBorder);
			this.setDividerSize(divider);
		}
		catch (Exception e)
		{
		}
	}
}

