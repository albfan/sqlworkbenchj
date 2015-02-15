/*
 * LookupValuePicker.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.text.JTextComponent;

import workbench.interfaces.NullableEditor;
import workbench.interfaces.Reloadable;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ResultSetter;
import workbench.interfaces.StatusBar;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
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
import workbench.gui.components.RowHighlighter;
import workbench.gui.components.SelectionHandler;
import workbench.gui.components.TableRowHeader;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.LookupDataLoader;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.SortDefinition;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;

import workbench.util.StringUtil;
import workbench.util.WbDateFormatter;
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
	private final LookupDataLoader lookupLoader;
	private WbScrollPane scroll;
	private JPanel statusPanel;
	private JLabel statusBar;
	private JLabel rowCount;
	private WbConnection dbConnection;
	private ValidatingDialog dialog;
	private final Map<String, Object> currentValues = new HashMap<>();
	private SelectionHandler selectionHandler;
	private boolean multiSelect;

	public LookupValuePicker(WbConnection conn, LookupDataLoader loader, Map<String, Object> values, boolean allowMultiSelect)
	{
		super(new GridBagLayout());

		lookupLoader = loader;
		dbConnection = conn;
		multiSelect = allowMultiSelect;

		if (values != null)
		{
			currentValues.putAll(values);
		}

		filterValue = new JTextField();
		filterValue.addKeyListener(this);
		filterValue.addActionListener(this);
		filterValue.setToolTipText(ResourceMgr.getString("d_LblFkFilterValue"));

		lookupData = new WbTable(false, false, false)
		{
			@Override
			protected JPopupMenu getHeaderPopup()
			{
				return createLimitedHeaderPopup();
			}
		};

		if (allowMultiSelect)
		{
			lookupData.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		}
		else
		{
			lookupData.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		lookupData.setReadOnly(true);
		lookupData.setRendererSetup(new RendererSetup(false));
		lookupData.addMouseListener(this);
		lookupData.setColumnSelectionAllowed(false);
		lookupData.setRowSelectionAllowed(true);
		lookupData.getHeaderRenderer().setShowPKIcon(true);
		selectionHandler = new SelectionHandler(lookupData);

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
		rowCount.setBorder(new CompoundBorder(new DividerBorder(DividerBorder.LEFT), new EmptyBorder(0, 5, 0, 0)));
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
		if (e.getSource() == this.filterValue && doSearch.isSelected())
		{
			loadData(false);
		}
	}

	private int highlightCurrentValues()
	{
		if (currentValues.isEmpty())
		{
			return -1;
		}

		int found = -1;

		int rows = lookupData.getRowCount();
		DataStore ds = lookupData.getDataStore();

		final Map<String, String> fkMap = lookupLoader.getForeignkeyMap();

		for (int row = 0; row < rows; row++)
		{
			Map<String, Object> rowValues = ds.getRowData(row);
			int matchingCount = 0;
			for (Map.Entry<String, String> entry : fkMap.entrySet())
			{
				Object fkValue = currentValues.get(entry.getValue());
				Object pkValue = rowValues.get(entry.getKey());
				if (RowData.objectsAreEqual(pkValue, fkValue))
				{
					matchingCount++;
				}
			}
			if (matchingCount == fkMap.size())
			{
				found = row;
				break;
			}
		}

		if (found > -1)
		{
			final int hrow = found;
			RowHighlighter highlighter = new RowHighlighter()
			{
				@Override
				public boolean hightlightColumn(int row, String column, Object columnValue)
				{
					String pkColumn = fkMap.get(column);
					return row == hrow && currentValues.containsKey(pkColumn);
				}
			};
			lookupData.applyHighlightExpression(highlighter);
		}
		return found;
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
			PkDefinition pk = lookupLoader.getPK();
			ResultInfo metadata = data.getResultInfo();
			final SortDefinition sort = new SortDefinition();
			for (String pkCol : pk.getColumns())
			{
				int index = metadata.findColumn(pkCol);
				if (index > -1)
				{
					metadata.getColumn(index).setIsPkColumn(true);
					sort.addSortColumn(index, true);
				}
			}

			EventQueue.invokeLater(new Runnable()
			{
				// <editor-fold defaultstate="collapsed" desc="Implementation">
				@Override
				public void run()
				{
					DataStoreTableModel model = new DataStoreTableModel(data);
					model.setAllowEditing(false);
					model.setSortDefinition(sort);
					lookupData.setModel(model, true);
					if (GuiSettings.getShowTableRowNumbers())
					{
						TableRowHeader.showRowHeader(lookupData);
					}

					int row = highlightCurrentValues();

					// always select at least one row.
					// as the focus is set to the table containing the lookup data,
					// the user can immediately use the cursor keys to select one entry.
					if (!selectCurrent || row < 0) row = 0;
					selectionHandler.selectRow(row);

					int rows = data.getRowCount();
					int maxRowNum = maxRows.getValue();
					rowCount.setText(ResourceMgr.getFormattedString("MsgRows", rows).replaceAll("[\\(\\)]", ""));

					if (rows >= maxRowNum) // some drivers return one row more than requested
					{
						rowCount.setIcon(IconMgr.getInstance().getLabelIcon("alert"));
					}
					else
					{
						rowCount.setIcon(null);
					}
					statusPanel.doLayout();

					if (doFilter.isSelected())
					{
						filterValue.requestFocusInWindow();
					}
					else
					{
						lookupData.requestFocusInWindow();
					}
				}
				// </editor-fold>
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

	public List<Map<String, Object>> getSelectedPKValues()
	{
		int[] rows = lookupData.getSelectedRows();
		if (rows == null || rows.length == 0) return Collections.emptyList();
		PkDefinition pk = lookupLoader.getPK();
		List<Map<String, Object>> result = new ArrayList<>(1);
		List<String> columns = pk.getColumns();
		DataStore ds = lookupData.getDataStore();

		for (int i=0; i < rows.length; i++)
		{
			int row = rows[i];
			Map<String, Object> values = new HashMap<>();
			for (String column : columns)
			{
				Object value = ds.getValue(row, column);
				values.put(column, value);
			}
			result.add(values);
		}
		return result;
	}

	protected void applyFilter()
	{
		ContainsComparator comp = new ContainsComparator();
		DataRowExpression filter = new DataRowExpression(comp, filterValue.getText());
		filter.setIgnoreCase(true);
		lookupData.applyFilter(filter);
		selectionHandler.selectRow(0);
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
				if (e.getKeyChar() == KeyEvent.VK_ENTER)
				{
					selectValue();
				}
				else if (e.getKeyChar() == KeyEvent.VK_ESCAPE)
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
		if (e.getSource() == this.filterValue && e.getModifiers() == 0)
		{
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE && StringUtil.isNonBlank(filterValue.getText()))
			{
				e.consume();
				resetFilter();
			}
			else
			{
				selectionHandler.handleKeyPressed(e);
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	@Override
	public boolean validateInput()
	{
		if (multiSelect)
		{
			return lookupData.getSelectedRowCount() > 0;
		}
		else
		{
			return lookupData.getSelectedRowCount() == 1;
		}
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

	public static void pickValue(final JComponent parent, final ResultSetter result, final WbConnection conn, final String column, final TableIdentifier baseTable, final boolean allowMultiSelect)
	{
		// The retrieval of the FK references must be done in a background thread to avoid blocking the UI
		// especially with Oracle retrieving FK information is deadly slow and can take minutes!
		WbThread retrieve = new WbThread("RetrieveLookupFk")
		{
			// <editor-fold defaultstate="collapsed" desc="Implementation">
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
							showDialog(parent, result, conn, baseTable, loader, allowMultiSelect);
						}
					});
				}
				else
				{
					showNotFound(parent);
				}
			}
			// </editor-fold>
		};
		retrieve.start();
	}

	private static void showDialog(final JComponent parent, final ResultSetter result, WbConnection conn, TableIdentifier baseTable, final LookupDataLoader loader, final boolean multiSelect)
	{
		try
		{
			WbSwingUtilities.showWaitCursor(parent);

			// the found table is cached in the loader, so this call does not access the database
			TableIdentifier lookupTable = loader.getLookupTable();
			List<String> refColumns = loader.getReferencingColumns();

			String cols  = "(" + StringUtil.listToString(refColumns, ',') + ")";

			LookupValuePicker picker = new LookupValuePicker(conn, loader, result.getFKValues(refColumns), multiSelect);
			JFrame window = (JFrame)SwingUtilities.getWindowAncestor(parent);

			String title = ResourceMgr.getFormattedString("MsgFkPickVal", baseTable.getRawTableName() + cols, lookupTable.getTableExpression());

			ValidatingDialog dialog = new ValidatingDialog(window, title, picker);
			picker.dialog = dialog;
			if (!Settings.getInstance().restoreWindowSize(dialog, "workbench.gui.lookupvaluepicker"))
			{
				dialog.setSize(450,350);
			}
			WbSwingUtilities.center(dialog, window);
			dialog.setVisible(true);

			Settings.getInstance().storeWindowSize(dialog, "workbench.gui.lookupvaluepicker");
			picker.saveSettings();

			if (!dialog.isCancelled())
			{
				List<Map<String, Object>> values = picker.getSelectedPKValues();
				result.setResult(values, loader.getForeignkeyMap());
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

		WbConnection conn = ds.getOriginalConnection();
		if (!WbSwingUtilities.isConnectionIdle(table, conn)) return;

		final String editColumn = table.getColumnName(col);
		ds.checkUpdateTable();
		TableIdentifier baseTable = ds.getUpdateTable();

		if (baseTable == null)
		{
			LogMgr.logWarning("LookupValuePicker.openPicker()", "No update table available for the current table. Cannot open FK dialog");
			return;
		}

		ResultSetter result = new ResultSetter()
		{
			// <editor-fold defaultstate="collapsed" desc="Implementation">
			@Override
			public Map<String, Object> getFKValues(List<String> columns)
			{
				Map<String, Object> result = new HashMap<>(columns.size());
				for (String name : columns)
				{
					int colIndex = table.getColumnIndex(name);
					if (colIndex > -1)
					{
						result.put(name, table.getValueAt(row, colIndex));
					}
				}
				return result;
			}

			@Override
			public void setResult(final List<Map<String, Object>> valueList, Map<String, String> fkColumnMap)
			{
				JComponent comp = (JComponent)table.getEditorComponent();
				final JTextComponent editor;
				if (comp instanceof JTextComponent)
				{
					editor = (JTextComponent)comp;
				}
				else if (comp instanceof NullableEditor)
				{
					editor = ((NullableEditor)comp).getEditor();
				}
				else
				{
					editor = null;
				}

				// we only allow single selection when the lookup picker is invoked for a WbTable
				Map<String, Object> values = valueList.get(0);

				for (Map.Entry<String, Object> entry : values.entrySet())
				{
					String col = entry.getKey();
					String fkColumn = fkColumnMap.get(col);
					int colIndex = table.getColumnIndex(fkColumn);

					Object value = entry.getValue();
					if (fkColumn.equals(editColumn) && editor != null)
					{
						editor.setText(value == null ? "" : WbDateFormatter.getDisplayValue(value));
					}
					if (colIndex > -1)
					{
						table.setValueAt(value, row, colIndex);
					}
				}
			}
			// </editor-fold>
		};
		pickValue(table, result, conn, editColumn, baseTable, false);
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
