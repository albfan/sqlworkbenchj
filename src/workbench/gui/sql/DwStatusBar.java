/*
 * DwStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;


/**
 *
 * @author  info@sql-workbench.net
 */
public class DwStatusBar extends JPanel
{
	private JTextField tfRowCount;

	private JTextField tfStatus;
	JTextField tfMaxRows;
	private String readyMsg;

	private static final int BAR_HEIGHT = 22;
	private static final int FIELD_HEIGHT = 18;

	public DwStatusBar()
	{
		Dimension d = new Dimension(40, FIELD_HEIGHT);
		this.tfRowCount = new JTextField();
		this.tfStatus = new JTextField();
		this.tfMaxRows = new JTextField(6);
		this.tfMaxRows.setEditable(true);
		this.tfMaxRows.setMaximumSize(d);
		this.tfMaxRows.setMargin(new Insets(0, 2, 0, 2));
		this.tfMaxRows.setText("0");
		this.tfMaxRows.setToolTipText(ResourceMgr.getDescription("TxtMaxRows"));
		this.tfMaxRows.setHorizontalAlignment(SwingConstants.RIGHT);
		this.tfMaxRows.addMouseListener(new TextComponentMouseListener());

		Border b = BorderFactory.createCompoundBorder(new LineBorder(Color.LIGHT_GRAY, 1), new EmptyBorder(1,1,1,1));
		this.tfMaxRows.setBorder(b);
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.EMPTY_BORDER);
		FlowLayout fl = new FlowLayout(FlowLayout.RIGHT);
		fl.setHgap(0);
		fl.setVgap(0);
		p.setLayout(fl);
		p.setMaximumSize(new Dimension(300, FIELD_HEIGHT));

		this.setLayout(new BorderLayout());

		this.setMaximumSize(new Dimension(32768, BAR_HEIGHT));
		this.setMinimumSize(new Dimension(80, BAR_HEIGHT));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(JTextField.RIGHT);
		tfRowCount.setBorder(WbSwingUtilities.EMPTY_BORDER);
		tfRowCount.setDisabledTextColor(Color.BLACK);
		tfRowCount.setMargin(new Insets(0, 15, 0, 10));
		//tfRowCount.setMaximumSize(new java.awt.Dimension(32768, FIELD_HEIGHT));
		tfRowCount.setMinimumSize(d);
		tfRowCount.setPreferredSize(null);
		tfRowCount.setAutoscrolls(false);
		tfRowCount.setEnabled(false);

		tfStatus.setHorizontalAlignment(JTextField.LEFT);
		tfStatus.setBorder(new DividerBorder(DividerBorder.RIGHT));
		tfStatus.setDisabledTextColor(Color.BLACK);
		tfStatus.setMaximumSize(new Dimension(32768, FIELD_HEIGHT));
		tfStatus.setMinimumSize(new Dimension(80, FIELD_HEIGHT));
		tfStatus.setPreferredSize(null);
		tfStatus.setAutoscrolls(false);
		tfStatus.setEnabled(false);
		tfStatus.setEditable(false);

		this.add(tfStatus, BorderLayout.CENTER);
		JLabel l = new JLabel(" " + ResourceMgr.getString("LabelMaxRows") + " ");
		p.add(l);
		p.add(tfMaxRows);
		p.add(tfRowCount);
		this.add(p, BorderLayout.EAST);
		//this.add(tfMaxRows, BorderLayout.EAST);
		//this.add(tfRowCount, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString(ResourceMgr.STAT_READY);
		this.clearStatusMessage();
	}

	public void setReadyMsg(String aMsg)
	{
		if (aMsg == null)
		{
			this.readyMsg = StringUtil.EMPTY_STRING;
		}
		else
		{
			this.readyMsg = aMsg;
		}
	}

	public void setRowcount(int start, int end, int count)
	{
		StringBuffer s = new StringBuffer(20);
		if (count > 0)
		{
			// for some reason the dynamic layout does not leave enough
			// space to the left of the text, so we'll add some space here
			s.append(" ");
			s.append(start);
			s.append("-");
			s.append(end);
			s.append("/");
			s.append(count);
		}
		this.tfRowCount.setText(s.toString());
		this.doLayout();
		this.updateUI();
		this.doRepaint();
	}

	public void setMaxRows(int max)
	{
		this.tfMaxRows.setText(Integer.toString(max));
		this.doRepaint();
	}

	public void clearRowcount()
	{
		this.tfRowCount.setText("");
		this.doRepaint();
	}

	public void setStatusMessage(String aMsg)
	{
		if (aMsg == null || aMsg.length() == 0)
		{
			this.clearStatusMessage();
		}
		else
		{
			this.tfStatus.setText(aMsg);
			this.doRepaint();
		}
	}

	public void clearStatusMessage()
	{
		this.tfStatus.setText(this.readyMsg);
		this.doRepaint();
	}

	public int getMaxRows()
	{
		if (this.tfMaxRows == null) return 0;
		return StringUtil.getIntValue(this.tfMaxRows.getText(), 0);
	}

  public void selectMaxRowsField()
  {
		this.tfMaxRows.selectAll();
    this.tfMaxRows.requestFocusInWindow();
  }

	private void doRepaint()
	{
		this.repaint();
	}

}
