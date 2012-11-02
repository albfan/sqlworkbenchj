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
import java.awt.Font;
import java.awt.FontMetrics;
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
import workbench.gui.components.WbScrollPane;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbarButton;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.renderer.RendererSetup;

import workbench.storage.DataStore;
import workbench.storage.LookupDataLoader;
import workbench.storage.filter.ContainsComparator;
import workbench.storage.filter.DataRowExpression;

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
	private JLabel statusBar;
	private WbConnection dbConnection;
	private ValidatingDialog dialog;

	public LookupValuePicker(WbConnection conn, LookupDataLoader loader)
	{
		super(new GridBagLayout());

		lookupLoader = loader;
		dbConnection = conn;

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
		doFilter.setSelected(true);
		doSearch = new JRadioButton(ResourceMgr.getString("LblFkPickerSelect"));
		doSearch.setToolTipText(ResourceMgr.getDescription("LblFkPickerSelect"));
		buttonGroup = new ButtonGroup();
		buttonGroup.add(doFilter);
		buttonGroup.add(doSearch);

		radios.add(doFilter);
		radios.add(doSearch);

		JPanel edit = new JPanel(new BorderLayout(0, 0));
		edit.add(filterValue, BorderLayout.CENTER);
		edit.add(radios, BorderLayout.PAGE_START);

		FlatButton reload = new FlatButton(new ReloadAction(this));
		reload.setText(null);
		reload.setMargin(WbToolbarButton.MARGIN);
		edit.add(reload, BorderLayout.LINE_END);

		JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0,0));
		statusBar = new JLabel();
		Font f = statusBar.getFont();
		FontMetrics fm = null;
		if (f != null) fm = statusBar.getFontMetrics(f);
		int height = fm == null ? 0 : fm.getHeight() + 6;
		height = Math.min(24, height);
		Dimension d = new Dimension(80, height);
		statusBar.setMinimumSize(d);
		statusBar.setPreferredSize(d);
		statusPanel.add(statusBar);

		JLabel l = new JLabel(" " + ResourceMgr.getString("LblMaxRows") + " ");
		statusPanel.add(l);
		statusBar.setBorder(new DividerBorder(DividerBorder.RIGHT));
		maxRows = new NumberField(6);
		WbSwingUtilities.setMinimumSize(maxRows, 6);
		statusPanel.add(maxRows);

		Border b = new CompoundBorder(new EtchedBorder(EtchedBorder.LOWERED), new EmptyBorder(1,5,1,1));
		statusPanel.setBorder(b);

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
				loadData();
			}
		}
	}

	@Override
	public void reload()
	{
		filterValue.setText("");
		loadData();
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

	public void loadData()
	{
		try
		{
			setStatusText("Retrieving lookup data...");
			WbSwingUtilities.showWaitCursorOnWindow(this);

			DataStore data = lookupLoader.getLookupData(dbConnection, maxRows.getValue(), getSqlSearchExpression());
			DataStoreTableModel model = new DataStoreTableModel(data);
			model.setAllowEditing(false);
			lookupData.setModel(model, true);
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					lookupData.getSelectionModel().setSelectionInterval(0, 0);
					lookupData.requestFocusInWindow();
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
		loadData();
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

	public static Object pickValue(final JComponent parent, WbConnection conn, String column, TableIdentifier baseTable)
	{
		LookupDataLoader loader = new LookupDataLoader(baseTable, column);
		try
		{
			loader.retrieveReferencedTable(conn);
			TableIdentifier lookupTable = loader.getReferencedTable();
			if (lookupTable != null)
			{
				LookupValuePicker picker = new LookupValuePicker(conn, loader);
				JFrame window = (JFrame)SwingUtilities.getWindowAncestor(parent);
				String title = ResourceMgr.getFormattedString("MsgFkPickVal", baseTable.getRawTableName() + "." + column, lookupTable.getTableExpression());
				ValidatingDialog dialog = new ValidatingDialog(window, title, picker);
				picker.dialog = dialog;
				dialog.setSize(450,350);
				Settings.getInstance().restoreWindowSize(picker);
				WbSwingUtilities.center(dialog, window);
				dialog.setVisible(true);
				Settings.getInstance().storeWindowSize(picker);

				if (!dialog.isCancelled())
				{
					Map<String, Object> values = picker.getSelectedPKValue();
					Object value = values.get(loader.getReferencedColumn());
					return value;
				}
			}
			else
			{
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						showNotFound(parent);
					}
				});
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("LookupValuePicker.openPicker()", "Could not retrieve lookup information", sql);
		}
		return null;
	}

	public static void openPicker(WbTable table)
	{
		int col = table.getEditingColumn();
		if (col == -1) return;

		int row = table.getEditingRow();

		DataStore ds = table.getDataStore();
		String column = table.getColumnName(col);
		ds.checkUpdateTable();
		TableIdentifier baseTable = ds.getUpdateTable();
		Object value = pickValue(table, ds.getOriginalConnection(), column, baseTable);
		if (value != null)
		{
			table.setValueAt(value, row, col);
		}
	}

	private static void showNotFound(JComponent component)
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
		if (status != null)
		{
			status.setStatusMessage(ResourceMgr.getString("MsgComplNoFK"), 2500);
		}
	}
}
