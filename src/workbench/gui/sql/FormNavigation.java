/*
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * Copyright 2002-2008, Thomas Kellerer
 * 
 * No part of this code maybe reused without the permission of the author
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 * 
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class FormNavigation
	extends JPanel
	implements ActionListener, AdjustmentListener
{
	private RecordFormPanel display;
	private JTextField currentRow = new JTextField(4);
	private JScrollBar scrollBar;

	public FormNavigation(RecordFormPanel panel)
	{
		super(new BorderLayout());
		this.display = panel;
		setBorder(new EmptyBorder(10,0,0,0));
		scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, display.getCurrentRow(), 1, 0, display.getRowCount());
		scrollBar.setBlockIncrement(10);
		scrollBar.addAdjustmentListener(this);

		JPanel status = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5,0,0,5);
		status.add(new JLabel(ResourceMgr.getString("TxtRow")), c);

		c.gridx++;
		status.add(currentRow, c);
		c.gridx++;
		c.weightx = 1.0;
		status.add(new JLabel(ResourceMgr.getString("TxtOf") + " " + display.getRowCount()), c);

		this.add(status, BorderLayout.SOUTH);
		this.add(scrollBar, BorderLayout.NORTH);
		currentRow.addActionListener(this);
		updateStatus();
	}

	public void actionPerformed(ActionEvent e)
	{
		int newRow = StringUtil.getIntValue(currentRow.getText(), -1);
		if (newRow > 0 && newRow <= display.getRowCount())
		{
			if (changeRow(newRow - 1))
			{
				try
				{
					scrollBar.setValueIsAdjusting(true);
					scrollBar.setValue(display.getCurrentRow());
				}
				finally
				{
					scrollBar.setValueIsAdjusting(false);
				}
			}
		}
		updateStatus();
	}

	private boolean changeRow(int newRow)
	{
		if (display.validateInput())
		{
			display.showRecord(newRow);
			return true;
		}
		return false;
	}
	
	private void updateStatus()
	{
		currentRow.setText(Integer.toString(display.getCurrentRow() + 1));
	}

	public void adjustmentValueChanged(AdjustmentEvent e)
	{
		int newRow = e.getValue();
		int current = display.getCurrentRow();
		if (changeRow(newRow))
		{
			updateStatus();
		}
		else
		{
			try
			{
				scrollBar.setValueIsAdjusting(true);
				scrollBar.setValue(current);
			}
			finally
			{
				scrollBar.setValueIsAdjusting(false);
			}
		}
	}


}
