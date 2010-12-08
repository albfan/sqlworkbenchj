/*
 * DwStatusBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

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

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.NotifierWindow;
import workbench.gui.components.SelectionDisplay;
import workbench.gui.components.TextComponentMouseListener;
import workbench.gui.components.WbTextLabel;
import workbench.interfaces.EditorStatusbar;
import workbench.interfaces.EventDisplay;
import workbench.interfaces.StatusBar;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.DurationFormatter;
import workbench.util.EventNotifier;
import workbench.util.NotifierEvent;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;
import workbench.util.WbThread;


/**
 *
 * @author  Thomas Kellerer
 */
public class DwStatusBar
	extends JPanel
	implements StatusBar, EditorStatusbar, ActionListener, EventDisplay, MouseListener
{
	private JTextField tfRowCount;

	protected WbTextLabel tfStatus;

	private JTextField tfMaxRows;
	private String readyMsg;
	private JTextField tfTimeout;
	private WbTextLabel execTime;
	private JLabel editorStatus;
	private JPanel alertPanel;
	private JPanel infoPanel;

	public static final int BAR_HEIGHT = 22;
	private static final int FIELD_HEIGHT = 18;
	private static final Border MAX_ROWS_BORDER = new EmptyBorder(0, 0, 0, 1);
	private static final Insets MAX_ROWS_INSETS = new Insets(0, 2, 0, 2);

	private int timerInterval = Settings.getInstance().getIntProperty("workbench.gui.execution.timer.interval", 1000);
	private final boolean showTimer = Settings.getInstance().getBoolProperty("workbench.gui.execution.timer.enabled", true);
	private long timerStarted;
	private Timer executionTimer;
	private boolean timerRunning;
	private ActionListener notificationHandler;
	private JLabel notificationLabel;
	private String editorLinePrefix;
	private String editorColPrefix;
	private SelectionDisplay selectionDisplay;

	private DurationFormatter durationFormatter = new DurationFormatter();

	public static final Border DEFAULT_BORDER = new CompoundBorder(new EmptyBorder(2, 1, 0, 1), BorderFactory.createEtchedBorder());

	public DwStatusBar()
	{
		this(false, false);
	}

	public DwStatusBar(boolean showTimeout, boolean showEditorStatus)
	{
		super();
		Dimension d = new Dimension(40, FIELD_HEIGHT);
		this.tfRowCount = new JTextField();
		this.tfMaxRows = new JTextField(6);
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

		this.setLayout(new BorderLayout());

		this.setMaximumSize(new Dimension(32768, BAR_HEIGHT));
		this.setMinimumSize(new Dimension(80, BAR_HEIGHT));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(JTextField.RIGHT);
		tfRowCount.setBorder(MAX_ROWS_BORDER);
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

		this.add(tfStatus, BorderLayout.CENTER);

		infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0,0));
		infoPanel.setBorder(WbSwingUtilities.EMPTY_BORDER);
		infoPanel.setMaximumSize(new Dimension(300, FIELD_HEIGHT));

		setBorder(DEFAULT_BORDER);

		execTime = new WbTextLabel();
		execTime.setHorizontalAlignment(SwingConstants.RIGHT);
		execTime.setToolTipText(ResourceMgr.getString("MsgTotalSqlTime"));

		Font f = execTime.getFont();
		FontMetrics fm = null;
		if (f != null) fm = execTime.getFontMetrics(f);

		if (showTimer)
		{
			this.executionTimer = new Timer(timerInterval, this);
		}

		if (showEditorStatus)
		{
			this.editorStatus = new JLabel();
			this.editorStatus.setHorizontalAlignment(SwingConstants.CENTER);
			int ew = (fm == null ? 85 : fm.stringWidth("L:999 C:999"));
			d = new Dimension(ew + 4, FIELD_HEIGHT);
			editorStatus.setMinimumSize(d);
			this.editorStatus.setBorder(new CompoundBorder(new DividerBorder(DividerBorder.LEFT), new EmptyBorder(0, 3, 0, 3)));
			this.editorStatus.setToolTipText(ResourceMgr.getDescription("LblEditorStatus"));
			infoPanel.add(editorStatus);
			this.editorColPrefix = ResourceMgr.getString("LblEditorPosCol");
			this.editorLinePrefix = ResourceMgr.getString("LblEditorPosLine");
		}

		b = new CompoundBorder(new DividerBorder(DividerBorder.LEFT_RIGHT), new EmptyBorder(0, 3, 0, 3));
		int width = (fm == null ? 100 : fm.stringWidth("000000000000s"));
		d = new Dimension(width + 4, FIELD_HEIGHT);
		execTime.setPreferredSize(d);
		execTime.setMaximumSize(d);
		execTime.setBorder(b);
		infoPanel.add(execTime);

		if (showTimeout)
		{
			JLabel l = new JLabel(" " + ResourceMgr.getString("LblQueryTimeout") + " ");
			//l.setBorder(new DividerBorder(DividerBorder.LEFT));
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
		JLabel l = new JLabel(" " + ResourceMgr.getString("LblMaxRows") + " ");
		l.setToolTipText(this.tfRowCount.getToolTipText());
		infoPanel.add(l);
		infoPanel.add(tfMaxRows);
		infoPanel.add(tfRowCount);
		this.add(infoPanel, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString("MsgReady");
		this.clearStatusMessage();

		EventNotifier.getInstance().addEventDisplay(this);
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

	public void actionPerformed(ActionEvent e)
	{
		if (!timerRunning) return;
		long time = System.currentTimeMillis() - timerStarted;
		execTime.setText(durationFormatter.formatDuration(time, false));
	}

	public void setExecutionTime(long millis)
	{
		if (timerRunning) executionEnd();
		boolean includeFraction = (millis < DurationFormatter.ONE_MINUTE);
		execTime.setText(durationFormatter.formatDuration(millis, includeFraction));
		execTime.repaint();
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
		tfRowCount.setText(s.toString());
		refresh();
	}

	private Runnable refresher = new Runnable()
	{
		public void run()
		{
			validate();
			repaint();
		}
	};

	protected void refresh()
	{
		EventQueue.invokeLater(refresher);
	}

	public void clearRowcount()
	{
		tfRowCount.setText("");
		refresh();
	}

	public String getText()
	{
		return this.tfStatus.getText();
	}

	@Override
	public void setStatusMessage(final String message, int duration)
	{
		setStatusMessage(message);
		WbThread t = new WbThread("Notification")
		{
			public void run()
			{
				WbThread.sleepSilently(2500);
				String m = getText();
				if (message.equals(m)) clearStatusMessage();
			}
		};
		t.start();
	}

	/**
	 *	Show a message in the status panel.
	 *
	 *	This method might be called from within a background thread, so we
	 *  need to make sure the actual setText() stuff is called on the AWT
	 *  thread in order to update the GUI correctly.
	 *  @see DwStatusBar#setStatusMessage(String)
	 */
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

	public void showAlert(final NotifierEvent evt)
	{
		if (this.notificationHandler != null)
		{
			this.removeAlert();
		}

		if (this.alertPanel == null)
		{
			alertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
			infoPanel.add(alertPanel, 0);
		}
		else
		{
			alertPanel.removeAll();
		}

		notificationHandler = evt.getHandler();
		notificationLabel = new JLabel(ResourceMgr.getImageByName(evt.getIconKey()));
		notificationLabel.setText(null);
		notificationLabel.setToolTipText(evt.getTooltip());
		notificationLabel.setIconTextGap(0);
		notificationLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		notificationLabel.addMouseListener(this);

		final Frame f = WbManager.getInstance().getCurrentWindow();

		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				alertPanel.add(notificationLabel);
				NotifierWindow w = new NotifierWindow(f, evt.getTooltip());
				w.show(notificationLabel);
				validate();
			}
		});
	}

	public void removeAlert()
	{
		if (alertPanel != null)
		{
			notificationLabel.removeMouseListener(this);
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					infoPanel.remove(alertPanel);
					alertPanel.removeAll();
					notificationHandler = null;
					validate();
				}
			});
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() != this.notificationLabel) return;
		if (this.notificationHandler == null) return;

		if (e.getButton() == MouseEvent.BUTTON1)
		{
			ActionEvent evt = new ActionEvent(this, -1, "notifierClicked");
			this.notificationHandler.actionPerformed(evt);
		}
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
