/*
 * FeedbackWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.gui.WbSwingUtilities;

import workbench.util.StringUtil;
import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class FeedbackWindow
	extends JDialog
{
	private JLabel connectLabel;

	public FeedbackWindow(Frame owner, String msg)
	{
		super(owner, false);
		initComponents(msg);
	}

	public FeedbackWindow(Dialog owner, String msg)
	{
		super(owner, true);
		initComponents(msg);
	}

	private void initComponents(String msg)
	{
		JPanel p = new JPanel();
    Color c = p.getBackground().darker();
    p.setBorder(new CompoundBorder(new LineBorder(c, 1), new EmptyBorder(15, 20, 15, 20)));
		p.setLayout(new BorderLayout(0, 0));
		p.setMinimumSize(new Dimension(350, 50));
		connectLabel = new JLabel(msg);
		connectLabel.setMinimumSize(new Dimension(300, 50));
		connectLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(connectLabel, BorderLayout.CENTER);
		setUndecorated(true);
    getRootPane().setWindowDecorationStyle(JRootPane.NONE);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.CENTER);
		pack();
	}

	public void showAndStart(final Runnable task)
	{
		EventQueue.invokeLater(() ->
    {
      WbThread t = new WbThread(task, "FeedbackWindow");
      t.start();
      setVisible(true);
    });
	}

	public String getMessage()
	{
		return connectLabel.getText();
	}

	public void setMessage(String msg)
	{
		if (StringUtil.isBlank(msg))
		{
			connectLabel.setText("");
		}
		else
		{
			connectLabel.setText(msg);
		}
		pack();
	}

	public void forceRepaint()
	{
		WbSwingUtilities.invoke(() ->
    {
      doLayout();
      invalidate();
      validate();
      repaint();
    });
	}

}
