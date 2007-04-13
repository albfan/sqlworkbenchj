/*
 * WbTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.CellEditor;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JPopupMenu.Separator;
import javax.swing.UIDefaults;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CopyAsSqlDeleteInsertAction;
import workbench.gui.actions.CopyAsSqlInsertAction;
import workbench.gui.actions.CopyAsSqlUpdateAction;
import workbench.gui.actions.CopySelectedAsSqlDeleteInsertAction;
import workbench.gui.actions.CopySelectedAsSqlInsertAction;
import workbench.gui.actions.CopySelectedAsSqlUpdateAction;
import workbench.gui.actions.CopySelectedAsTextAction;
import workbench.gui.actions.CopyAsTextAction;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.OptimizeColumnWidthAction;
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.SetColumnWidthAction;
import workbench.gui.actions.SortAscendingAction;
import workbench.gui.actions.SortDescendingAction;
import workbench.gui.actions.WbAction;
import workbench.gui.renderer.RendererFactory;
import workbench.gui.renderer.RequiredFieldHighlighter;
import workbench.gui.renderer.RowStatusRenderer;
import workbench.gui.renderer.TextAreaRenderer;
import workbench.gui.renderer.WbRenderer;
import workbench.gui.sql.DwStatusBar;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.NullValue;
import workbench.storage.PkMapping;
import workbench.storage.ResultInfo;
import workbench.storage.filter.FilterExpression;
import workbench.util.FileDialogUtil;
import workbench.util.SqlUtil;

public class WbTable
	extends JTable
	implements ActionListener, FocusListener, MouseListener,
	           FontChangedListener, ListSelectionListener, PropertyChangeListener, Resettable
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	protected JPopupMenu popup;

	private JPopupMenu headerPopup;

	private DataStoreTableModel dwModel;
	private String lastSearchCriteria;
	private int lastFoundRow = -1;

	private WbTextCellEditor defaultEditor;
	private WbCellEditor multiLineEditor;
	private TextAreaRenderer multiLineRenderer;
	private WbTextCellEditor defaultNumberEditor;
	private JTextField numberEditorTextField;

	private SortAscendingAction sortAscending;
	private SortDescendingAction sortDescending;
	private OptimizeColumnWidthAction optimizeCol;
	private OptimizeAllColumnsAction optimizeAllCol;
	private SetColumnWidthAction setColWidth;

	private TableReplacer replacer;
	
	private SaveDataAsAction saveDataAsAction;
	
	private CopyAsTextAction copyAsTextAction;
	private CopyAsSqlInsertAction copyInsertAction;
	private CopyAsSqlDeleteInsertAction copyDeleteInsertAction;
	private CopyAsSqlUpdateAction copyUpdateAction;

	private CopySelectedAsTextAction copySelectedAsTextAction;
	private CopySelectedAsSqlInsertAction copySelectedAsInsertAction;
	private CopySelectedAsSqlDeleteInsertAction copySelectedAsDeleteInsertAction;
	private CopySelectedAsSqlUpdateAction copySelectedAsUpdateAction;
	
	private FilterDataAction filterAction;
	private ResetFilterAction resetFilterAction;

	private PrintAction printDataAction;
	private PrintPreviewAction printPreviewAction;

	private boolean adjustToColumnLabel = false;
	private boolean modelChanging = false;
	
	private int headerPopupX = -1;
	private Map<String, Integer> savedColumnSizes;

	private RowHeightResizer rowResizer;
	private List<TableModelListener> changeListener = new ArrayList<TableModelListener>();
	private JScrollPane scrollPane;

	private DwStatusBar statusBar;
	
	private String defaultPrintHeader = null;
	private boolean showPopup = true;
	private boolean selectOnRightButtonClick = false;
	private boolean highlightRequiredFields = false;
	private boolean useMultilineTooltip = true;
	private Color requiredColor;
	// </editor-fold>
	
	public WbTable()
	{
		this(true, true, false);
	}

	public WbTable(boolean printEnabled)
  {
      this(printEnabled, true, false);
  }

	public WbTable(boolean printEnabled, boolean sqlCopyAllowed, boolean replaceAllowed)
	{
		super(EmptyTableModel.EMPTY_MODEL);
		
		this.sortAscending = new SortAscendingAction(this);
		this.sortAscending.setEnabled(false);
		this.sortDescending = new SortDescendingAction(this);
		this.sortDescending.setEnabled(false);
		this.optimizeCol = new OptimizeColumnWidthAction(this);
		this.optimizeAllCol = new OptimizeAllColumnsAction(this);
		this.optimizeAllCol.setEnabled(true);
		this.setColWidth = new SetColumnWidthAction(this);
		this.setAutoCreateColumnsFromModel(true);

		this.headerPopup = new JPopupMenu();
		this.headerPopup.add(this.sortAscending.getMenuItem());
		this.headerPopup.add(this.sortDescending.getMenuItem());
		this.headerPopup.addSeparator();
		this.headerPopup.add(this.optimizeCol.getMenuItem());
		this.headerPopup.add(this.optimizeAllCol.getMenuItem());
		this.headerPopup.add(this.setColWidth.getMenuItem());

		this.setDoubleBuffered(true);

		Font dataFont = this.getFont();
		if (dataFont == null) dataFont = (Font)UIManager.get("Table.font");

		this.defaultEditor = WbTextCellEditor.createInstance(this);
		this.defaultEditor.setFont(dataFont);

		setFont(dataFont);
		
		// Create a separate editor for numbers that is right alligned
		numberEditorTextField = new JTextField();
		numberEditorTextField.setFont(dataFont);
		numberEditorTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		this.defaultNumberEditor = new WbTextCellEditor(this, numberEditorTextField);
		
		this.multiLineEditor = new WbCellEditor(this);
		this.multiLineRenderer = new TextAreaRenderer();

		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		this.replacer = new TableReplacer(this);

		this.copyAsTextAction = new CopyAsTextAction(this);
		if (sqlCopyAllowed)
		{
			this.copyInsertAction = new CopyAsSqlInsertAction(this);
			this.copyDeleteInsertAction = new CopyAsSqlDeleteInsertAction(this);
			this.copyUpdateAction = new CopyAsSqlUpdateAction(this);
		}
		this.saveDataAsAction = new SaveDataAsAction(this);
		this.saveDataAsAction.setEnabled(true);

		this.filterAction = new FilterDataAction(this);
		this.resetFilterAction = new ResetFilterAction(this);

		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.addPopupAction(this.saveDataAsAction, false);
		this.addPopupAction(this.copyAsTextAction, true);
		
		if (copyUpdateAction != null) this.addPopupAction(this.copyUpdateAction, false);
		if (copyInsertAction != null) this.addPopupAction(this.copyInsertAction, false);
		if (copyDeleteInsertAction != null) this.addPopupAction(this.copyDeleteInsertAction, false);

		if (sqlCopyAllowed)
		{
			WbMenu copy = this.getCopySelectedMenu();
			this.addPopupSubMenu(copy, true);
		}
		else
		{
			this.copySelectedAsTextAction = new CopySelectedAsTextAction(this, "MnuTxtCopySelected");
			this.addPopupAction(this.copySelectedAsTextAction, false);
		}

		this.addPopupAction(this.replacer.getFindAction(), true);
		this.addPopupAction(this.replacer.getFindAgainAction(), false);
		if (replaceAllowed) 
		{
			this.addPopupAction(this.replacer.getReplaceAction(), false);
		}

		if (printEnabled)
		{
			this.printDataAction = new PrintAction(this);
			this.printPreviewAction = new PrintPreviewAction(this);
			this.popup.addSeparator();
			this.popup.add(this.printDataAction.getMenuItem());
			this.popup.add(this.printPreviewAction.getMenuItem());
		}

		this.addMouseListener(this);

		InputMap im = this.getInputMap(WHEN_FOCUSED);
		ActionMap am = this.getActionMap();
		this.replacer.getFindAction().addToInputMap(im, am);
		this.replacer.getFindAgainAction().addToInputMap(im, am);
		this.copyAsTextAction.addToInputMap(im, am);
		this.saveDataAsAction.addToInputMap(im, am);
		this.optimizeAllCol.addToInputMap(im, am);
		
		Settings.getInstance().addFontChangedListener(this);
		Settings.getInstance().addPropertyChangeListener(this);
		
		this.initDefaultRenderers();
		this.initDefaultEditors();
					
		Action a = new AbstractAction()
		{
			public void actionPerformed(ActionEvent evt)
			{
				stopEditing();
			}
		};
		
		this.getInputMap().put(WbSwingUtilities.ENTER, "wbtable-stop-editing");
		this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(WbSwingUtilities.ENTER, "wbtable-stop-editing");
		this.getActionMap().put("wbtable-stop-editing", a);
	}

	public void setShowPopupMenu(boolean aFlag)
	{
		this.showPopup = aFlag;
	}

	public void setRowResizeAllowed(boolean aFlag)
	{
		if (aFlag && this.rowResizer == null)
		{
			this.rowResizer = new RowHeightResizer(this);
		}
		else if (!aFlag)
		{
			if (this.rowResizer != null)
			{
				this.rowResizer.done();
			}
			this.rowResizer = null;
		}
	}

	public void setStatusBar(DwStatusBar bar)
	{
		this.statusBar = bar;
	}
	
	/**
	 * For some reason my Renderers do not display bigger
	 * fonts properly. So I have to adjust the row height
	 * when the font is defined
	 */
	public void setFont(Font f)
	{
		super.setFont(f);
		adjustRowHeight();
	}
	
	private void adjustRowHeight()
	{
		Graphics g = getGraphics();
		Font f = getFont();
		if (f == null) return;
		
		// Depending on the stage of initialization 
		// not all calls work the same, so we need
		// to take care that no exceptioin gets thrown 
		// in here
		FontMetrics fm = null;
		if (g != null)
		{
			try
			{
				fm = g.getFontMetrics(getFont());
			}
			catch (Throwable e)
			{
				 fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
			}
		}
		else
		{
			fm = getFontMetrics(getFont());
		}
		if (fm != null) setRowHeight(fm.getHeight() + 2);
	}
	
	public void useMultilineTooltip(boolean flag)
	{
		this.useMultilineTooltip = flag;
	}
	
	public JToolTip createToolTip()
	{
		if (useMultilineTooltip)
		{
			JToolTip tip = new MultiLineToolTip();
			tip.setComponent(this);
			return tip;
		}
		else
		{
			return super.createToolTip();
		}
	}

	public FilterDataAction getFilterAction() { return this.filterAction; }
	public ResetFilterAction getResetFilterAction() { return this.resetFilterAction; }

	public CopySelectedAsTextAction getCopySelectedAsTextAction()
	{
		return this.copySelectedAsTextAction;
	}

	public CopySelectedAsSqlDeleteInsertAction getCopySelectedAsSqlDeleteInsertAction()
	{
		return this.copySelectedAsDeleteInsertAction;
	}

	public CopySelectedAsSqlInsertAction getCopySelectedAsSqlInsertAction()
	{
		return this.copySelectedAsInsertAction;
	}

	public CopySelectedAsSqlUpdateAction getCopySelectedAsSqlUpdateAction()
	{
		return this.copySelectedAsUpdateAction;
	}

	public void populateCopySelectedMenu(WbMenu copyMenu)
	{
		if (copySelectedAsTextAction == null)
		{
			copySelectedAsTextAction = new CopySelectedAsTextAction(this);
		}

		if (copySelectedAsInsertAction == null && this.copyInsertAction != null)
		{
			copySelectedAsInsertAction = new CopySelectedAsSqlInsertAction(this);
		}

		if (copySelectedAsDeleteInsertAction == null && copyDeleteInsertAction != null)
		{
			copySelectedAsDeleteInsertAction = new CopySelectedAsSqlDeleteInsertAction(this);
		}

		if (copySelectedAsUpdateAction == null && copyUpdateAction != null)
		{
			copySelectedAsUpdateAction = new CopySelectedAsSqlUpdateAction(this);
		}

		copyMenu.add(this.copySelectedAsTextAction);

		if (copySelectedAsUpdateAction != null) copyMenu.add(copySelectedAsUpdateAction);
		if (copySelectedAsInsertAction != null) copyMenu.add(copySelectedAsInsertAction);
		if (copySelectedAsDeleteInsertAction != null) copyMenu.add(copySelectedAsDeleteInsertAction);
	}
	
	public WbMenu getCopySelectedMenu()
	{
		WbMenu copyMenu = createCopySelectedMenu();
		populateCopySelectedMenu(copyMenu);
		return copyMenu;
	}

	public static WbMenu createCopySelectedMenu()
	{
		WbMenu copyMenu = new WbMenu(ResourceMgr.getString("MnuTxtCopySelected"));
		copyMenu.setParentMenuId(ResourceMgr.MNU_TXT_DATA);
		return copyMenu;
	}
	
	public CopyAsSqlInsertAction getCopyAsInsertAction()
	{
		 return this.copyInsertAction;
	}

	public CopyAsSqlDeleteInsertAction getCopyAsDeleteInsertAction()
	{
		 return this.copyDeleteInsertAction;
	}

	public CopyAsSqlUpdateAction getCopyAsUpdateAction()
	{
		return this.copyUpdateAction;
	}

	public SaveDataAsAction getExportAction()
	{
		return this.saveDataAsAction;
	}

	public CopyAsTextAction getDataToClipboardAction()
	{
		return this.copyAsTextAction;
	}


	public TableReplacer getReplacer() { return this.replacer; }
	
	public void setSelectOnRightButtonClick(boolean flag) { this.selectOnRightButtonClick = flag; }
	public boolean getSelectOnRightButtonClick() { return this.selectOnRightButtonClick; }

	public void reset()
	{
		this.stopEditing();
		if (this.getModel() == EmptyTableModel.EMPTY_MODEL) return;
		this.setModel(EmptyTableModel.EMPTY_MODEL, false);
	}

	public JPopupMenu getPopupMenu()
	{
		return this.popup;
	}

	private void addPopupSubMenu(WbMenu submenu, boolean withSep)
	{
		if (this.popup == null) this.popup = new JPopupMenu();
		if (withSep)
		{
			this.popup.addSeparator();
		}
		this.popup.add(submenu);
	}

	public void addPopupAction(WbAction anAction, boolean withSep)
	{
		this.addPopupMenu(anAction.getMenuItem(), withSep);
	}

	public void addPopupMenu(JMenuItem item, boolean withSep)
	{
		if (this.popup == null) this.popup = new JPopupMenu();

		if (this.printDataAction != null)
		{

			if (withSep)
			{
				this.popup.add(new Separator(), this.popup.getComponentCount() - 3);
			}
			this.popup.add(item, this.popup.getComponentCount() - 3);
		}
		else
		{
			if (withSep) this.popup.addSeparator();
			this.popup.add(item);
		}
	}
	
	public void valueChanged(ListSelectionEvent e)
	{
		super.valueChanged(e);
		if (e.getValueIsAdjusting()) return;

		boolean selected = this.getSelectedRowCount() > 0;

		if (this.copySelectedAsTextAction != null)
		{
			this.copySelectedAsTextAction.setEnabled(selected);
		}

		if (this.copySelectedAsInsertAction != null)
		{
			this.copySelectedAsInsertAction.setEnabled(selected);
		}

		if (this.copySelectedAsUpdateAction != null)
		{
			this.copySelectedAsUpdateAction.setEnabled(selected);
		}

		if (this.copySelectedAsDeleteInsertAction != null)
		{
			this.copySelectedAsDeleteInsertAction.setEnabled(selected);
		}

	}

	protected void configureEnclosingScrollPane()
	{
		super.configureEnclosingScrollPane();
		Container p = getParent();
		if (p instanceof JViewport)
		{
			Container gp = p.getParent();
			if (gp instanceof JScrollPane)
			{
				// the scrollpane is needed to check the position
				// of the scrollbars in getFirstVisibleRow()
				this.scrollPane = (JScrollPane)gp;
				JViewport viewport = scrollPane.getViewport();
				if (viewport == null || viewport.getView() != this)
				{
					this.scrollPane = null;
				}
			}
		}
		this.checkMouseListener();
	}

	private void checkMouseListener()
	{
		JTableHeader th = this.getTableHeader();
		MouseListener[] list = th.getMouseListeners();
		if (list == null)
		{
			th.addMouseListener(this);
			return;
		}
		for (int i=0; i < list.length; i++)
		{
			if (list[i] == this) return;
		}
		th.addMouseListener(this);
	}

	/**
	 * Set the header to be used for printing.
	 * 
	 * @param aHeader the print header
	 * @see workbench.print#setHeaderText(String)
	 */
	public void setPrintHeader(String aHeader)
	{
		this.defaultPrintHeader = aHeader;
	}

	/**
	 * Return the header to be used for printing.
	 * 
	 * @return the print header
	 * @see workbench.print.TablePrinter
	 */
	public String getPrintHeader()
	{
		return this.defaultPrintHeader;
	}

	public Component prepareEditor(TableCellEditor editor, int row, int column)
	{
		Component comp = super.prepareEditor(editor, row, column);
		if (!this.highlightRequiredFields) return comp;

		ResultInfo info = this.getDataStore().getResultInfo();
		int realColumn  = column;
		if (this.dwModel.getShowStatusColumn())
		{
			realColumn = column - 1;
		}
		boolean nullable = info.isNullable(realColumn);
		if (editor == this.defaultEditor || editor == this.defaultNumberEditor)
		{
			WbTextCellEditor wbEditor = (WbTextCellEditor)editor;
			if (nullable)
			{
				wbEditor.setBackground(wbEditor.getDefaultBackground());
			}
			else
			{
				wbEditor.setBackground(requiredColor);
			}
		}
		return comp;
	}

	public boolean editCellAt(int row, int column, EventObject e)
	{
		boolean result = super.editCellAt(row, column, e);
		if (result && this.highlightRequiredFields)
		{
			initRendererHighlight(row);
		}
		return result;
	}

	private void initRendererHighlight(int row)
	{
		ResultInfo info = this.getDataStore().getResultInfo();
		int offset = 0;
		int tableCols = this.getColumnCount();
		if (this.dwModel.getShowStatusColumn()) offset = 1;
		boolean[] highlightCols = new boolean[tableCols];
		for (int i=0; i < info.getColumnCount(); i++)
		{
			boolean nullable = info.isNullable(i);
			highlightCols[i+offset] = !nullable;
		}

		for (int i=0; i < tableCols; i++)
		{
			TableCellRenderer rend = getCellRenderer(row, i);
			if (rend instanceof RequiredFieldHighlighter)
			{
				RequiredFieldHighlighter highlighter = (RequiredFieldHighlighter)rend;
				highlighter.setHighlightBackground(requiredColor);
				highlighter.setEditingRow(row);
				highlighter.setHighlightColumns(highlightCols);
			}
		}
		this.repaint();
	}

	public boolean isUpdateable()
	{
		DataStore ds = this.getDataStore();
		if (ds == null) return false;
		return ds.isUpdateable();
	}

	public void removeEditor()
	{
		final int row = this.getEditingRow();
		final int col = this.getEditingColumn();
		super.removeEditor();
		resetHighlightRenderers(row, col);
	}

	private void resetHighlightRenderers(final int row, final int col)
	{
		if (!this.highlightRequiredFields) return;
		int colcount = this.getColumnCount();
		for (int i=0; i < colcount; i++)
		{
			TableCellRenderer renderer = getCellRenderer(row, i);
			if (renderer instanceof RequiredFieldHighlighter)
			{
				RequiredFieldHighlighter highlighter = (RequiredFieldHighlighter)renderer;
				highlighter.setEditingRow(-1);
				highlighter.setHighlightBackground(null);
				highlighter.setHighlightColumns(null);
			}
		}
		requestFocusInWindow();
		changeSelection(row, col, false, false);
	}

	/**
	 *	Removes all registered listeners from the table model
	 */
	private void removeListeners()
	{
		if (this.dwModel == null) return;
		for (TableModelListener l : changeListener)
		{
			this.dwModel.removeTableModelListener(l);
		}
	}

	private void addListeners()
	{
		if (this.dwModel == null) return;
		TableModelEvent evt = new TableModelEvent(this.dwModel);
		for (TableModelListener l : changeListener)
		{
			l.tableChanged(evt);
			this.dwModel.addTableModelListener(l);
		}
	}

	public void setModel(TableModel aModel)
	{
		this.setModel(aModel, false);
	}

	public void setModel(TableModel aModel, boolean sortIt)
	{
		removeListeners();

		JTableHeader header = this.getTableHeader();
		if (header != null)
		{
			header.removeMouseListener(this);
		}

		if (this.dwModel != null)
		{
			this.dwModel.dispose();
			this.dwModel = null;
		}

		try
		{
			this.modelChanging = true;
			super.setModel(aModel);
		}
		catch (Throwable th)
		{
			LogMgr.logError("WbTable.setModel()", "Error setting table model", th);
		}
		finally
		{
			this.modelChanging = false;
		}

		resetFilter();
		adjustRowHeight();

		if (aModel instanceof DataStoreTableModel)
		{

			this.dwModel = (DataStoreTableModel)aModel;
			if (sortIt && header != null)
			{
				header.setDefaultRenderer(RendererFactory.getSortHeaderRenderer());
				header.addMouseListener(this);
			}
			
			// If none of the columns is a BLOB column
			// getCellRenderer() does not need to check for the BLOB renderer
			int cols = this.dwModel.getColumnCount();
			this.checkForBlobs = false;
			for (int i=0; i < cols; i++)
			{
				//Class cl = this.dwModel.getColumnClass(i);
				int type = this.dwModel.getColumnType(i);
				if (SqlUtil.isBlobType(type))// || java.sql.Blob.class.isAssignableFrom(cl))
				{
					this.checkForBlobs = true;
					break;
				}
			}
		}

		if (aModel != EmptyTableModel.EMPTY_MODEL)
		{
			if (this.sortAscending != null) this.sortAscending.setEnabled(sortIt);
			if (this.sortDescending != null) this.sortDescending.setEnabled(sortIt);

			// it seems that JTable.setModel() does reset the default
			// renderers and editors, so we'll have to do it again...
			this.initDefaultRenderers();
			this.initDefaultEditors();
		}
		addListeners();
	}

	private FilterExpression lastFilter;
	private FilterExpression currentFilter;

	public void clearLastFilter(boolean keepGeneralFilter)
	{
		if (keepGeneralFilter && this.lastFilter != null && !this.lastFilter.isColumnSpecific()) return;
		this.lastFilter = null;
	}
	
	public void resetFilter()
	{
		this.currentFilter = null;
		if (this.dwModel == null) return;
		this.dwModel.resetFilter();
	}

	public FilterExpression getLastFilter() { return lastFilter; }
	public boolean isFiltered() { return (currentFilter != null); }

	public void applyFilter(FilterExpression filter)
	{
		if (this.dwModel == null) return;
		this.lastFilter = filter;
		this.currentFilter = filter;
		this.dwModel.applyFilter(filter);
	}

	public DataStoreTableModel getDataStoreTableModel()
	{
		return this.dwModel;
	}

	public DataStore getDataStore()
	{
		if (this.dwModel != null)
		{
			return this.dwModel.getDataStore();
		}
		else
		{
			return null;
		}
	}

	public void restoreOriginalValues()
	{
		if (this.dwModel != null)
		{
			this.dwModel.getDataStore().restoreOriginalValues();
			this.dwModel.fireTableDataChanged();
		}
	}

	public PrintPreviewAction getPrintPreviewAction()
	{
		return this.printPreviewAction;
	}

	public PrintAction getPrintAction()
	{
		return this.printDataAction;
	}

	public String getValueAsString(int row, int column)
		throws IndexOutOfBoundsException
	{
		Object value = this.getValueAt(row, column);
		if (value == null) return null;
		if (value instanceof NullValue) return null;
		return value.toString();
	}

	public boolean getShowStatusColumn()
	{
		if (this.dwModel == null) return false;
		return this.dwModel.getShowStatusColumn();
	}

	public void setAdjustToColumnLabel(boolean aFlag)
	{
		this.adjustToColumnLabel = aFlag;
	}

	public void setShowStatusColumn(final boolean flag)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				_setShowStatusColumn(flag);
			}
		});
	}
	
	protected void _setShowStatusColumn(boolean aFlag)
	{
		if (this.dwModel == null) return;
		if (aFlag == this.dwModel.getShowStatusColumn()) return;

		int column = this.getSelectedColumn();
		final int row = this.getSelectedRow();

		this.saveColumnSizes();
		this.dwModel.setShowStatusColumn(aFlag);

		if (aFlag)
		{
			TableColumn col = this.getColumnModel().getColumn(0);
			col.setCellRenderer(new RowStatusRenderer());
			col.setMaxWidth(20);
			col.setMinWidth(20);
			col.setPreferredWidth(20);
		}
		else
		{
			TableColumnModel model = this.getTableHeader().getColumnModel();
			if (model.getColumnCount() > this.dwModel.getColumnCount())
			{
				TableColumn col = model.getColumn(0);
				model.removeColumn(col);
			}
		}

		this.initMultiLineRenderer();
		this.initDefaultEditors();
		this.restoreColumnSizes();

		if (row >= 0)
		{
			this.getSelectionModel().setSelectionInterval(row, row);
			final int newColumn;
			if (aFlag)
				newColumn = column + 1;
			else
				newColumn = column - 1;

			if (newColumn >= 0)
			{
				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						changeSelection(row, newColumn, true, true);
					}
				});
			}
		}
	}

	public boolean isPrimarySortColumn(int viewIndex)
	{
		if (this.dwModel == null) return false;
		int col = this.convertColumnIndexToModel(viewIndex);
		return this.dwModel.isPrimarySortColumn(col);
	}
	
	public boolean isViewColumnSorted(int viewIndex)
	{
		if (this.dwModel == null) return false;
		int col = this.convertColumnIndexToModel(viewIndex);
		return this.dwModel.isSortColumn(col);
	}
	
	public boolean isViewColumnSortAscending(int viewIndex)
	{
		if (this.dwModel == null) return true;
		int col = this.convertColumnIndexToModel(viewIndex);
		return this.dwModel.isSortAscending(col);
	}

	public void sortingStarted()
	{
		final Container c = (this.scrollPane == null ? this : scrollPane);
		
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.showWaitCursor(c);
			}
		});
	}

	public void sortingFinished()
	{
		final Container c = (this.scrollPane == null ? this : scrollPane);
		
		WbSwingUtilities.invoke(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.showDefaultCursor(c);
				// The sorting indicator is not properly displayed
				// if repaint() is not called
				getTableHeader().repaint();
			}
		});
	}

	public boolean canSearchAgain()
	{
		return this.lastFoundRow >= 0;
	}

	public void saveColumnSizes()
	{
		TableColumnModel colMod = this.getColumnModel();
		int count = colMod.getColumnCount();
		this.savedColumnSizes = new HashMap<String, Integer>();
		int start = 0;
		if (this.dwModel.getShowStatusColumn()) start = 1;

		for (int i=start; i < count; i++)
		{
			TableColumn col = colMod.getColumn(i);
			String name = this.getColumnName(i);
			savedColumnSizes.put(name, new Integer(col.getPreferredWidth()));
		}
	}

	public void restoreColumnSizes()
	{
		if (this.savedColumnSizes == null) return;
		for (Map.Entry<String, Integer> entry : this.savedColumnSizes.entrySet())
		{
			TableColumn col = this.getColumn(entry.getKey());
			if (col != null)
			{
				int width = entry.getValue().intValue();
				col.setPreferredWidth(width);
			}
		}
		this.savedColumnSizes = null;
	}

	private boolean useDefaultStringRenderer = true;
	
	public void setUseDefaultStringRenderer(boolean aFlag)
	{
		this.useDefaultStringRenderer = aFlag;
	}
	
	public boolean getUseDefaultStringRenderer() { return this.useDefaultStringRenderer; }

	private void initDateRenderers()
	{
		Settings sett = Settings.getInstance();
		
		String format = sett.getDefaultDateFormat();
		this.setDefaultRenderer(java.sql.Date.class, RendererFactory.getDateRenderer(format));
		this.setDefaultRenderer(java.util.Date.class, RendererFactory.getDateRenderer(format));

		format = sett.getDefaultTimestampFormat();
		this.setDefaultRenderer(java.sql.Timestamp.class, RendererFactory.getDateRenderer(format));
		format = sett.getDefaultTimeFormat();
		this.setDefaultRenderer(java.sql.Time.class, RendererFactory.getDateRenderer(format));
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.getInstance().isDateFormatProperty(evt.getPropertyName()))
		{
			initDateRenderers();
		}
	}

	protected void createDefaultRenderers()
	{
		defaultRenderersByColumnClass = new UIDefaults();
		initDefaultRenderers();
	}

	private boolean checkForBlobs = false;
	
	public TableCellRenderer getCellRenderer(int row, int column) 
	{
		TableCellRenderer rend = null;
		
		if (this.checkForBlobs && isBlobColumn(column)) 
		{
			rend = RendererFactory.getBlobRenderer();
		}
		else 
		{
			rend = super.getCellRenderer(row,column);
		}
		// in some cases getCellRenderer does not seem to return
		// a renderer at all. This is a fallback to prevent a null
		// value beeing passed to prepareCellRenderer()
		if (rend == null) rend = getDefaultRenderer(Object.class);
		return rend;
	}

	public boolean isBlobColumn(int column)
	{
		if (this.dwModel == null) return false;
		int type = this.dwModel.getColumnType(column);
		if (SqlUtil.isBlobType(type))// || java.sql.Blob.class.isAssignableFrom(cl))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Initialize the default renderers for this table
	 * @see workbench.gui.renderer.RendererFactory
	 */
	public void initDefaultRenderers()
	{
		// need to let JTable do some initialization stuff
		// otherwise setDefaultRenderer() bombs out with a NullPointerException
		if (this.defaultRenderersByColumnClass == null) createDefaultRenderers();
		initDateRenderers();

		Settings sett = Settings.getInstance();
		int maxDigits = sett.getMaxFractionDigits();
		char sep = sett.getDecimalSymbol().charAt(0);

		TableCellRenderer numberRenderer = RendererFactory.createNumberRenderer(maxDigits, sep);

		this.setDefaultRenderer(Object.class, RendererFactory.getTooltipRenderer());
		
		this.setDefaultRenderer(byte[].class, RendererFactory.getBlobRenderer());
		
		this.setDefaultRenderer(Number.class, numberRenderer);
		this.setDefaultRenderer(Double.class, numberRenderer);
		this.setDefaultRenderer(Float.class, numberRenderer);
		this.setDefaultRenderer(BigDecimal.class, numberRenderer);

		TableCellRenderer intRenderer = RendererFactory.getIntegerRenderer();
		this.setDefaultRenderer(BigInteger.class, intRenderer);
		this.setDefaultRenderer(Integer.class, intRenderer);

		// If useDefaultStringRenderer is set to false
		// a specialized renderer is already supplied for character
		// columns. Currently this is only used in the "Search Tables"
		// display to provide a renderer that highlights values where the
		// search value was found
		if (this.useDefaultStringRenderer)
		{
			this.setDefaultRenderer(String.class, RendererFactory.getStringRenderer());
			initMultiLineRenderer();
		}
	}

	private void initMultiLineRenderer()
	{
		if (!this.useDefaultStringRenderer) return;
		
		if (this.dwModel != null) 
		{
			TableColumnModel colMod = this.getColumnModel();
			for (int i=0; i < colMod.getColumnCount(); i++)
			{
				TableColumn col = colMod.getColumn(i);
				if (col == null) continue;
				if (isMultiLineColumn(i))
				{
					col.setCellRenderer(this.multiLineRenderer);
				}
			}
		}
	}
	private boolean isMultiLineColumn(int col)
	{
		if (this.dwModel == null) return false;

		int offset = (this.dwModel.getShowStatusColumn() ? 1 : 0);
		int sqlType = this.dwModel.getColumnType(col);
		int charLength = 0;
		
		if (SqlUtil.isClobType(sqlType))
		{
			charLength = Integer.MAX_VALUE;
		}
		else if (SqlUtil.isCharacterType(sqlType))
		{
			charLength = this.dwModel.getDataStore().getResultInfo().getColumn(col - offset).getColumnSize();
		}
		else 
		{
			return false;
		}
		
		int sizeThreshold = Settings.getInstance().getIntProperty("workbench.gui.display.multilinethreshold", 500);
		return charLength > sizeThreshold;
	}
	
	protected void initDefaultEditors()
	{
		Exception e = new Exception();
		TableColumnModel colMod = this.getColumnModel();
		
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			if (col == null) continue;
			Class clz = null;
			
			if (this.dwModel != null) 
			{
				clz = this.dwModel.getColumnClass(i);
			}
			if (clz != null && Number.class.isAssignableFrom(clz))
			{
				col.setCellEditor(this.defaultNumberEditor);
			}
			else if (this.dwModel != null && isBlobColumn(i))
			{
				col.setCellEditor((TableCellEditor)RendererFactory.getBlobRenderer());
			}
			else if (isMultiLineColumn(i))
			{
				col.setCellEditor(this.multiLineEditor);
			}
			else
			{
				col.setCellEditor(this.defaultEditor);
			}
		}
	}

	/**
	 * Calls adjustOrOptimizeColumns(true).
	 * 
	 * @see #adjustOrOptimizeColumns(boolean)
	 */
	public void adjustOrOptimizeColumns()
	{
		adjustOrOptimizeColumns(true);
	}
	
	/**
	 * Enhance the column width display.
	 * 
	 * If the user chose to automatically optimize the column
	 * width according to the content, this will 
	 * call {@link #optimizeAllColWidth()}
	 * otherwise this will call {@link #adjustColumns()}
	 * 
	 * @param checkHeaders to be passed to optimizeAllColWidth() in case the column width should be optimized
	 */
	public void adjustOrOptimizeColumns(boolean checkHeaders)
	{
		if (Settings.getInstance().getAutomaticOptimalWidth())
		{
			optimizeAllColWidth(checkHeaders);
		}
		else
		{
			adjustColumns();
		}
	}
	
	/**
	 * Adjusts the columns to the width defined from the 
	 * underlying tables (i.e. getColumnWidth() for each column)
	 * This does not adjust the width of the columns to the content.
	 * 
	 * @see #optimizeAllColWidth()
	 */
	public void adjustColumns()
	{
		if (this.getModel() == null) return;
		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.getColumnModel();
		if (colMod == null) return;

		int minWidth = Settings.getInstance().getMinColumnWidth();
		int maxWidth = Settings.getInstance().getMaxColumnWidth();
		
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			int addWidth = this.getAdditionalColumnSpace();
			int addHeaderWidth = this.getAdditionalColumnSpace();

			if (this.dwModel != null)
			{
				int lblWidth = 0;
				if (this.adjustToColumnLabel)
				{
					String s = this.dwModel.getColumnName(i);
					lblWidth = fm.stringWidth(s) + addHeaderWidth;
				}
				int width = (this.dwModel.getColumnWidth(i) * charWidth) + addWidth;
				int w = Math.max(width, lblWidth);
				if (maxWidth > 0) w	= Math.min(w, maxWidth);
				if (minWidth > 0) w = Math.max(w, minWidth);
				col.setPreferredWidth(w);
			}
		}
	}

	public void optimizeAllColWidth()
	{
		this.optimizeAllColWidth(Settings.getInstance().getMinColumnWidth(), Settings.getInstance().getMaxColumnWidth(), Settings.getInstance().getIncludeHeaderInOptimalWidth());
	}

	public void optimizeAllColWidth(boolean respectColName)
	{
		this.optimizeAllColWidth(Settings.getInstance().getMinColumnWidth(), Settings.getInstance().getMaxColumnWidth(), respectColName);
	}

	public void optimizeAllColWidth(int minWidth, int maxWidth, boolean respectColName)
	{
		int count = this.getColumnCount();
		for (int i=0; i < count; i++)
		{
			this.optimizeColWidth(i, minWidth, maxWidth, respectColName);
		}
	}

	public void optimizeColWidth(int aColumn)
	{
		this.optimizeColWidth(aColumn, 0, -1, false);
	}

	public void optimizeColWidth(int aColumn, boolean respectColName)
	{
		this.optimizeColWidth(aColumn, Settings.getInstance().getMinColumnWidth(), Settings.getInstance().getMaxColumnWidth(), respectColName);
	}

	public void optimizeColWidth(int aColumn, int minWidth, int maxWidth)
	{
		this.optimizeColWidth(aColumn, minWidth, maxWidth, false);
	}

	public void optimizeColWidth(int aColumn, int minWidth, int maxWidth, boolean respectColumnName)
	{
		if (this.dwModel == null) return;
		if (aColumn < 0 || aColumn > this.getColumnCount() - 1) return;

		Font f = this.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		TableColumnModel colMod = this.getColumnModel();
		TableColumn col = colMod.getColumn(aColumn);
		int addWidth = this.getAdditionalColumnSpace();
		String s = null;
		int stringWidth = 0;
		int optWidth = minWidth;

		if (respectColumnName)
		{
			s = this.dwModel.getColumnName(aColumn);
			stringWidth = fm.stringWidth(s) + 5;
			optWidth = Math.max(optWidth, stringWidth + addWidth);
		}
		int rowCount = this.getRowCount();

		for (int row = 0; row < rowCount; row ++)
		{
			TableCellRenderer rend = this.getCellRenderer(row, aColumn);
			Component c = rend.getTableCellRendererComponent(this, getValueAt(row, aColumn), false, false, row, aColumn);
			if (c instanceof WbRenderer)
			{
				s = ((WbRenderer)c).getDisplayValue();
			}
			else if (c instanceof JLabel)
			{
				// DefaultCellRenderer is a JLabel
				s = ((JLabel)c).getText();
			}
			else
			{
				s = this.getValueAsString(row, aColumn);
			}
			
			if (s == null || s.length() == 0)
				stringWidth = 0;
			else
				stringWidth = fm.stringWidth(s);
			
			optWidth = Math.max(optWidth, stringWidth + addWidth);
		}
		if (maxWidth > 0)
		{
			optWidth = Math.min(optWidth, maxWidth);
		}
		if (optWidth > 0)
		{
			col.setPreferredWidth(optWidth);
		}
	}

	private int getAdditionalColumnSpace()
	{
		int addWidth = this.getIntercellSpacing().width * 2;
		if (this.getShowVerticalLines()) addWidth += 4;

		return addWidth;
	}

	public void cancelEditing()
	{
		if (this.isEditing())
		{
			CellEditor editor = this.getCellEditor();
			if(editor != null)
			{
				editor.cancelCellEditing();
			}
		}
	}

	public boolean stopEditing()
	{
		if (!this.isEditing()) return false;
		CellEditor editor = this.getCellEditor();
		if(editor != null)
		{
			return editor.stopCellEditing();
		}
		
		return false;
	}

	public void openEditWindow()
	{
		if (!this.isEditing()) return;

		int col = this.getEditingColumn();
		int row = this.getEditingRow();
		String data = null;
		TableCellEditor editor = this.getCellEditor();
		
		if (editor instanceof WbTextCellEditor)
		{
			WbTextCellEditor wbeditor = (WbTextCellEditor)editor;
			if (this.isEditing() && wbeditor.isModified())
			{
				data = wbeditor.getText();
			}
			else
			{
				data = this.getValueAsString(row, col);
			}
		}
		else
		{
			data = (String)editor.getCellEditorValue();
		}
		
		Window owner = SwingUtilities.getWindowAncestor(this);
		Frame ownerFrame = null;
		if (owner instanceof Frame)
		{
			ownerFrame = (Frame)owner;
		}
		String title = ResourceMgr.getString("TxtEditWindowTitle");
		EditWindow w = new EditWindow(ownerFrame, title, data);
		w.setVisible(true);
		if (editor != null)
		{
			// we need to "cancel" the editor so that the data
			// in the editor component will not be written into the
			// table model!
			editor.cancelCellEditing();
		}
		if (!w.isCancelled())
		{
			this.setValueAt(w.getText(), row, col);
		}
		w.dispose();
	}

	public void addTableModelListener(TableModelListener aListener)
	{
		if (this.changeListener.add(aListener))
		{
			if (this.dwModel != null) this.dwModel.addTableModelListener(aListener);
		}
	}

	public void removeTableModelListener(TableModelListener aListener)
	{
		this.changeListener.remove(aListener);
		if (this.dwModel != null) this.dwModel.removeTableModelListener(aListener);
	}

	public int getFirstVisibleRow()
	{
		if (this.getRowCount() == 0) return -1;
		Point p = this.scrollPane.getViewport().getViewPosition();
		int row = this.rowAtPoint(p);
		return row;
	}

	public int getLastVisibleRow()
	{
		return this.getLastVisibleRow(this.getFirstVisibleRow());
	}

	public int getLastVisibleRow(int first)
	{
		int count = this.getRowCount();
		if (count == 0) return -1;

		JViewport view = this.scrollPane.getViewport();
		Point p = view.getViewPosition();
		Dimension d = view.getExtentSize();
		int height = (int)d.getHeight();
		int currentRowHeight = 0;
		int spacing = this.getRowMargin();
		int lastRow = 0;
		for (int r = first; r < count; r ++)
		{
			int h = this.getRowHeight(r) + spacing;
			if (currentRowHeight + h > height) break;
			currentRowHeight += h;
		}
		p.move(0, currentRowHeight);
		lastRow = this.rowAtPoint(p);

		// if rowAtPoint() returns a negative number, then all
		// rows fit into the current viewport
		if (lastRow < 0 || lastRow > count) lastRow = count - 1;

		return first + lastRow;
	}

	/** 
	 * Scroll the given row into view.
	 */
	public void scrollToRow(int aRow)
	{
		Rectangle rect = this.getCellRect(aRow, 1, true);
		this.scrollRectToVisible(rect);
	}

	/**
	 * Start sorting if the column header has been clicked.
	 * @param e the MouseEvent triggering the click
	 */
	public void mouseClicked(MouseEvent e)
	{
	
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (e.getSource() instanceof JTableHeader)
			{
				this.headerPopupX = e.getX();
				this.headerPopup.show(this.getTableHeader(), e.getX(), e.getY());
			}
			else if (this.showPopup && this.popup != null)
			{
				final int row = this.rowAtPoint(e.getPoint());
				int selected = this.getSelectedRowCount();
				if (selected <= 1 && row >= 0 && this.selectOnRightButtonClick)
				{
					int selectedRow = this.getSelectedRow();
					if (row != selectedRow)
					{
						final ListSelectionModel model = this.getSelectionModel();
						EventQueue.invokeLater(new Runnable()
						{
							public void run()
							{
								model.setSelectionInterval(row, row);
							}
						});
					}
				}	
				final int x = e.getX();
				final int y = e.getY();

				EventQueue.invokeLater(new Runnable()
				{
					public void run()
					{
						popup.show(WbTable.this, x,y);
					}
				});
			}
		}
		else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1
		         && this.dwModel != null && e.getSource() instanceof JTableHeader)
		{
			TableColumnModel colMod = this.getColumnModel();
			int viewColumn = colMod.getColumnIndexAtX(e.getX());
			int realColumn = this.convertColumnIndexToModel(viewColumn);
			boolean addSortColumn = e.isControlDown(); //(e.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
			if (realColumn >= 0)
			{
				if (e.isShiftDown())
				{
					this.dwModel.removeSortColumn(realColumn);
					this.repaint();
				}
				else
				{
					this.dwModel.sortInBackground(this, realColumn, addSortColumn);
				}
			}
		}
	}

	/** Invoked when the mouse enters a component.
	 *
	 */
	public void mouseEntered(MouseEvent e)
	{
	}

	/** Invoked when the mouse exits a component.
	 *
	 */
	public void mouseExited(MouseEvent e)
	{
	}

	/** Invoked when a mouse button has been pressed on a component.
	 *
	 */
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == e.BUTTON1)
		{
			boolean altDown = ((e.getModifiersEx() & e.ALT_DOWN_MASK) == e.ALT_DOWN_MASK);
			this.setColumnSelectionAllowed(altDown);
		}
	}

	/** Invoked when a mouse button has been released on a component.
	 *
	 */
	public void mouseReleased(MouseEvent e)
	{
	}

	public void setColumnWidth(int column, int width)
	{
		TableColumn col = this.getColumnModel().getColumn(column);
		if (width > 0 && col != null)
		{
			col.setWidth(width);
			col.setPreferredWidth(width);
		}
	}

	public int getPopupColumnIndex()
	{
		TableColumnModel colMod = this.getColumnModel();
		int viewColumn = colMod.getColumnIndexAtX(this.headerPopupX);
		int column = this.convertColumnIndexToModel(viewColumn);
		return column;
	}
	
	public void actionPerformed(ActionEvent e)
	{
		int column = getPopupColumnIndex();
		if (e.getSource() == this.sortAscending && this.dwModel != null)
		{
			this.dwModel.sortInBackground(this, column, true, false);
		}
		else if (e.getSource() == this.sortDescending && this.dwModel != null)
		{
			this.dwModel.sortInBackground(this, column, false, false);
		}
		else if (e.getSource() == this.setColWidth)
		{
			try
			{
				TableColumn col = this.getColumnModel().getColumn(column);
				int colWidth = col.getWidth();
				String s = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("MsgEnterNewColWidth"), Integer.toString(colWidth));
				if (s != null)
				{
					try { colWidth = Integer.parseInt(s); } catch (Exception ex) { colWidth = -1; }
					if (colWidth > 0)
					{
						col.setWidth(colWidth);
						col.setPreferredWidth(colWidth);
					}
				}
			}
			catch (Exception ex)
			{
			}
		}
	}

	public void resetPopup()
	{
		if (this.popup != null) this.popup.setVisible(false);
		this.headerPopupX = -1;
		this.popup = null;
	}

	/**
	 *	Display dialog window to let the user
	 *	select the key columns for the current update table
	 *
	 *	@return true if the user selected OK, false if the user cancelled the dialog
	 */
	public boolean selectKeyColumns()
	{
		DataStore ds = this.getDataStore();
		ColumnIdentifier[] originalCols = ds.getColumns();
		TableIdentifier table = ds.getUpdateTable();
		if (table == null) 
		{
			Window w = SwingUtilities.getWindowAncestor(this);
			WbSwingUtilities.showErrorMessageKey(w, "MsgNoUpdateTable");
			return false;
		}
		
		KeyColumnSelectorPanel panel = new KeyColumnSelectorPanel(originalCols, table);
		Window parent = SwingUtilities.getWindowAncestor(this);
		boolean selected = ValidatingDialog.showConfirmDialog(parent, panel, ResourceMgr.getString("MsgSelectKeyColumnsWindowTitle"), null, 0, true);
		
		if (selected)
		{
			ColumnIdentifier[] cols = panel.getColumns();
			ds.setPKColumns(cols);
			checkCopyActions();
			if (panel.getSaveToGlobalPKMap())
			{
				PkMapping.getInstance().addMapping(table, cols);
				FileDialogUtil.selectPkMapFileIfNecessary(parent);
			}
			
			return true;
		}
		return false;
	}

	public boolean detectDefinedPkColumns()
	{
		DataStore ds = this.getDataStore();
		if (ds == null) return false;
		if (ds.hasPkColumns()) return true;
		try
		{
			if (this.statusBar != null) statusBar.setStatusMessage(ResourceMgr.getString("MsgRetrievingKeyColumns"));
			ds.updatePkInformation();
			if (this.statusBar != null) statusBar.clearStatusMessage();
		}
		catch (Exception e)
		{
			LogMgr.logError("WbTable.detectDefinedPkColumns()", "Could not read PK columns", e);
			return false;
		}
		return ds.hasPkColumns();
	}

	/**
	 * Enables the actions related to copying data to the clipboard
	 * if this table contains rows. If this table is empty
	 * the actions are disabled
	 */
	public void checkCopyActions()
	{
		boolean hasRows = getRowCount() > 0;
		
		if (this.copyInsertAction != null)
		{
			this.copyInsertAction.setEnabled(hasRows);
		}

		if (this.copyUpdateAction != null)
		{
			this.copyUpdateAction.setEnabled(hasRows);
		}

		if (this.copyDeleteInsertAction != null)
		{
			this.copyDeleteInsertAction.setEnabled(hasRows);
		}
	}

	/**
	 * Checks if the underlying DataStore has PrimaryKey columns defined.
	 * @return true, if the DataStore has PK columns. false if no PKs defined or no DataStore attached to this Table
	 * @see workbench.storage.DataStore#hasPkColumns()
	 */
	public boolean hasPkColumns()
	{
		DataStore ds = this.getDataStore();
		if (ds == null) return false;
		return ds.hasPkColumns();
	}
	
	/**
	 *	Check for any defined PK columns.
	 *	If no key columns can be found, the user
	 *  is prompted for the key columns
	 *
	 *  @param promptWhenNeeded if true, the user is asked to supply PK columns if none were found
	 *  @return true, if primary key columns where found (or selected by the user) for the underlying table.
	 * 
	 *	@see #detectDefinedPkColumns()
	 *	@see #selectKeyColumns()
	 */
	public boolean checkPkColumns(boolean promptWhenNeeded)
	{
		DataStore ds = this.getDataStore();
		if (ds == null) return false;

		boolean hasPK = detectDefinedPkColumns();
		boolean pkColumnsComplete = ds.pkColumnsComplete();
		
		if (hasPK && pkColumnsComplete) return true;

		if (promptWhenNeeded)
		{
			if (hasPK && !pkColumnsComplete)
			{
				hasPK = WbSwingUtilities.getYesNo(this, ResourceMgr.getString("MsgIgnoreMissingPK"));
			}
			else
			{
				hasPK = this.selectKeyColumns();
			}
		}
		if (!hasPK)
		{
			LogMgr.logWarning("WbTable.checkPkColumns()", "Could not find key columns for updating table " + ds.getUpdateTable());
		}
		return hasPK;
	}

	public void fontChanged(String aFontId, Font newFont)
	{
		if (aFontId.equals(Settings.PROPERTY_DATA_FONT))
		{
			this.setFont(newFont);
			this.getTableHeader().setFont(newFont);
			this.defaultEditor.setFont(newFont);
			this.numberEditorTextField.setFont(newFont);
		}
	}

	public void focusGained(FocusEvent e)
	{
	}

	public void focusLost(FocusEvent e)
	{
		this.stopEditing();
	}

	public long addRow()
	{
		DataStoreTableModel ds = this.getDataStoreTableModel();
		if (ds == null) return -1;

		int selectedRow = this.getSelectedRow();
		final int newRow;

		this.stopEditing();

		if (selectedRow == -1)
		{
			newRow = ds.addRow();
		}
		else
		{
			newRow = ds.insertRow(selectedRow);
		}
		this.getSelectionModel().setSelectionInterval(newRow, newRow);
		this.scrollToRow(newRow);
		this.setEditingRow(newRow);
		if (this.dwModel.getShowStatusColumn())
		{
			this.setEditingColumn(1);
			this.editCellAt(newRow, 1);
		}
		else
		{
			this.setEditingColumn(0);
			this.editCellAt(newRow, 0);
		}

		final Component edit = this.getEditorComponent();
		if (edit != null)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					edit.requestFocusInWindow();
				}
			});
		}
		return newRow;
	}

	public int duplicateRow()
	{
		DataStoreTableModel model = this.getDataStoreTableModel();
		if (model == null) return -1;
		if (this.getSelectedRowCount() != 1) return -1;
		int row = this.getSelectedRow();
		int newRow = model.duplicateRow(row);
		return newRow;
	}

	public boolean deleteRow()
	{
		DataStoreTableModel ds = this.getDataStoreTableModel();
		if (ds == null) return false;

		int[] selectedRows = this.getSelectedRows();
		int numRows = selectedRows.length;
		if (numRows > 0)
		{
			for (int i = numRows - 1; i >= 0; i--)
			{
				ds.deleteRow(selectedRows[i]);
			}
		}
		return true;
	}

	public boolean isHighlightRequiredFields()
	{
		return highlightRequiredFields;
	}

	public void setHighlightRequiredFields(boolean flag)
	{
		this.highlightRequiredFields = flag;
		if (flag && this.requiredColor == null)
		{
			requiredColor = Settings.getInstance().getRequiredFieldColor();
		}
		else if (!flag)
		{
			requiredColor = null;
		}
	}

}
