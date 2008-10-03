/*
 * NotifierWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class NotifierWindow
	extends JWindow
	implements MouseListener
{
	public NotifierWindow(String msg)
	{
		super();
		setFocusable(false);
		
		setLayout(new BorderLayout());

		Font textFont = UIManager.getFont("ToolTip.font");
		Color textColor = UIManager.getColor("ToolTip.foreground");
		Color backgroundColor = UIManager.getColor("ToolTip.background");

		setBackground(backgroundColor);

		JTextArea text = new JTextArea(msg);
		text.setFont(textFont);
		text.setForeground(textColor);
		text.setBackground(backgroundColor);
		text.setBorder(new EmptyBorder(10,10,10,10));
		text.addMouseListener(this);

		add(text, BorderLayout.CENTER);
		
		getRootPane().setBorder(new LineBorder(Color.GRAY, 1));
		pack();
	}

	public void show(JComponent where)
	{
		if (where == null) return;
		if (!where.isShowing()) return;
		
		Point p = where.getLocationOnScreen();

		setLocation((int) p.getX() - 10, (int) p.getY() - this.getHeight() - 10);
		setVisible(true);

		TimerTask task = new TimerTask()
		{
			public void run()
			{
				closeWindow();
			}
		};
		long period = Settings.getInstance().getIntProperty("workbench.gui.updatecheck.tooltiptime", 15000);
		Timer timer = new Timer(true);
		timer.schedule(task, period);
	}

	protected void closeWindow()
	{
		setVisible(false);
		dispose();
	}
	public void mouseClicked(MouseEvent e)
	{
		closeWindow();
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}
}
