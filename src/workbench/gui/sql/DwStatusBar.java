/*
 * DwStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import workbench.interfaces.EditorStatusbar;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.SelectionDisplay;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTextLabel;

import workbench.util.DurationFormatter;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbThread;


/**
 *
 * @author  Thomas Kellerer
 */
public class DwStatusBar
	extends JPanel
	implements StatusBar, EditorStatusbar, ActionListener
{
	private final JTextField tfRowCount;

	protected WbTextLabel tfStatus;

	private final JTextField tfMaxRows;
	private String readyMsg;
	private JTextField tfTimeout;
	private final WbTextLabel execTime;
	private JLabel editorStatus;
	private JLabel maxRowsLabel;
	private final JPanel infoPanel;

	private static final int DEFAULT_FIELD_HEIGHT = 18;
	private static final Border MAX_ROWS_BORDER = new EmptyBorder(0, 0, 0, 1);
	private static final Insets MAX_ROWS_INSETS = new Insets(0, 2, 0, 2);

	private final int timerInterval = Settings.getInstance().getIntProperty("workbench.gui.execution.timer.interval", 1000);
	private final boolean showTimer = Settings.getInstance().getBoolProperty("workbench.gui.execution.timer.enabled", true);
	private long timerStarted;
	private Timer executionTimer;
	private boolean timerRunning;
	private String editorLinePrefix;
	private String editorColPrefix;
	private SelectionDisplay selectionDisplay;

	private final DurationFormatter durationFormatter = new DurationFormatter();

	public static final Border DEFAULT_BORDER = new CompoundBorder(new EmptyBorder(2, 1, 0, 1), BorderFactory.createEtchedBorder());

	public DwStatusBar(boolean showTimeout, boolean showEditorStatus)
	{
		super(new BorderLayout());

		this.tfRowCount = new JTextField();
		this.tfMaxRows = new JTextField(6);

		Font f = tfMaxRows.getFont();
		FontMetrics fm = null;
		if (f != null) fm = tfMaxRows.getFontMetrics(f);

		int fieldHeight = (fm == null ? 0 : fm.getHeight() + 2);
		fieldHeight = Math.min(DEFAULT_FIELD_HEIGHT, fieldHeight);
		int barHeight = fieldHeight + 4;
		Dimension d = new Dimension(40, fieldHeight);

		this.tfMaxRows.setEditable(true);
		this.tfMaxRows.setMaximumSize(d);
		this.tfMaxRows.setMargin(MAX_ROWS_INSETS);
		this.tfMaxRows.setText("0");
		this.tfMaxRows.setName("maxrows");
		this.tfMaxRows.setToolTipText(ResourceMgr.getDescription("TxtMaxRows"));
		this.tfMaxRows.setHorizontalAlignment(SwingConstants.RIGHT);
		this.tfMaxRows.addMouseListener(new TextComponentMouseListener());

		Border b = BorderFactory.createCompoundBorder(new LineBorder(Color.LIGHT_GRAY, 1), new EmptyBorder(1,1,1,1));
		this.tfMaxRows.setBorder(b);

		this.setMaximumSize(new Dimension(32768, barHeight));
		this.setMinimumSize(new Dimension(80, barHeight));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(JTextField.RIGHT);
		tfRowCount.setBorder(MAX_ROWS_BORDER);
		tfRowCount.setDisabledTextColor(tfRowCount.getForeground());
		tfRowCount.setMargin(new Insets(0, 15, 0, 10));
		tfRowCount.setMinimumSize(d);
		tfRowCount.setPreferredSize(null);
		tfRowCount.setAutoscrolls(false);
		tfRowCount.setEnabled(false);

		this.tfStatus = new WbTextLabel();

		this.add(tfStatus, BorderLayout.CENTER);

		infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
		infoPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		infoPanel.setMaximumSize(new Dimension(300, fieldHeight));

		setBorder(DEFAULT_BORDER);

		execTime = new WbTextLabel();
		execTime.setMininumCharacters(8);
		execTime.setHorizontalAlignment(SwingConstants.RIGHT);
		execTime.setToolTipText(ResourceMgr.getString("MsgTotalSqlTime"));

		if (showTimer)
		{
			this.executionTimer = new Timer(timerInterval, this);
		}

		if (showEditorStatus)
		{
			this.editorStatus = new JLabel();
			this.editorStatus.setHorizontalAlignment(SwingConstants.CENTER);
			int ew = (fm == null ? 85 : fm.stringWidth("L:999 C:999"));
			d = new Dimension(ew + 4, fieldHeight);
			editorStatus.setMinimumSize(d);
			this.editorStatus.setBorder(new CompoundBorder(new DividerBorder(DividerBorder.LEFT), new EmptyBorder(0, 3, 0, 3)));
			this.editorStatus.setToolTipText(ResourceMgr.getDescription("LblEditorStatus"));
			infoPanel.add(editorStatus);
			this.editorColPrefix = ResourceMgr.getString("LblEditorPosCol");
			this.editorLinePrefix = ResourceMgr.getString("LblEditorPosLine");
		}

		b = new CompoundBorder(new DividerBorder(DividerBorder.LEFT_RIGHT), new EmptyBorder(0, 3, 0, 3));
		execTime.setBorder(b);
		infoPanel.add(execTime);

		if (showTimeout)
		{
			JLabel l = new JLabel(" " + ResourceMgr.getString("LblQueryTimeout") + " ");
			infoPanel.add(l);
			this.tfTimeout = new JTextField(3);
			this.tfTimeout.setBorder(b);
			this.tfTimeout.setMargin(new Insets(0, 2, 0, 2));
			this.tfTimeout.setToolTipText(ResourceMgr.getDescription("LblQueryTimeout"));
			this.tfTimeout.setHorizontalAlignment(SwingConstants.RIGHT);
			this.tfTimeout.addMouseListener(new TextComponentMouseListener());
			l.setToolTipText(this.tfTimeout.getToolTipText());
			infoPanel.add(this.tfTimeout);
		}

		maxRowsLabel = new JLabel(" " + ResourceMgr.getString("LblMaxRows") + " ");
		maxRowsLabel.setToolTipText(this.tfRowCount.getToolTipText());
		infoPanel.add(maxRowsLabel);
		infoPanel.add(tfMaxRows);
		infoPanel.add(tfRowCount);
		this.add(infoPanel, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString("MsgReady");
		this.clearStatusMessage();
	}

	public void removeMaxRows()
	{
		if (this.tfMaxRows != null)
		{
			infoPanel.remove(tfMaxRows);
			infoPanel.remove(maxRowsLabel);
		}
	}

	public void removeSelectionIndicator(JTable client)
	{
		if (this.selectionDisplay == null) return;
		this.selectionDisplay.removeClient(client);
	}

	public void showSelectionIndicator(JTable client)
	{
		if (this.selectionDisplay == null)
		{
			this.selectionDisplay = new SelectionDisplay();
		}
		this.selectionDisplay.setClient(client);
		this.infoPanel.add(this.selectionDisplay, 0);
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
		execTime.setText("");
		execTime.repaint();
	}

	@Override
	public void setEditorLocation(int line, int column)
	{
		 if (this.editorStatus == null) return;
		 StringBuilder text = new StringBuilder(20);
		 text.append(editorLinePrefix);
		 text.append(NumberStringCache.getNumberString(line));
		 text.append(' ');
		 text.append(editorColPrefix);
		 text.append(NumberStringCache.getNumberString(column));
		 this.editorStatus.setText(text.toString());
	}

	public void executionStart()
	{
		if (!showTimer) return;
		timerStarted = System.currentTimeMillis();
		executionTimer.setInitialDelay(timerInterval);
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

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (!timerRunning) return;
		long time = System.currentTimeMillis() - timerStarted;
		setExecTimeText(durationFormatter.formatDuration(time, false));
	}

	public void setExecutionTime(long millis)
	{
		if (timerRunning) executionEnd();
		setExecTimeText(durationFormatter.formatDuration(millis));
	}

	private void setExecTimeText(final String text)
	{
		EventQueue.invokeLater(() ->
    {
      execTime.setText(text);
    });
	}

	public void setRowcount(int start, int end, int count)
	{
		final StringBuilder s = new StringBuilder(20);
		if (count > 0)
		{
			// for some reason the layout manager does not leave enough
			// space to the left of the text, so we'll add some space here
			s.append(' ');
			s.append(NumberStringCache.getNumberString(start));
			s.append('-');
			s.append(NumberStringCache.getNumberString(end));
			s.append('/');
			s.append(NumberStringCache.getNumberString(count));
		}
		setRowcountText(s.toString());
	}

	private void setRowcountText(final String text)
	{
		EventQueue.invokeLater(() ->
    {
      tfRowCount.setText(text);
      validate();
    });
	}

	public void clearRowcount()
	{
		setRowcountText("");
	}

	@Override
	public String getText()
	{
		return tfStatus.getText();
	}

	@Override
	public void setStatusMessage(final String message, final int duration)
	{
		setStatusMessage(message);
		if (duration > 0)
		{
			WbThread t = new WbThread("ClearStatusMessage")
			{
				@Override
				public void run()
				{
					WbThread.sleepSilently(duration);
					String m = getText();
					if (message.equals(m)) clearStatusMessage();
				}
			};
			t.start();
		}
	}

	/**
	 *	Display the status message
	 *
	 */
	@Override
	public void setStatusMessage(final String aMsg)
	{
		if (aMsg == null) return;
		tfStatus.setText(aMsg);
	}

	public void forcePaint()
	{
		tfStatus.forcePaint();
	}

	/**
	 * Clears the status bar by displaying the default message.
	 */
	@Override
	public final void clearStatusMessage()
	{
		this.setStatusMessage(this.readyMsg);
	}

	public void setQueryTimeout(int timeout)
	{
		if (this.tfTimeout != null)
		{
			this.tfTimeout.setText(NumberStringCache.getNumberString(timeout));
		}
	}

	public int getQueryTimeout()
	{
		if (this.tfTimeout == null) return 0;
		return StringUtil.getIntValue(this.tfTimeout.getText(), 0);
	}

	public void setMaxRows(int max)
	{
		this.tfMaxRows.setText(NumberStringCache.getNumberString(max));
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

	@Override
	public void doRepaint()
	{
		this.forcePaint();
	}
}
