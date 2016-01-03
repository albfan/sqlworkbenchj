/*
 * ListValuePicker.java
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
package workbench.gui.filter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.Types;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import workbench.WbManager;
import workbench.interfaces.ValidatingComponent;
import workbench.interfaces.ValueProvider;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;


/**
 *
 * @author Thomas Kellerer
 */
public class ListValuePicker
	extends JPanel
	implements KeyListener, ValidatingComponent, MouseListener, ActionListener
{
	private JTextField filterValue;
	private WbScrollPane scroll;
	private ValidatingDialog dialog;
	private WbTable lookupData;
	private Object currentValue;

	public ListValuePicker(Collection<String> listData, String columnName, Object current)
	{
		super(new GridBagLayout());

		currentValue = current;

		filterValue = new JTextField();
		filterValue.addKeyListener(this);
		filterValue.addActionListener(this);

		lookupData = new WbTable(false, false, false);
		lookupData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lookupData.setReadOnly(true);

		lookupData.setRendererSetup(new RendererSetup(false));
		lookupData.addMouseListener(this);
		lookupData.setColumnSelectionAllowed(false);
		lookupData.setRowSelectionAllowed(true);

		DataStore ds = new DataStore(new String[] {columnName}, new int[] { Types.VARCHAR });
		for (String value : listData)
		{
			int row = ds.addRow();
			ds.setValue(row, 0, value);
		}
		DataStoreTableModel model = new DataStoreTableModel(ds);
		model.setAllowEditing(false);
		lookupData.setModel(model, true);

		Action nextComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentAfter(ListValuePicker.this, lookupData);
				next.requestFocusInWindow();
			}
		};

		Action prevComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentBefore(ListValuePicker.this, lookupData);
				next.requestFocusInWindow();
			}
		};

		InputMap im = lookupData.getInputMap(JComponent.WHEN_FOCUSED);
		im.put(WbSwingUtilities.TAB, "picker-next-comp");
		im.put(WbSwingUtilities.SHIFT_TAB, "picker-prev-comp");

		Action selectValue = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				selectValue();
			}
		};
		lookupData.configureEnterKeyAction(selectValue);
		lookupData.getActionMap().put("picker-next-comp", nextComponent);
		lookupData.getActionMap().put("picker-prev-comp", prevComponent);

		scroll = new WbScrollPane(lookupData);

		JPanel edit = new JPanel(new BorderLayout(0, 0));
		WbLabel lbl = new WbLabel();
		lbl.setTextByKey("LblFkFilterValue", false);
		lbl.setLabelFor(filterValue);
		lbl.setBorder(new EmptyBorder(0, 0, 0, 10));
		edit.add(lbl, BorderLayout.WEST);
		edit.add(filterValue, BorderLayout.CENTER);

		GridBagConstraints gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		add(edit, gc);

		gc.gridy++;
		gc.weighty = 1.0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(10,0,2,0);
		add(scroll, gc);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(filterValue);
		pol.addComponent(lookupData);
		setFocusCycleRoot(true);
		lookupData.setFocusCycleRoot(false);
		setFocusTraversalPolicy(pol);
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.filterValue)
		{
			applyFilter();
		}
	}

	private void findCurrentValue()
	{
		if (currentValue == null) return;
		boolean selected = false;
		int rows = lookupData.getRowCount();
		for (int row = 0; row < rows; row++)
		{
			Object pk = lookupData.getValueAt(row, 0);
			if (currentValue.equals(pk))
			{
				lookupData.getSelectionModel().setSelectionInterval(row, row);
				selected = true;
				break;
			}
		}
		if (!selected)
		{
			lookupData.getSelectionModel().setSelectionInterval(0, 0);
		}
	}

	protected String getSelectedValue()
	{
		int row = lookupData.getSelectedRow();
		if (row < 0) return null;
		return lookupData.getValueAsString(row, 0);
	}

	protected void resetFilter()
	{
		lookupData.resetFilter();
	}

	protected void applyFilter()
	{
		ContainsComparator comp = new ContainsComparator();
		DataRowExpression filter = new DataRowExpression(comp, filterValue.getText());
		filter.setIgnoreCase(true);
		lookupData.applyFilter(filter);
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
				{
					resetFilter();
				}
				else
				{
					applyFilter();
				}
			}
		});
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public boolean validateInput()
	{
		return lookupData.getSelectedRowCount() == 1;
	}

	@Override
	public void componentDisplayed()
	{
		findCurrentValue();
		lookupData.requestFocus();
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	public void selectValue()
	{
		dialog.approveAndClose();
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			selectValue();
		}
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

	public static String pickValue(final JComponent parent, final String column, final ValueProvider data, final String current)
	{
		try
		{
			WbSwingUtilities.showWaitCursor(parent);
			Collection<String> values = data.getColumnValues(column);

			ListValuePicker picker = new ListValuePicker(values, column, current);
			Window callerWindow = SwingUtilities.getWindowAncestor(parent);

			String title = ResourceMgr.getString("TxtSelectValue");
			String key = "workbench.gui.listvaluepicker";

			ValidatingDialog dialog = null;
			if (callerWindow instanceof JDialog)
			{
				dialog = new ValidatingDialog((JDialog)callerWindow, title, picker);
			}
			else if (callerWindow instanceof JFrame)
			{
				dialog = new ValidatingDialog((JFrame)callerWindow, title, picker);
			}

			picker.dialog = dialog;
			if (!Settings.getInstance().restoreWindowSize(dialog, key))
			{
				dialog.setSize(250,300);
			}
			WbSwingUtilities.center(dialog, WbManager.getInstance().getCurrentWindow());
			dialog.setVisible(true);

			Settings.getInstance().storeWindowSize(dialog, key);

			if (dialog.isCancelled())
			{
				return null;
			}
			return picker.getSelectedValue();
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(parent);
		}
	}

}
