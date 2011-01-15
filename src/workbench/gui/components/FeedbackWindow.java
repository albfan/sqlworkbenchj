/*
 * FeedbackWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
		p.setBorder(new CompoundBorder(WbSwingUtilities.getBevelBorderRaised(), new EmptyBorder(15, 20, 15, 20)));
		p.setLayout(new BorderLayout(0, 0));
		p.setMinimumSize(new Dimension(250, 50));
		connectLabel = new JLabel(msg);
		connectLabel.setMinimumSize(new Dimension(200, 50));
		connectLabel.setHorizontalAlignment(SwingConstants.CENTER);
		p.add(connectLabel, BorderLayout.CENTER);
		setUndecorated(true);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(p, BorderLayout.CENTER);
		pack();
	}

	public void showAndStart(final Runnable task)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				WbThread t = new WbThread(task, "FeedbackWindow");
				t.start();
				setVisible(true);
			}
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
		WbSwingUtilities.repaintNow(connectLabel);
	}

}
