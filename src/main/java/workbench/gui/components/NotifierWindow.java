/*
 * NotifierWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class NotifierWindow
	extends JDialog
	implements MouseListener
{
	public NotifierWindow(Frame owner, String msg)
	{
		super(owner, false);
		init(msg);
	}

	public NotifierWindow(JDialog owner, String msg)
	{
		super(owner, false);
		init(msg);
	}

	private void init(String msg)
	{
		setUndecorated(true);
		setFocusable(false);
		setLayout(new BorderLayout());

		Font ttFont = UIManager.getFont("ToolTip.font");
		float size = (float)(ttFont.getSize() * 1.2);
		Font textFont = ttFont.deriveFont(size);
		Color textColor = UIManager.getColor("ToolTip.foreground");
		Color backgroundColor = UIManager.getColor("ToolTip.background");

		setBackground(backgroundColor);

		JTextArea text = new JTextArea(msg);
		text.setFont(textFont);
		text.setEnabled(false);
		text.setEditable(false);
		text.setForeground(textColor);
		text.setDisabledTextColor(textColor);
		text.setBackground(backgroundColor);
		text.setBorder(new EmptyBorder(10,10,10,10));
		text.addMouseListener(this);
		text.setCursor(Cursor.getDefaultCursor());

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
			@Override
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

	@Override
	public void mouseClicked(MouseEvent e)
	{
		closeWindow();
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

}
