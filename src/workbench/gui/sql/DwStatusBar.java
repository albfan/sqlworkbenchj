/*
 * DwStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTextLabel;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;


/**
 *
 * @author  support@sql-workbench.net
 */
public class DwStatusBar 
	extends JPanel
	implements StatusBar
{
	private JTextField tfRowCount;

	private WbTextLabel tfStatus;
	
	private JTextField tfMaxRows;
	private String readyMsg;
	private JTextField tfTimeout;
	private WbTextLabel execTime;
	private JLabel timeoutLabel;
	private JPanel auxPanel;
	
	private static final int BAR_HEIGHT = 22;
	private static final int FIELD_HEIGHT = 18;
	private DecimalFormat numberFormatter;
	
	public DwStatusBar()
	{
		this(false);
	}
	
	public DwStatusBar(boolean showTimeout)
	{
		Dimension d = new Dimension(40, FIELD_HEIGHT);
		this.tfRowCount = new JTextField();
		this.tfMaxRows = new JTextField(6);
		this.tfMaxRows.setEditable(true);
		this.tfMaxRows.setMaximumSize(d);
		this.tfMaxRows.setMargin(new Insets(0, 2, 0, 2));
		this.tfMaxRows.setText("0");
		this.tfMaxRows.setName("maxrows");
		this.tfMaxRows.setToolTipText(ResourceMgr.getDescription("TxtMaxRows"));
		this.tfMaxRows.setHorizontalAlignment(SwingConstants.RIGHT);
		this.tfMaxRows.addMouseListener(new TextComponentMouseListener());

		Border b = BorderFactory.createCompoundBorder(new LineBorder(Color.LIGHT_GRAY, 1), new EmptyBorder(1,1,1,1));
		this.tfMaxRows.setBorder(b);

		this.setLayout(new BorderLayout());

		this.setMaximumSize(new Dimension(32768, BAR_HEIGHT));
		this.setMinimumSize(new Dimension(80, BAR_HEIGHT));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(JTextField.RIGHT);
		tfRowCount.setBorder(WbSwingUtilities.EMPTY_BORDER);
		tfRowCount.setDisabledTextColor(Color.BLACK);
		tfRowCount.setMargin(new Insets(0, 15, 0, 10));
		tfRowCount.setMinimumSize(d);
		tfRowCount.setPreferredSize(null);
		tfRowCount.setAutoscrolls(false);
		tfRowCount.setEnabled(false);
		
		this.tfStatus = new WbTextLabel();
		tfStatus.setMaximumSize(new Dimension(32768, FIELD_HEIGHT));
		tfStatus.setMinimumSize(new Dimension(80, FIELD_HEIGHT));
		tfStatus.setPreferredSize(null);
		
		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.EMPTY_BORDER);
		FlowLayout fl = new FlowLayout(FlowLayout.RIGHT);
		fl.setHgap(0);
		fl.setVgap(0);
		p.setLayout(fl);
		p.setMaximumSize(new Dimension(300, FIELD_HEIGHT));
		
		this.add(tfStatus, BorderLayout.CENTER);
		
		this.execTime = new WbTextLabel();
		execTime.setHorizontalAlignment(SwingConstants.RIGHT);
		this.execTime.setToolTipText(ResourceMgr.getString("MsgTotalSqlTime"));

		Font f = execTime.getFont();
		FontMetrics fm = execTime.getFontMetrics(f);
		Dimension pref = execTime.getPreferredSize();
		
		int width = fm.stringWidth("000000000000s");
		d = new Dimension(width + 4, FIELD_HEIGHT);
		execTime.setPreferredSize(d);
		execTime.setMaximumSize(d);
		
		b = new CompoundBorder(new DividerBorder(DividerBorder.LEFT_RIGHT), new EmptyBorder(0, 3, 0, 3));
		execTime.setBorder(b);	
		p.add(execTime);
		
		if (showTimeout)
		{
			JLabel l = new JLabel(" " + ResourceMgr.getString("LabelQueryTimeout") + " ");
			//l.setBorder(new DividerBorder(DividerBorder.LEFT));		
			p.add(l);
			this.tfTimeout = new JTextField(3);
			this.tfTimeout.setBorder(b);
			this.tfTimeout.setMargin(new Insets(0, 2, 0, 2));
			this.tfTimeout.setToolTipText(ResourceMgr.getDescription("LabelQueryTimeout"));
			this.tfTimeout.setHorizontalAlignment(SwingConstants.RIGHT);
			this.tfTimeout.addMouseListener(new TextComponentMouseListener());
			l.setToolTipText(this.tfTimeout.getToolTipText());
			p.add(this.tfTimeout);
		}
		JLabel l = new JLabel(" " + ResourceMgr.getString("LabelMaxRows") + " ");
//		if (!showTimeout)
//		{
//			l.setBorder(new DividerBorder(DividerBorder.LEFT));		
//		}
		l.setToolTipText(this.tfRowCount.getToolTipText());
		p.add(l);
		p.add(tfMaxRows);
		p.add(tfRowCount);
		this.add(p, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString(ResourceMgr.STAT_READY);
		this.clearStatusMessage();
		
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		String sep = Settings.getInstance().getDecimalSymbol();
		symb.setDecimalSeparator(sep.charAt(0));		
		numberFormatter = new DecimalFormat("0.#s", symb);
		numberFormatter.setMaximumFractionDigits(2);
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

	public void clearExecutionTime()
	{
		this.execTime.setText("");
		this.execTime.repaint();
	}
	
	public void setExecutionTime(long millis)
	{
		double time = (double)(millis/1000.0);
		this.execTime.setText(numberFormatter.format(time));
		this.execTime.repaint();
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
		this.doRepaint();
	}
	
	public void clearRowcount()
	{
		this.tfRowCount.setText("");
		this.doRepaint();
	}

	private void doRepaint()
	{
		this.invalidate();
		this.validate();
	}
	
	public String getText() { return this.tfStatus.getText(); }
	
	public void setStatusMessage(String aMsg)
	{
		this.tfStatus.setText(aMsg);
	}

	public void clearStatusMessage()
	{
		this.tfStatus.setText(this.readyMsg);
	}

	public void setQueryTimeout(int timeout)
	{
		if (this.tfTimeout != null)
		{
			this.tfTimeout.setText(Integer.toString(timeout));
		}
	}
	
	public int getQueryTimeout()
	{
		if (this.tfTimeout == null) return 0;
		return StringUtil.getIntValue(this.tfTimeout.getText(), 0);
	}

	public void setMaxRows(int max)
	{
		this.tfMaxRows.setText(Integer.toString(max));
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

}
