/*
 * WbTabbedPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Insets;
import javax.swing.JTabbedPane;
import javax.swing.JToolTip;
import javax.swing.UIManager;
import javax.swing.plaf.TabbedPaneUI;
import workbench.gui.WbSwingUtilities;
import workbench.log.LogMgr;


/**
 *
 * @author  support@sql-workbench.net
 */
public class WbTabbedPane
	extends JTabbedPane
{

	private boolean suspendRepaint = false;

	/** Creates a new instance of WbTabbedPane */
	public WbTabbedPane()
	{
		super();
		init();
	}

	public int getTabHeight()
	{
		Font font = getFont();
		if (font == null) return 0;
		FontMetrics metrics = getFontMetrics(font);
		if (metrics == null) return 0;
		int fontHeight = metrics.getHeight();
    Insets tabInsets = UIManager.getInsets("TabbedPane.tabInsets");
		fontHeight += tabInsets.top + tabInsets.bottom + 2;
		return fontHeight + 5;
	}
	
	public WbTabbedPane(int placement)
	{
		super(placement);
		init();
	}
	
	public JToolTip createToolTip()
	{
		JToolTip tip = new MultiLineToolTip();
		tip.setComponent(this);
		return tip;
	}
	
	private void init()
	{
		// For use with the jGoodies Plastic look & feel
		this.putClientProperty("jgoodies.noContentBorder", Boolean.TRUE);
		try
		{
			TabbedPaneUI ui = TabbedPaneUIFactory.getBorderLessUI();
			if (ui != null) this.setUI(ui);
		}
		catch (Exception e)
		{
			LogMgr.logError("WbTabbedPane.init()", "Error during init",e);
		}
		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
	}

	public Insets getInsets()
	{
		return new Insets(0,0,0,0);
	}
	
	boolean stateChangedPending = false;
	protected void fireStateChanged()
	{
		if (!isRepaintSuspended())
		{
			super.fireStateChanged();
		}
		else
		{
			stateChangedPending = true;
		}
	}

	public synchronized void setSuspendRepaint(boolean suspendNow)
	{
		boolean wasSuspended = this.suspendRepaint;
		this.suspendRepaint = suspendNow;

		// if repainting was re-enabled, then queue
		// a repaint event right away
		// I'm using invokeLater() to make sure, that
		// this is executed on the AWT thread.
		if (wasSuspended && !suspendNow)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					validate();
					repaint();
					if (stateChangedPending)
					{
						stateChangedPending = false;
						fireStateChanged();
					}
				}
			});
		}
	}

	public boolean isRepaintSuspended()
	{
		 return this.suspendRepaint;
	}

	public void repaint()
	{
		if (this.suspendRepaint) return;
		super.repaint();
	}

	public void paintComponent(Graphics g)
	{
		if (this.suspendRepaint) return;
		super.paintComponent(g);
	}

	public void paintComponents(Graphics g)
	{
		if (this.suspendRepaint) return;
		super.paintComponents(g);
	}
}
