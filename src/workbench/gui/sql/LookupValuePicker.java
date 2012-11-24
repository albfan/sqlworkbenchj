/*
 * LookupValuePicker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FocusTraversalPolicy;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import workbench.interfaces.Reloadable;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ResultSetter;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.ReloadAction;
import workbench.gui.components.DataStoreTableModel;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.FlatButton;
import workbench.gui.components.NumberField;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.LookupDataLoader;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;

import workbench.util.WbThread;

/**
 *
 * @author Thomas Kellerer
 */
public class LookupValuePicker
	extends JPanel
	implements KeyListener, ValidatingComponent, MouseListener, Restoreable, Reloadable, ActionListener
{
	private JTextField filterValue;
	private JRadioButton doFilter;
	private JRadioButton doSearch;
	private ButtonGroup buttonGroup;
	private NumberField maxRows;
	private WbTable lookupData;
	private LookupDataLoader lookupLoader;
	private WbScrollPane scroll;
	private JPanel statusPanel;
	private JLabel statusBar;
	private JLabel rowCount;
	private WbConnection dbConnection;
	private ValidatingDialog dialog;
	private Object currentValue;

	public LookupValuePicker(WbConnection conn, LookupDataLoader loader, Object current)
	{
		super(new GridBagLayout());

		lookupLoader = loader;
		dbConnection = conn;
		currentValue = current;

		filterValue = new JTextField();
		filterValue.addKeyListener(this);
		filterValue.addActionListener(this);
		filterValue.setToolTipText(ResourceMgr.getString("d_LblFkFilterValue"));

		lookupData = new WbTable(false, false, false);
		lookupData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		lookupData.setReadOnly(true);
		lookupData.setRendererSetup(new RendererSetup(false));
		lookupData.addMouseListener(this);
		lookupData.setColumnSelectionAllowed(false);
		lookupData.setRowSelectionAllowed(true);

		Action nextComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentAfter(LookupValuePicker.this, lookupData);
				next.requestFocusInWindow();
			}
		};

		Action prevComponent = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				FocusTraversalPolicy policy = getFocusTraversalPolicy();
				Component next = policy.getComponentBefore(LookupValuePicker.this, lookupData);
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

		JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		doFilter = new JRadioButton(ResourceMgr.getString("LblFkPickerFilter"));
		doFilter.setToolTipText(ResourceMgr.getDescription("LblFkPickerFilter"));
		doFilter.setBorder(new EmptyBorder(0, 0, 0, 10));
		doSearch = new JRadioButton(ResourceMgr.getString("LblFkPickerSelect"));
		doSearch.setToolTipText(ResourceMgr.getDescription("LblFkPickerSelect"));
		buttonGroup = new ButtonGroup();
		buttonGroup.add(doFilter);
		buttonGroup.add(doSearch);

		boolean useFilter = Settings.getInstance().getBoolProperty("workbench.gui.lookupvaluepicker.usefilter", true);
		if (useFilter)
		{
			doFilter.setSelected(true);
		}
		else
		{
			doSearch.setSelected(true);
		}

		radios.add(doFilter);
		radios.add(doSearch);

		JPanel edit = new JPanel(new BorderLayout(0, 0));
		WbLabel lbl = new WbLabel();
		lbl.setTextByKey("LblFkFilterValue");
		lbl.setLabelFor(filterValue);
		lbl.setBorder(new EmptyBorder(0, 0, 0, 10));
		edit.add(lbl, BorderLayout.WEST);
		edit.add(filterValue, BorderLayout.CENTER);
		edit.add(radios, BorderLayout.PAGE_START);

		FlatButton reload = new FlatButton(new ReloadAction(this));
		reload.setText(null);
		reload.setMargin(WbToolbarButton.MARGIN);
		edit.add(reload, BorderLayout.LINE_END);

		statusPanel = createStatusPanel();

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

		gc.gridy++;
		gc.weighty = 0.0;
		gc.insets = new Insets(0,0,10,0);
		gc.fill = GridBagConstraints.HORIZONTAL;
		add(statusPanel, gc);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(filterValue);
		pol.addComponent(lookupData);
		pol.addComponent(maxRows);
		setFocusCycleRoot(true);
		lookupData.setFocusCycleRoot(false);
		setFocusTraversalPolicy(pol);
	}

	private JPanel createStatusPanel()
	{
		JPanel result = new JPanel(new GridBagLayout());

		Insets empty = new Insets(0,0,0,0);
		Insets small = new Insets(0,2,0,2);
		GridBagConstraints gc = new GridBagConstraints();

		statusBar = new JLabel();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.anchor = GridBagConstraints.FIRST_LINE_START;
		gc.fill = GridBagConstraints.HORIZONTAL;
		gc.weightx = 1.0;
		gc.weighty = 0.0;
		result.add(statusBar, gc);

		rowCount = new JLabel("   ");
		rowCount.setIconTextGap(2);
		rowCount.setBorder(new DividerBorder(DividerBorder.LEFT));
		gc.insets = small;
		gc.weightx = 0.0;
		gc.gridx ++;
		result.add(rowCount, gc);

		JLabel l = new JLabel(" " + ResourceMgr.getString("LblMaxRows") + " ");
		l.setBorder(new DividerBorder(DividerBorder.LEFT));
		gc.insets = small;
		gc.gridx ++;
		result.add(l, gc);

		maxRows = new NumberField(6);
		WbSwingUtilities.setMinimumSize(maxRows, 6);
		gc.insets = empty;
		gc.gridx ++;
		result.add(maxRows, gc);

		Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1,5,1,1));
		result.setBorder(b);

		return result;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.filterValue)
		{
			if (doFilter.isSelected())
			{
				applyFilter();
			}
			else if (doSearch.isSelected())
			{
				loadData(false);
			}
		}
	}

	private void findCurrentValue(boolean selectRow)
	{
		if (currentValue == null) return;

		String colName = this.lookupLoader.getReferencedColumn();
		int colIndex = lookupData.getDataStore().getColumnIndex(colName);
		if (colIndex < 0)
		{
			LogMgr.logError("LookupValuePicker.findCurrentValue()", "Referenced column " + colName + " not found!", null);
			return;
		}

		if (selectRow)
		{
			int rows = lookupData.getRowCount();
			for (int row = 0; row < rows; row++)
			{
				Object pk = lookupData.getValueAt(row, colIndex);
				if (currentValue.equals(pk))
				{
					lookupData.getSelectionModel().setSelectionInterval(row, row);
					break;
				}
			}
		}
		ColumnComparator comp = getComparator();
		ColumnExpression search = new ColumnExpression(colName, comp, currentValue);
		lookupData.applyHighlightExpression(search);
	}

	private ColumnComparator getComparator()
	{
		return new ColumnComparator()
		{
			@Override
			public String getOperator()
			{
				return "=";
			}

			@Override
			public String getDescription()
			{
				return "equals";
			}

			@Override
			public boolean evaluate(Object reference, Object value, boolean ignoreCase)
			{
				if (reference == null && value == null) return true;
				if (reference == null || value == null) return false;
				return value.equals(reference);
			}

			@Override
			public boolean supportsType(Class valueClass)
			{
				return true;
			}

			@Override
			public boolean supportsIgnoreCase()
			{
				return false;
			}

			@Override
			public boolean needsValue()
			{
				return true;
			}

			@Override
			public String getValueExpression(Object value)
			{
				return value == null ? "" : value.toString();
			}

			@Override
			public boolean validateInput(Object input)
			{
				return true;
			}

			@Override
			public boolean comparesEquality()
			{
				return true;
			}
		};
	}

	@Override
	public void reload()
	{
		loadData(false);
	}

	@Override
	public void restoreSettings()
	{
		int rows = Settings.getInstance().getIntProperty("workbench.gui.lookupvalue.picker.maxrows", 150);
		maxRows.setText(Integer.toString(rows));
	}

	@Override
	public void saveSettings()
	{
		Settings.getInstance().setProperty("workbench.gui.lookupvalue.picker.maxrows", maxRows.getText());
	}

	protected void resetFilter()
	{
		lookupData.resetFilter();
	}

	private void setStatusText(final String text)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				statusBar.setText(text);
			}
		});
	}

	protected void fixStatusBarHeight()
	{
		Dimension size = statusBar.getPreferredSize();
		statusBar.setMinimumSize(size);
		statusBar.setText("");
	}

	private String getSqlSearchExpression()
	{
		if (doFilter.isSelected()) return null;

		String filter = filterValue.getText().trim();
		if (filter.isEmpty()) return null;

		String lowerFunc = dbConnection.getDbSettings().getLowerFunctionTemplate();
		if (lowerFunc != null)
		{
			filter = filter.toLowerCase();
		}
		return "%" + filter + "%";
	}

	public void loadData(final boolean selectCurrent)
	{
		WbThread load = new WbThread("LookupPickerRetrieval")
		{
			@Override
			public void run()
			{
				_loadData(selectCurrent);
			}
		};
		load.start();
	}

	private void _loadData(final boolean selectCurrent)
	{
		try
		{
			setStatusText(ResourceMgr.getString("MsgRetrieving"));
			WbSwingUtilities.showWaitCursorOnWindow(this);

			boolean useOrderBy = Settings.getInstance().getBoolProperty("workbench.gui.lookupvaluepicker.useorderby", true);
			final DataStore data = lookupLoader.getLookupData(dbConnection, maxRows.getValue(), getSqlSearchExpression(), useOrderBy);

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					DataStoreTableModel model = new DataStoreTableModel(data);
					model.setAllowEditing(false);
					lookupData.setModel(model, true);
					if (currentValue != null)
					{
						findCurrentValue(selectCurrent);
					}
					else
					{
						lookupData.getSelectionModel().setSelectionInterval(0, 0);
					}
					lookupData.requestFocusInWindow();
					int rows = data.getRowCount();
					int maxRowNum = maxRows.getValue();
					rowCount.setText(rows + " rows");

					if (rows == maxRowNum || rows == maxRowNum + 1)
					{
						rowCount.setIcon(ResourceMgr.getPng("alert"));
					}
					else
					{
						rowCount.setIcon(null);
					}
					statusPanel.doLayout();
				}
			});
		}
		catch (SQLException ex)
		{
			LogMgr.logError("LookupValuePicker.loadData()", "Could not load lookup data", ex);
		}
		finally
		{
			setStatusText("");
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}
	}

	public Map<String, Object> getSelectedPKValue()
	{
		int row = lookupData.getSelectedRow();
		if (row < 0) return null;
		PkDefinition pk = lookupLoader.getPK();
		Map<String, Object> values = new TreeMap<String, Object>();
		List<String> columns = pk.getColumns();
		DataStore ds = lookupData.getDataStore();
		int realRow = lookupData.convertRowIndexToModel(row);
		for (String column : columns)
		{
			Object value = ds.getValue(realRow, column);
			values.put(column, value);
		}
		return values;
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
		if (!doFilter.isSelected()) return;

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
		restoreSettings();
		fixStatusBarHeight();
		loadData(true);
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

	public static void pickValue(final JComponent parent, final ResultSetter result, final WbConnection conn, final String column, final TableIdentifier baseTable)
	{
		// The retrieval of the FK references must be done in a background thread to avoid blocking the UI
		// especially with Oracle retrieving FK information is deadly slow and can take minutes!
		WbThread retrieve = new WbThread("RetrieveLookupFk")
		{
			@Override
			public void run()
			{
				WbSwingUtilities.showWaitCursor(parent);

				showStatusMessage(parent, "MsgFkDeps", 0);
				TableIdentifier lookupTable = null;

				final LookupDataLoader loader = new LookupDataLoader(baseTable, column);
				try
				{
					// this is the slow part!
					loader.retrieveReferencedTable(conn);
					lookupTable = loader.getLookupTable();
				}
				catch (SQLException sql)
				{
					LogMgr.logError("LookupValuePicker.openPicker()", "Could not retrieve lookup information", sql);
				}
				finally
				{
					showStatusMessage(parent, null, 0);
					WbSwingUtilities.showDefaultCursor(parent);
				}

				if (lookupTable != null)
				{
					// a referenced table was found --> Open the picker dialog butm make sure that is opened on the EDT
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							showDialog(parent, result, conn, column, baseTable, loader);
						}
					});
				}
				else
				{
					showNotFound(parent);
				}
			}
		};
		retrieve.start();
	}

	private static void showDialog(final JComponent parent, final ResultSetter result, WbConnection conn, String column, TableIdentifier baseTable, final LookupDataLoader loader)
	{
		try
		{
			WbSwingUtilities.showWaitCursor(parent);

			// the found table is cached in the loader, so this call does not access the database
			TableIdentifier lookupTable = loader.getLookupTable();

			LookupValuePicker picker = new LookupValuePicker(conn, loader, result.getCurrentValue());
			JFrame window = (JFrame)SwingUtilities.getWindowAncestor(parent);

			String title = ResourceMgr.getFormattedString("MsgFkPickVal", baseTable.getRawTableName() + "." + column, lookupTable.getTableExpression());

			ValidatingDialog dialog = new ValidatingDialog(window, title, picker);
			picker.dialog = dialog;
			if (!Settings.getInstance().restoreWindowSize(dialog, "workbench.gui.lookupvaluepicker"))
			{
				dialog.setSize(450,350);
			}
			WbSwingUtilities.center(dialog, window);
			dialog.setVisible(true);

			Settings.getInstance().storeWindowSize(dialog, "workbench.gui.lookupvaluepicker");

			if (!dialog.isCancelled())
			{
				Map<String, Object> values = picker.getSelectedPKValue();
				Object value = values.get(loader.getReferencedColumn());
				result.setResult(value);
			}
		}
		finally
		{
			WbSwingUtilities.showDefaultCursor(parent);
		}
	}

	public static void openPicker(final WbTable table)
	{
		final int col = table.getEditingColumn();
		if (col == -1) return;

		final int row = table.getEditingRow();

		DataStore ds = table.getDataStore();
		final Object currentValue = table.getValueAt(row, col);

		WbConnection conn = ds.getOriginalConnection();
		if (!WbSwingUtilities.isConnectionIdle(table, conn)) return;

		String column = table.getColumnName(col);
		ds.checkUpdateTable();
		TableIdentifier baseTable = ds.getUpdateTable();

		ResultSetter result = new ResultSetter()
		{

			@Override
			public Object getCurrentValue()
			{
				return currentValue;
			}

			@Override
			public void setResult(Object value)
			{
				table.setValueAt(value, row, col);
			}
		};
		pickValue(table, result, conn, column, baseTable);
	}

	private static void showNotFound(JComponent current)
	{
		showStatusMessage(current, "MsgComplNoFK", 2500);
	}

	private static void showStatusMessage(final JComponent component, final String msgKey, final int timeout)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_showStatusMessage(component, msgKey, timeout);
			}
		});
	}

	private static void _showStatusMessage(JComponent component, String msgKey, int timeout)
	{
		StatusBar status = null;
		Container parent = component.getParent();
		while (parent != null)
		{
			if (parent instanceof StatusBar)
			{
				status = (StatusBar)parent;
				break;
			}
			parent = parent.getParent();
		}

		if (status == null) return;

		if (msgKey == null)
		{
			status.clearStatusMessage();
			status.doRepaint();
		}
		else
		{
			String message = ResourceMgr.getString(msgKey);
			if (timeout > 0)
			{
				status.setStatusMessage(message, timeout);
			}
			else
			{
				status.setStatusMessage(message);
				status.doRepaint();
			}
		}
	}
}
