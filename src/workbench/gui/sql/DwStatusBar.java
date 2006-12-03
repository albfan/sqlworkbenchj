/*
 * DwStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTextLabel;
import workbench.interfaces.EditorStatusbar;
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
	implements StatusBar, EditorStatusbar, ActionListener
{
	private JTextField tfRowCount;

	protected WbTextLabel tfStatus;
	
	private JTextField tfMaxRows;
	private String readyMsg;
	private JTextField tfTimeout;
	private WbTextLabel execTime;
	private JLabel editorStatus;
	
	private static final int BAR_HEIGHT = 22;
	private static final int FIELD_HEIGHT = 18;
	private DecimalFormat numberFormatter;
	private SimpleDateFormat timeFormatter = new SimpleDateFormat("m'm' ss's'");

	private int timerInterval = Settings.getInstance().getIntProperty("workbench.gui.execution.timer.interval", 1000);
	private int timerDelay = Settings.getInstance().getIntProperty("workbench.gui.execution.timer.interval", 1000);
	private final boolean showTimer = Settings.getInstance().getBoolProperty("workbench.gui.execution.timer.enabled", true);
	private long timerStarted;
	private Timer executionTimer;
	private boolean timerRunning;
	
	public DwStatusBar()
	{
		this(false, false);
	}
	
	public DwStatusBar(boolean showTimeout, boolean showEditorStatus)
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
		
		if (showTimer)
		{
			this.executionTimer = new Timer(timerInterval, this);
		}

		if (showEditorStatus)
		{
			this.editorStatus = new JLabel();
			this.editorStatus.setHorizontalAlignment(SwingConstants.CENTER);
			int ew = fm.stringWidth("L:999 C:999");
			d = new Dimension(ew + 4, FIELD_HEIGHT);
			editorStatus.setMinimumSize(d);
			this.editorStatus.setBorder(new CompoundBorder(new DividerBorder(DividerBorder.LEFT), new EmptyBorder(0, 3, 0, 3)));
			this.editorStatus.setToolTipText(ResourceMgr.getDescription("LblEditorStatus"));
			p.add(editorStatus);
		}
		
		b = new CompoundBorder(new DividerBorder(DividerBorder.LEFT_RIGHT), new EmptyBorder(0, 3, 0, 3));
		int width = fm.stringWidth("000000000000s");
		d = new Dimension(width + 4, FIELD_HEIGHT);
		execTime.setPreferredSize(d);
		execTime.setMaximumSize(d);
		execTime.setBorder(b);	
		p.add(execTime);
		
		if (showTimeout)
		{
			JLabel l = new JLabel(" " + ResourceMgr.getString("LblQueryTimeout") + " ");
			//l.setBorder(new DividerBorder(DividerBorder.LEFT));		
			p.add(l);
			this.tfTimeout = new JTextField(3);
			this.tfTimeout.setBorder(b);
			this.tfTimeout.setMargin(new Insets(0, 2, 0, 2));
			this.tfTimeout.setToolTipText(ResourceMgr.getDescription("LblQueryTimeout"));
			this.tfTimeout.setHorizontalAlignment(SwingConstants.RIGHT);
			this.tfTimeout.addMouseListener(new TextComponentMouseListener());
			l.setToolTipText(this.tfTimeout.getToolTipText());
			p.add(this.tfTimeout);
		}
		JLabel l = new JLabel(" " + ResourceMgr.getString("LblMaxRows") + " ");
		l.setToolTipText(this.tfRowCount.getToolTipText());
		p.add(l);
		p.add(tfMaxRows);
		p.add(tfRowCount);
		this.add(p, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString("MsgReady");
		this.clearStatusMessage();
		
		numberFormatter = DwStatusBar.createTimingFormatter(); 
	}

	public static final DecimalFormat createTimingFormatter()
	{
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		String sep = Settings.getInstance().getProperty("workbench.gui.timining.decimal", ".");
		symb.setDecimalSeparator(sep.charAt(0));		
		DecimalFormat numberFormatter = new DecimalFormat("0.#s", symb);
		numberFormatter.setMaximumFractionDigits(2);
		return numberFormatter;
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

	public void setEditorLocation(int line, int column)
	{
		 if (this.editorStatus == null) return;
		 StringBuilder text = new StringBuilder(20);
		 text.append("L:");
		 text.append(line);
		 text.append(" C:");
		 text.append(column);
		 this.editorStatus.setText(text.toString());
	}
	
	public void executionStart()
	{
		if (!showTimer) return;
		timerStarted = System.currentTimeMillis();
		executionTimer.setInitialDelay(timerDelay);
		executionTimer.setDelay(timerInterval);
		timerRunning = true;
		executionTimer.start();	
	}
	
	public void executionEnd()
	{
		if (!showTimer) return;
		timerRunning = false;
		executionTimer.stop();		
	}

	private String formatDuration(long millis)
	{
		if (millis < 1000)
			return "0s";
		else if (millis <= 60000)
			return Long.toString((long)(millis / 1000)) + "s";
		else
			return timeFormatter.format(new java.util.Date(millis));
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (!timerRunning) return;
		long time = System.currentTimeMillis() - timerStarted;
		this.execTime.setText(formatDuration(time));
	}
	
	public void setExecutionTime(long millis)
	{
		if (timerRunning) executionEnd();
		if (millis <= 60000)
		{
			double time = (double)(millis/1000.0);
			this.execTime.setText(numberFormatter.format(time));
		}
		else
		{
			this.execTime.setText(timeFormatter.format(new java.util.Date(millis)));
		}
		this.execTime.repaint();
	}
	
	public void setRowcount(int start, int end, int count)
	{
		StringBuilder s = new StringBuilder(20);
		if (count > 0)
		{
			// for some reason the dynamic layout does not leave enough
			// space to the left of the text, so we'll add some space here
			s.append(' ');
			s.append(start);
			s.append('-');
			s.append(end);
			s.append('/');
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

	/**
	 *	Show a message in the status panel.
	 *	This method might be called from within a background thread, so we
	 *  need to make sure the actual setText() stuff is called on the AWT
	 *  thread in order to update the GUI correctly.
	 *  @see DwStatusBar#setStatusMessage(String)
	 */
	public void setStatusMessage(final String aMsg)
	{
		if (aMsg == null) return;
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				tfStatus.setText(aMsg);
			}
		});
	}
	
	/**
	 * Clears the status bar by displaying the default message.
	 */
	public void clearStatusMessage()
	{
		this.setStatusMessage(this.readyMsg);
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
