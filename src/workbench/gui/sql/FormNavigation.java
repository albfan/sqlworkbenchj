/*
 * FormNavigation.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import workbench.console.DataStorePrinter;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.components.WbButton;

import workbench.storage.DataStore;
import workbench.storage.RowData;

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class FormNavigation
	extends JPanel
	implements ActionListener, AdjustmentListener
{
	private RecordFormPanel display;
	private JTextField currentRow = new JTextField(4);
	private JScrollBar scrollBar;
	private JButton copyButton;

  public FormNavigation(RecordFormPanel panel)
  {
    super(new BorderLayout());
    this.display = panel;
    int size = IconMgr.getInstance().getSizeForLabel();
    setBorder(new EmptyBorder(size, 0, size, 0));
    scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, display.getCurrentRow(), 1, 0, display.getRowCount());
    scrollBar.setBlockIncrement(10);
    scrollBar.addAdjustmentListener(this);

    JPanel status = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.WEST;
    c.insets = new Insets(5, 0, 0, 5);
    status.add(new JLabel(ResourceMgr.getString("TxtRow")), c);

    c.gridx++;
    status.add(currentRow, c);
    c.gridx++;
    c.weightx = 1.0;
    status.add(new JLabel(ResourceMgr.getString("TxtOf") + " " + display.getRowCount()), c);

    copyButton = new WbButton(ResourceMgr.getString("MnuTxtCopy"));
    c.gridx++;
    c.anchor = GridBagConstraints.EAST;
    c.weightx = 1.0;
    c.insets = new Insets(5, 0, 0, 0);
    status.add(copyButton, c);
    copyButton.addActionListener(this);

    this.add(status, BorderLayout.SOUTH);
    this.add(scrollBar, BorderLayout.NORTH);
    currentRow.addActionListener(this);
    updateStatus();
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == this)
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

    if (e.getSource() == copyButton)
    {
      copyToClipboard();
    }
  }

  private void copyToClipboard()
  {
    DataStore ds = display.getDataStore();
    DataStorePrinter printer = new DataStorePrinter(ds);
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    RowData row = ds.getRow(display.getCurrentRow());
    printer.printAsRecord(pw, row, display.getCurrentRow());

    Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
    StringSelection sel = new StringSelection(writer.toString());
    clp.setContents(sel, sel);
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

	@Override
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
