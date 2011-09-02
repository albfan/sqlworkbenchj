/*
 * WbTable.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import workbench.gui.fontzoom.FontZoomer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
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
import javax.swing.JMenu;
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
import javax.swing.JPopupMenu.Separator;
import javax.swing.KeyStroke;
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
import workbench.gui.actions.CopyColumnNameAction;
import workbench.gui.actions.DisplayDataFormAction;
import workbench.gui.actions.FilterDataAction;
import workbench.gui.actions.ResetFilterAction;
import workbench.gui.actions.OptimizeAllColumnsAction;
import workbench.gui.actions.OptimizeColumnWidthAction;
import workbench.gui.actions.OptimizeRowHeightAction;
import workbench.gui.actions.PrintAction;
import workbench.gui.actions.PrintPreviewAction;
import workbench.gui.actions.ResetColOrderAction;
import workbench.gui.actions.ResetHighlightAction;
import workbench.gui.actions.SaveColOrderAction;
import workbench.gui.actions.SaveDataAsAction;
import workbench.gui.actions.ScrollToColumnAction;
import workbench.gui.actions.SetColumnWidthAction;
import workbench.gui.actions.SortAscendingAction;
import workbench.gui.actions.SortDescendingAction;
import workbench.gui.actions.WbAction;
import workbench.gui.fontzoom.DecreaseFontSize;
import workbench.gui.fontzoom.FontZoomProvider;
import workbench.gui.fontzoom.IncreaseFontSize;
import workbench.gui.fontzoom.ResetFontSize;
import workbench.gui.renderer.RendererFactory;
import workbench.gui.renderer.RendererSetup;
import workbench.gui.renderer.RequiredFieldHighlighter;
import workbench.gui.renderer.RowStatusRenderer;
import workbench.gui.sql.DwStatusBar;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.ListSelectionControl;
import workbench.interfaces.Resettable;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.PlatformShortcuts;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataConverter;
import workbench.storage.DataStore;
import workbench.storage.PkMapping;
import workbench.storage.ResultInfo;
import workbench.storage.RowDataFactory;
import workbench.storage.filter.ColumnExpression;
import workbench.storage.filter.FilterExpression;
import workbench.util.FileDialogUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class WbTable
	extends JTable
	implements ActionListener, MouseListener, FontChangedListener, ListSelectionListener, PropertyChangeListener,
						 Resettable, FontZoomProvider
{
	// <editor-fold defaultstate="collapsed" desc=" Variables ">
	protected JPopupMenu popup;

	private DataStoreTableModel dwModel;
	private int lastFoundRow = -1;

	private WbTextCellEditor defaultEditor;
	private WbCellEditor multiLineEditor;
	private TableCellRenderer multiLineRenderer;
	private TableCellRenderer sortRenderer;

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

	private ResetHighlightAction resetHighlightAction;

	private ColumnExpression highlightExpression;

	private FilterDataAction filterAction;
	private ResetFilterAction resetFilterAction;

	private PrintAction printDataAction;
	private PrintPreviewAction printPreviewAction;

	private boolean adjustToColumnLabel;

	private int headerPopupX = -1;
	private Map<String, Integer> savedColumnSizes;

	private RowHeightResizer rowResizer;
	private List<TableModelListener> changeListener = new ArrayList<TableModelListener>();
	private JScrollPane scrollPane;

	private DwStatusBar statusBar;

	private String defaultPrintHeader;
	private boolean showPopup = true;
	private boolean selectOnRightButtonClick;
	private boolean highlightRequiredFields;
	private boolean useMultilineTooltip = true;
	private boolean rowHeightWasOptimized;
	private Color requiredColor;
	private boolean allowColumnOrderSaving;

	private boolean showFocusPending;
	private FocusIndicator focusIndicator;
	private ListSelectionControl selectionController;
	private boolean readOnly;
	private FontZoomer zoomer;

	private RendererSetup rendererSetup;
	private Font printFont;

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

		this.rendererSetup = new RendererSetup();
		this.sortAscending = new SortAscendingAction(this);
		this.sortAscending.setEnabled(false);
		this.sortDescending = new SortDescendingAction(this);
		this.sortDescending.setEnabled(false);
		this.optimizeCol = new OptimizeColumnWidthAction(this);
		this.optimizeAllCol = new OptimizeAllColumnsAction(this);
		this.optimizeAllCol.setEnabled(true);
		this.setColWidth = new SetColumnWidthAction(this);
		this.setAutoCreateColumnsFromModel(true);

		this.defaultEditor = WbTextCellEditor.createInstance(this);

		// Create a separate editor for numbers that is right aligned
		numberEditorTextField = new JTextField();
		numberEditorTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		this.defaultNumberEditor = new WbTextCellEditor(this, numberEditorTextField);

		Font dataFont = Settings.getInstance().getDataFont();
		if (dataFont != null)
		{
			defaultEditor.setFont(dataFont);
			numberEditorTextField.setFont(dataFont);
			super.setFont(dataFont);
		}

		this.multiLineEditor = new WbCellEditor(this);
		this.multiLineRenderer = RendererFactory.getMultiLineRenderer();
		this.sortRenderer = RendererFactory.getSortHeaderRenderer();

		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		this.replacer = new TableReplacer(this);

		this.copyAsTextAction = new CopyAsTextAction(this);
		this.saveDataAsAction = new SaveDataAsAction(this);

		this.saveDataAsAction.setEnabled(true);

		this.filterAction = new FilterDataAction(this);
		this.resetFilterAction = new ResetFilterAction(this);

		this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.addPopupAction(this.saveDataAsAction, false);
		this.addPopupAction(this.copyAsTextAction, true);


		if (sqlCopyAllowed)
		{
			this.copyInsertAction = new CopyAsSqlInsertAction(this);
			this.copyDeleteInsertAction = new CopyAsSqlDeleteInsertAction(this);
			this.copyUpdateAction = new CopyAsSqlUpdateAction(this);

			this.addPopupAction(this.copyUpdateAction, false);
			this.addPopupAction(this.copyInsertAction, false);
			this.addPopupAction(this.copyDeleteInsertAction, false);

			WbMenu copy = this.getCopySelectedMenu();
			this.addPopupSubMenu(copy, true);
		}
		else
		{
			this.copySelectedAsTextAction = new CopySelectedAsTextAction(this, "MnuTxtCopySelectedAsTextSingle");
			this.addPopupAction(this.copySelectedAsTextAction, false);
		}

		this.addPopupAction(this.replacer.getFindAction(), true);
		this.addPopupAction(this.replacer.getFindAgainAction(), false);
		this.resetHighlightAction = new ResetHighlightAction(this);

		if (replaceAllowed)
		{
			this.addPopupAction(this.replacer.getReplaceAction(), false);
		}

		this.addPopupAction(resetHighlightAction, false);

		if (printEnabled)
		{
			this.printDataAction = new PrintAction(this);
			this.printPreviewAction = new PrintPreviewAction(this);
			this.popup.addSeparator();
			this.popup.add(this.printDataAction);
			this.popup.add(this.printPreviewAction);
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
		Settings.getInstance().registerDateFormatChangeListener(this);

		this.initDefaultRenderers();
		this.initDefaultEditors();

		Action a = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				stopEditing();
			}
		};

		this.getInputMap(WHEN_FOCUSED).put(WbSwingUtilities.ENTER, "wbtable-stop-editing");
		this.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(WbSwingUtilities.ENTER, "wbtable-stop-editing");
		this.getActionMap().put("wbtable-stop-editing", a);
		zoomer = new FontZoomer(this);
		IncreaseFontSize inc = new IncreaseFontSize(zoomer);
		inc.addToInputMap(im, am);

		DecreaseFontSize dec = new DecreaseFontSize(zoomer);
		dec.addToInputMap(im, am);

		ResetFontSize reset = new ResetFontSize(zoomer);
		reset.addToInputMap(im, am);
	}

	@Override
	public FontZoomer getFontZoomer()
	{
		return zoomer;
	}

	public void setReadOnly(boolean flag)
	{
		readOnly = flag;
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public Font getPrintFont()
	{
		return printFont;
	}

	public void setPrintFont(Font font)
	{
		this.printFont = font;
	}

	public RendererSetup getRendererSetup()
	{
		return rendererSetup;
	}

	public void setRendererSetup(RendererSetup newSetup)
	{
		rendererSetup = newSetup;
	}

	public void setModifiedColor(Color color)
	{
		this.rendererSetup.setModifiedColor(color);
	}

	public void setColumnOrderSavingEnabled(boolean flag)
	{
		allowColumnOrderSaving = flag;
	}

	public void showInputFormAction()
	{
		if (this.popup == null) return;

		DisplayDataFormAction formAction = new DisplayDataFormAction(this);
		popup.insert(formAction.getMenuItem(), 0);
		formAction.addToInputMap(this);
	}

	public void setListSelectionControl(ListSelectionControl controller)
	{
		this.selectionController = controller;
	}

	@Override
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend)
	{
		boolean canChange = true;
		if (selectionController != null)
		{
			canChange = selectionController.canChangeSelection();
		}
		if (canChange)
		{
			super.changeSelection(rowIndex, columnIndex, toggle, extend);
		}
	}

	public void showFocusBorder()
	{
		if (this.scrollPane == null)
		{
			this.showFocusPending = true;
		}
		else
		{
			if (this.focusIndicator != null)
			{
				this.focusIndicator.dispose();
			}
			else
			{
				this.focusIndicator = new FocusIndicator(this, scrollPane);
			}
		}
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
	@Override
	public void setFont(Font f)
	{
		super.setFont(f);
		if (tableHeader != null)
		{
			tableHeader.setFont(f);
		}
		if (defaultEditor != null)
		{
			defaultEditor.setFont(f);
		}
		if (multiLineEditor != null)
		{
			multiLineEditor.setFont(f);
		}

		if (defaultNumberEditor != null)
		{
			defaultNumberEditor.setFont(f);
		}
		adjustRowsAndColumns();
	}

	public void adjustRowHeight()
	{
		if (rowHeightWasOptimized || GuiSettings.getAutomaticOptimalRowHeight())
		{
			optimizeRowHeight();
		}
		else
		{
			calculateGlobalRowHeight();
		}
	}

	@SuppressWarnings("deprecation")
	private void calculateGlobalRowHeight()
	{
		Font f = getFont();
		if (f == null) return;

		Graphics g = getGraphics();

		// Depending on the stage of initialization
		// not all calls work the same, so we need
		// to take care that no exception gets thrown in here
		FontMetrics fm = null;
		if (g != null)
		{
			try
			{
				fm = g.getFontMetrics(f);
			}
			catch (Throwable e)
			{
				fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
			}
		}
		else
		{
			fm = getFontMetrics(f);
		}

		if (fm != null)
		{
			setRowHeight(fm.getHeight() + 2);
			final TableRowHeader header = TableRowHeader.getRowHeader(this);
			if (header != null)
			{
				header.rowHeightChanged();
			}
		}
	}

	public void useMultilineTooltip(boolean flag)
	{
		this.useMultilineTooltip = flag;
	}

	@Override
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

	public FilterDataAction getFilterAction()
	{
		return this.filterAction;
	}

	public ResetFilterAction getResetFilterAction()
	{
		return this.resetFilterAction;
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

	public final WbMenu getCopySelectedMenu()
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

	public void dispose()
	{
		reset();
		if (this.rowResizer != null)
		{
			this.rowResizer.done();
		}
		this.rowResizer = null;
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (popup != null)
				{
					popup.removeAll();
					popup = null;
				}
			}
		});
	}

	@Override
	public void reset()
	{
		this.cancelEditing();
		this.rowHeightWasOptimized = false;
		TableRowHeader.removeRowHeader(this);
		if (this.getModel() == EmptyTableModel.EMPTY_MODEL) return;
		this.setModel(EmptyTableModel.EMPTY_MODEL, false);
	}

	private void addPopupSubMenu(final WbMenu submenu, final boolean withSep)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (popup == null) popup = new JPopupMenu();
				if (withSep)
				{
					popup.addSeparator();
				}
				popup.add(submenu);
			}
		});
	}

	private int findPopupItem(WbAction reference)
	{
		if (reference == null) return -1;

		int count = popup.getComponentCount();
		for (int i=0; i < count; i++)
		{
			Component item = popup.getComponent(i);
			if (item instanceof JMenuItem)
			{
				JMenuItem menu = (JMenuItem)item;
				if (menu.getAction() == reference) return i;
			}
		}
		return -1;
	}

	public void addPopupActionAfter(final WbAction action, final WbAction reference)
	{
		if (popup == null) return;
		final int index = findPopupItem(reference);
		if (index == -1) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				popup.add(action.getMenuItem(), index + 1);
			}
		});
	}

	public final void addPopupAction(final WbAction anAction, final boolean withSep)
	{
		addPopupMenu(anAction.getMenuItem(), withSep);
	}

	public void addPopupMenu(final JMenuItem item, final boolean withSep)
	{
		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				if (popup == null) popup = new JPopupMenu();
				if (printDataAction != null)
				{
					if (withSep)
					{
						popup.add(new Separator(), popup.getComponentCount() - 3);
					}
					popup.add(item, popup.getComponentCount() - 3);
				}
				else
				{
					if (withSep) popup.addSeparator();
					popup.add(item);
				}
			}
		});
	}

	@Override
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

	@Override
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

		if (this.showFocusPending && this.scrollPane != null)
		{
			this.showFocusPending = false;
			showFocusBorder();
		}
		this.checkMouseListener();
		if (zoomer != null)
		{
			// The font zoomer must be attached to the scroll pane
			// it cannot be attached to the table!
			scrollPane.addMouseWheelListener(zoomer);
		}
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
	 * @see workbench.print.TablePrinter#setHeaderText(String)
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

	@Override
	public Color getBackground()
	{
		Color c = Settings.getInstance().getColor("workbench.gui.table.background", null);
		return (c == null ? super.getBackground() : c);
	}

	@Override
	public Color getForeground()
	{
		Color c = Settings.getInstance().getColor("workbench.gui.table.foreground", null);
		return (c == null ? super.getForeground() : c);
	}

	@Override
	public Color getSelectionBackground()
	{
		Color c = Settings.getInstance().getColor("workbench.gui.table.selection.background", null);
		return (c == null ? super.getSelectionBackground() : c);
	}

	@Override
	public Color getSelectionForeground()
	{
		Color c = Settings.getInstance().getColor("workbench.gui.table.selection.foreground", null);
		return (c == null ? super.getSelectionForeground() : c);
	}

	@Override
	protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed)
	{
		boolean result = true;

		try
		{
			int ctrl = PlatformShortcuts.getDefaultModifier();
			// Don't start when non-printing keys are typed.
			// Keystrokes like Alt-F4 should not automatically start editing mode
			int code = e.getModifiers();
			boolean modifierKeyPressed = ((code & KeyEvent.ALT_MASK) == KeyEvent.ALT_MASK || (code & ctrl) == ctrl);
			if (modifierKeyPressed)
			{
				// temporarily disable auto-editing
				putClientProperty("JTable.autoStartsEdit", Boolean.FALSE);
			}
			result = super.processKeyBinding(ks, e, condition, pressed);
		}
		finally
		{
			putClientProperty("JTable.autoStartsEdit", Boolean.TRUE);
		}
		return result;
	}

	@Override
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

	public void selectCell(int row, int col)
	{
		scrollToRow(row);
		Rectangle rect = getCellRect(row,col,true);
		setColumnSelectionAllowed(true);
		scrollRectToVisible(rect);
		setRowSelectionInterval(row, row);
		setColumnSelectionInterval(col, col);
	}

	@Override
	public boolean editCellAt(final int row, int column, final EventObject e)
	{
		boolean result = super.editCellAt(row, column, e);

		if (result)
		{
			if (this.highlightRequiredFields)
			{
				initRendererHighlight(row);
			}

			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					clearSelection();
				}
			});
		}
		return result;
	}

	protected void initRendererHighlight(int row)
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
	}

	@Override
	public void removeEditor()
	{
		final int row = this.getEditingRow();
		final int col = this.getEditingColumn();
		super.removeEditor();

		resetHighlightRenderers(row);

		requestFocusInWindow();
		// Make sure the editing column/row is selected
		changeSelection(row, -1, false, false);
		changeSelection(row, col, false, false);
	}

	private void resetHighlightRenderers(final int row)
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

	@Override
	public void setModel(TableModel aModel)
	{
		this.setModel(aModel, false);
	}

	public void setModel(TableModel aModel, boolean allowSort)
	{
		if (!SwingUtilities.isEventDispatchThread())
		{
			Exception e = new Exception("Wrong thread!");
			LogMgr.logWarning("WbTable.setModel()", "setModel() not called in EDT!", e);
		}
		removeListeners();
		rowHeightWasOptimized = false;

		JTableHeader header = this.getTableHeader();
		if (header != null)
		{
			header.removeMouseListener(this);
		}

		if (this.dwModel != null)
		{
			this.dwModel.dispose();

			// Setting the dwModel to null is important
			// because the new model might not be a DataStoreTableModel
			this.dwModel = null;
		}

		try
		{
			super.setModel(aModel);
		}
		catch (Throwable th)
		{
			LogMgr.logError("WbTable.setModel()", "Error setting table model", th);
		}

		this.currentFilter = null;

		if (aModel instanceof DataStoreTableModel)
		{
			this.dwModel = (DataStoreTableModel)aModel;
			if (allowSort && header != null)
			{
				header.addMouseListener(this);
			}
		}

		if (aModel != EmptyTableModel.EMPTY_MODEL)
		{
			if (this.sortAscending != null) this.sortAscending.setEnabled(allowSort);
			if (this.sortDescending != null) this.sortDescending.setEnabled(allowSort);

			// it seems that JTable.setModel() resets the default renderers and editors
			// so we'll have to do it again...
			this.initDefaultRenderers();
			this.initDefaultEditors();
		}
		addListeners();
		adjustRowsAndColumns();
		updateSortRenderer();
		checkCopyActions();
	}

	public boolean isColumnOrderChanged()
	{
		TableColumnModel model = getColumnModel();
		int count = model.getColumnCount();
		for (int i=0; i < count; i++)
		{
			int modelIndex = model.getColumn(i).getModelIndex();
			// Any index larger than the current count means, the column
			// has been removed, and thus is not relevant
			if (modelIndex < count)
			{
				if (i != modelIndex) return true;
			}
		}
		return false;
	}

	private FilterExpression lastFilter;
	private FilterExpression currentFilter;

	public void clearLastFilter(boolean keepGeneralFilter)
	{
		if (keepGeneralFilter && this.lastFilter != null && !this.lastFilter.isColumnSpecific()) return;
		this.lastFilter = null;
	}

	private Boolean lastRowHeaderState;

	protected boolean saveRowHeaderState()
	{
		lastRowHeaderState = Boolean.valueOf(TableRowHeader.isRowHeaderVisible(this));

		// For some reason, the row header is removed when adding or deleting a row.
		// But as it gets removed anyway, I'm removing it manually
		// in order to clean up properly the registered listeners
		if (lastRowHeaderState.booleanValue())
		{
			TableRowHeader.removeRowHeader(this);
		}
		return lastRowHeaderState.booleanValue();
	}

	protected void restoreRowHeaderState()
	{
		if (lastRowHeaderState == null) return;
		if (lastRowHeaderState.booleanValue())
		{
			TableRowHeader.showRowHeader(this);
		}
		lastRowHeaderState = null;
	}

	public void resetFilter()
	{
		this.currentFilter = null;
		if (this.dwModel == null) return;
		try
		{
			saveRowHeaderState();
			this.dwModel.resetFilter();
			adjustRowsAndColumns();
		}
		finally
		{
			restoreRowHeaderState();
		}
	}

	public FilterExpression getLastFilter()
	{
		return lastFilter;
	}

	public boolean isFiltered()
	{
		return (currentFilter != null);
	}

	public void applyFilter(FilterExpression filter)
	{
		if (dwModel == null) return;
		lastFilter = filter;
		currentFilter = filter;
		dwModel.applyFilter(filter);
		adjustRowsAndColumns();
		WbSwingUtilities.repaintLater(getParent());
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

	public Object restoreColumnValue(int row, int column)
	{
		int realColumn = column - getDataStoreTableModel().getRealColumnStart();
		return getDataStore().restoreColumnValue(row, realColumn);
	}

	/**
	 * Restores the original values in the underlying DataStore.
	 * Fires a tableDataChanged() event if values were restored.
	 *
	 * @see DataStore#restoreOriginalValues()
	 */
	public void restoreOriginalValues()
	{
		if (this.dwModel == null) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				boolean restored = dwModel.getDataStore().restoreOriginalValues();
				if (restored)
				{
					dwModel.fireTableDataChanged();
				}
			}
		});
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
		if (flag == this.dwModel.getShowStatusColumn()) return;

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				_setShowStatusColumn(flag);
			}
		});
	}

	protected void _setShowStatusColumn(boolean flag)
	{
		if (this.dwModel == null) return;

		int column = this.getSelectedColumn();
		final int row = this.getSelectedRow();

		this.saveColumnSizes();
		this.dwModel.setShowStatusColumn(flag);

		if (flag)
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
		this.adjustRowHeight();

		if (row >= 0)
		{
			//this.getSelectionModel().setSelectionInterval(row, row);
			final int newColumn;
			if (flag)
				newColumn = column + 1;
			else
				newColumn = column - 1;

			if (newColumn >= 0)
			{
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						changeSelection(row, -1, false, false);
						changeSelection(row, newColumn, false, true);
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
			@Override
			public void run()
			{
				updateSortRenderer();
				WbSwingUtilities.showWaitCursor(c.getParent());
			}
		});
	}

	protected void updateSortRenderer()
	{
		JTableHeader header = getTableHeader();
		if (header == null) return;

		TableColumnModel model = header.getColumnModel();
		if (model == null) return;

		for (int col = 0; col < model.getColumnCount(); col ++)
		{
			try
			{
				// By setting the renderer for each column seperately
				// instead of using a default renderer, we can re-use
				// the rendering done by the Look & Feel's default renderer
				TableColumn column = getColumn(getColumnName(col));
				if (column == null) continue;
				column.setHeaderRenderer(sortRenderer);
			}
			catch (Throwable th)
			{
				// just in case, because getColumn() can throw an IllegalArgumentException
			}
		}
	}

	public void sortingFinished()
	{
		final Container c = (this.scrollPane == null ? this : scrollPane);
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				adjustRowHeight();

				if (GuiSettings.getAutomaticOptimalWidth() && GuiSettings.getIncludeHeaderInOptimalWidth())
				{
					ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(WbTable.this);
					optimizer.optimizeHeader();
				}

				WbSwingUtilities.showDefaultCursor(c.getParent());
				WbSwingUtilities.showDefaultCursor(getTableHeader());

				// For some reason, the sorting indicator is not properly displayed
				// if repaint() is not called. It needs two clicks int order to
				// display the new sort icon if
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
			savedColumnSizes.put(name, Integer.valueOf(col.getPreferredWidth()));
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

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (Settings.getInstance().isDateFormatProperty(evt.getPropertyName()))
		{
			initDateRenderers();
		}
	}

	@Override
	protected void createDefaultRenderers()
	{
		defaultRenderersByColumnClass = new UIDefaults();
		initDefaultRenderers();
	}

	/**
	 * For some reason setting a default renderer for BLOB columns
	 * is not working. So getCellRenderer is overwritten, to first
	 * check for a BLOB column. If the specified column is not
	 * a BLOB column, the default handling from JTable will be used.
	 */
	@Override
	public TableCellRenderer getCellRenderer(int row, int column)
	{
		TableCellRenderer rend = null;

		if (isBlobColumn(column))
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

		if (SqlUtil.isBlobType(type))
		{
			String dbmsType = dwModel.getDbmsType(column);
			try
			{
				DataConverter conv = RowDataFactory.getConverterInstance(this.getDataStore().getOriginalConnection());
				if (conv != null)
				{
					if (conv.convertsType(type, dbmsType)) return false;
				}
				return true;
			}
			catch (Throwable th)
			{
				LogMgr.logWarning("WbTable.isBlobColumn()","Error checking for converted blob", th);
				return true;
			}
		}
		return false;
	}

	/**
	 * Initialize the default renderers for this table
	 * @see workbench.gui.renderer.RendererFactory
	 */
	private void initDefaultRenderers()
	{
		// need to let JTable do some initialization stuff
		// otherwise setDefaultRenderer() bombs out with a NullPointerException
		if (this.defaultRenderersByColumnClass == null)
		{
			defaultRenderersByColumnClass = new UIDefaults();
		}
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

	public ResetHighlightAction getResetHighlightAction()
	{
		return this.resetHighlightAction;
	}

	public boolean isHighlightEnabled()
	{
		return (highlightExpression != null);
	}

	public void clearHighlightExpression()
	{
		applyHighlightExpression(null);
	}

	public ColumnExpression getHighlightExpression()
	{
		return highlightExpression;
	}

	public void applyHighlightExpression(ColumnExpression filter)
	{
		boolean changed = (filter != null && this.highlightExpression == null) ||
			(filter == null && this.highlightExpression != null);

		this.highlightExpression = filter;
		this.resetHighlightAction.setEnabled(filter != null);

		if (changed) WbSwingUtilities.repaintLater(this);
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

		// the first column is never a multiline if the status column is displayed.
		if (col - offset < 0) return false;

		ColumnIdentifier column = this.dwModel.getDataStore().getResultInfo().getColumn(col - offset);
		return SqlUtil.isMultiLineColumn(column);
	}

	private void initDefaultEditors()
	{
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
			else if (isBlobColumn(i))
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
	 * Enhance the column width display.
	 *
	 * If the user chose to automatically optimize the column
	 * width according to the content, this will
	 * call {@link ColumnWidthOptimizer#optimizeAllColWidth()}
	 * otherwise this will call {@link ColumnWidthOptimizer#adjustColumns(boolean)}
	 *
	 * @see ColumnWidthOptimizer#optimizeAllColWidth()
	 * @see ColumnWidthOptimizer#adjustColumns(boolean)
	 */
	public void adjustRowsAndColumns()
	{
		ColumnWidthOptimizer optimizer = new ColumnWidthOptimizer(this);
		boolean checkHeaders = GuiSettings.getIncludeHeaderInOptimalWidth();
		if (GuiSettings.getAutomaticOptimalWidth())
		{
			optimizer.optimizeAllColWidth(checkHeaders);
		}
		else
		{
			optimizer.adjustColumns(this.adjustToColumnLabel);
		}
		adjustRowHeight();
	}

	public void optimizeRowHeight()
	{
		RowHeightOptimizer optimizer = new RowHeightOptimizer(this);
		optimizer.optimizeAllRows();
		rowHeightWasOptimized = true;
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
		boolean result = false;

		if (this.isEditing())
		{
			CellEditor editor = this.getCellEditor();
			if(editor != null)
			{
				result = editor.stopCellEditing();
			}
		}
		return result;
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
		try
		{
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
		}
		finally
		{
			w.dispose();
		}
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
		Rectangle rect = this.scrollPane.getViewport().getViewRect();
		Point p = new Point(0, rect.y);
		int row = this.rowAtPoint(p);
		if (row < 0) row = 0;
		return row;
	}

	/**
	 * Return the row number of the last row that is completely visible
	 *
	 * @return the last row that is completely visible
	 */
	public int getLastVisibleRow()
	{
		int count = getRowCount();
		if (count <= 0) return -1;

		Rectangle view = this.scrollPane.getViewport().getViewRect();
		Point p = new Point(0, view.y + view.height - 1);
		int lastRow = this.rowAtPoint(p);
		Rectangle r = getCellRect(lastRow, 0, true);
		if (!view.contains(r))
		{
			// if the lastRow determined by the viewPort() is not completely visible,
			// then it's not considered visible
			lastRow --;
		}
		if (lastRow <= 0 || lastRow >= count) lastRow = count - 1;
		return lastRow;
	}


	/**
	 * Scroll the given row into view.
	 */
	public void scrollToRow(int aRow)
	{
		Rectangle rect = this.getCellRect(aRow, 1, true);
		this.scrollRectToVisible(rect);
	}

	protected JPopupMenu getHeaderPopup()
	{
		JPopupMenu headerPopup = new JPopupMenu();
		headerPopup.add(sortAscending.getMenuItem());
		headerPopup.add(sortDescending.getMenuItem());
		headerPopup.addSeparator();
		headerPopup.add(optimizeCol.getMenuItem());
		headerPopup.add(optimizeAllCol.getMenuItem());
		headerPopup.add(setColWidth.getMenuItem());
		headerPopup.add(new CopyColumnNameAction(WbTable.this));
		headerPopup.addSeparator();
		headerPopup.add(new ScrollToColumnAction(WbTable.this));
		headerPopup.addSeparator();
		headerPopup.add(new OptimizeRowHeightAction(WbTable.this));
		if (allowColumnOrderSaving)
		{
			headerPopup.addSeparator();
			headerPopup.add(new ResetColOrderAction(WbTable.this));
			headerPopup.add(new SaveColOrderAction(WbTable.this));
		}
		JMenu zoom = new JMenu(ResourceMgr.getString("TxtZoom"));
		zoom.add(new JMenuItem(new IncreaseFontSize("TxtFntInc", zoomer)));
		zoom.add(new JMenuItem(new DecreaseFontSize("TxtFntDecr", zoomer)));
		zoom.addSeparator();
		zoom.add(new JMenuItem(new ResetFontSize("TxtFntReset", zoomer)));

		headerPopup.add(zoom);
		return headerPopup;
	}
	/**
	 * Start sorting if the column header has been clicked.
	 * @param e the MouseEvent triggering the click
	 */
	@Override
	public void mouseClicked(final MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (e.getSource() instanceof JTableHeader)
			{
				this.headerPopupX = e.getX();
				EventQueue.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						JPopupMenu headerPopup = getHeaderPopup();
						headerPopup.show(getTableHeader(), e.getX(), e.getY());
					}
				});
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
							@Override
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
					@Override
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
			boolean addSortColumn = e.isControlDown();
			if (realColumn >= 0)
			{
				if (e.isShiftDown())
				{
					dwModel.removeSortColumn(realColumn);
					repaint();
				}
				else
				{
					dwModel.sortInBackground(this, realColumn, addSortColumn);
				}
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			boolean altDown = ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK);
			setColumnSelectionAllowed(altDown);
		}
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	public int getPopupViewColumnIndex()
	{
		TableColumnModel colMod = this.getColumnModel();
		return colMod.getColumnIndexAtX(this.headerPopupX);
	}

	public int getPopupColumnIndex()
	{
		int viewColumn = getPopupViewColumnIndex();
		int column = this.convertColumnIndexToModel(viewColumn);
		return column;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		int column = getPopupColumnIndex();
		if (e.getSource() == this.sortAscending && this.dwModel != null)
		{
			dwModel.sortInBackground(this, column, true, false);
		}
		else if (e.getSource() == this.sortDescending && this.dwModel != null)
		{
			dwModel.sortInBackground(this, column, false, false);
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

	/**
	 *	Display dialog window to let the user
	 *	select the key columns for the current update table
	 *
	 *	@return true if the user selected OK, false if the user cancelled the dialog
	 */
	public boolean selectKeyColumns()
	{
		DataStore ds = this.getDataStore();
		TableIdentifier table = ds.getUpdateTable();

		if (table == null)
		{
			table = selectUpdateTable();
			if (table != null)
			{
				ds.setUpdateTable(table);
			}
		}

		ResultInfo info = ds.getResultInfo();

		if (!info.hasPkColumns())
		{
			try
			{
				info.readPkDefinition(ds.getOriginalConnection());
			}
			catch (SQLException e)
			{
				LogMgr.logError("WbTable.selectKeyColumns()", "Error when retrieving key columns", e);
			}
		}

		if (table == null)
		{
			Window w = SwingUtilities.getWindowAncestor(this);
			WbSwingUtilities.showErrorMessageKey(w, "MsgNoUpdateTable");
			return false;
		}

		KeyColumnSelectorPanel panel = new KeyColumnSelectorPanel(info);
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

		if (this.saveDataAsAction != null)
		{
			saveDataAsAction.setEnabled(hasRows);
		}

		if (this.copyAsTextAction != null)
		{
			this.copyAsTextAction.setEnabled(hasRows);
		}

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

	public TableIdentifier selectUpdateTable()
	{
		if (this.getDataStore() == null) return null;

		String csql = this.getDataStore().getGeneratingSql();
		List<String> tables = SqlUtil.getTables(csql, false);
		TableIdentifier table = null;

		if (tables.size() > 1)
		{
			SelectTablePanel p = new SelectTablePanel(tables);

			boolean ok = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(this), p, ResourceMgr.getString("MsgSelectTableTitle"));
			String selectedTable = null;
			if (ok)
			{
				selectedTable = p.getSelectedTable();
			}
			if (selectedTable != null)
			{
				table = new TableIdentifier(selectedTable);
			}
		}
		else if (tables.size() == 1)
		{
			table = getDataStore().getUpdateTable();
			if (table == null)
			{
				table = new TableIdentifier(tables.get(0));
			}
		}
		return table;
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

	@Override
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

	public long addRow()
	{
		DataStoreTableModel ds = this.getDataStoreTableModel();
		if (ds == null) return -1;

		int selectedRow = this.getSelectedRow();
		final int newRow;

		saveRowHeaderState();
		this.stopEditing();

		if (selectedRow == -1)
		{
			newRow = ds.addRow();
		}
		else
		{
			newRow = ds.insertRow(selectedRow);
		}
		restoreRowHeaderState();

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
				@Override
				public void run()
				{
					edit.requestFocusInWindow();
				}
			});
		}
		return newRow;
	}

	public int duplicateRow(int row)
	{
		DataStoreTableModel model = this.getDataStoreTableModel();
		if (model == null) return -1;
		int newRow = -1;

		try
		{
			saveRowHeaderState();
			newRow = model.duplicateRow(row);
		}
		finally
		{
			restoreRowHeaderState();
		}
		return newRow;
	}

	public boolean deleteRow()
	{
		try
		{
			return deleteRow(false);
		}
		catch (SQLException e)
		{
			// cannot happen when not deleting dependencies
			return false;
		}
	}

	public boolean deleteRow(boolean withDependencies)
		throws SQLException
	{
		DataStoreTableModel ds = this.getDataStoreTableModel();
		if (ds == null) return false;

		try
		{
			saveRowHeaderState();
			int[] selectedRows = this.getSelectedRows();

			int numRows = selectedRows.length;
			if (numRows > 0)
			{
				for (int i = numRows - 1; i >= 0; i--)
				{
					ds.deleteRow(selectedRows[i], withDependencies);
				}
			}
			return true;
		}
		finally
		{
			restoreRowHeaderState();
		}
	}

	public void setHighlightRequiredFields(boolean flag)
	{
		this.highlightRequiredFields = flag;
		if (flag && this.requiredColor == null)
		{
			requiredColor = GuiSettings.getRequiredFieldColor();
		}
		else if (!flag)
		{
			requiredColor = null;
		}
	}

}
