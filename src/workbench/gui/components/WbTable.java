/*
 * DwTable.java
 *
 * Created on December 1, 2001, 11:41 PM
 */
package workbench.gui.components;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.*;
import workbench.gui.renderer.DateColumnRenderer;
import workbench.gui.renderer.NumberColumnRenderer;
import workbench.gui.renderer.RowStatusRenderer;
import workbench.gui.renderer.ToolTipRenderer;
import workbench.interfaces.Exporter;
import workbench.interfaces.FontChangedListener;
import workbench.interfaces.Searchable;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.NullValue;




public class WbTable 
	extends JTable 
	implements ActionListener, MouseListener, Exporter, FontChangedListener, Searchable
{
	private JPopupMenu popup;
	private JPopupMenu headerPopup;
	
	private DataStoreTableModel dwModel;
	private String lastSearchCriteria;
	private int lastFoundRow = -1;
	private TableModelListener changeListener;

	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private SortAscendingAction sortAscending;
	private SortDescendingAction sortDescending;
	private OptimizeColumnWidthAction optimizeCol;
	private SetColumnWidthAction setColWidth;
	
	private FindAction findAction;
	private FindAgainAction findAgainAction;
	private DataToClipboardAction dataToClipboard;
	private SaveDataAsAction exportDataAction;
	
	private boolean adjustToColumnLabel = false;
	public static final LineBorder FOCUSED_CELL_BORDER = new LineBorder(Color.yellow);
	private int headerPopupY = -1;
	private int headerPopupX = -1;
	private HashMap savedColumnSizes;
	private int currentRow = -1;
	private int currentColumn = -1;
	private int maxColWidth = 32768;
	private int minColWidth = 10;
	private static final DefaultTableModel EMPTY_MODEL = new DefaultTableModel();
	
	public WbTable()
	{
		super();
		this.setMinimumSize(null);
		this.setMaximumSize(null);
		this.setPreferredSize(null);
		
		this.sortAscending = new SortAscendingAction(this);
		this.sortAscending.setEnabled(false);
		this.sortDescending = new SortDescendingAction(this);
		this.sortDescending.setEnabled(false);		
		this.optimizeCol = new OptimizeColumnWidthAction(this);
		this.setColWidth = new SetColumnWidthAction(this);
		
		this.headerPopup = new JPopupMenu();
		this.headerPopup.add(this.sortAscending.getMenuItem());
		this.headerPopup.add(this.sortDescending.getMenuItem());
		this.headerPopup.addSeparator();
		this.headerPopup.add(this.optimizeCol.getMenuItem());
		this.headerPopup.add(this.setColWidth.getMenuItem());
		
		Font dataFont = this.getFont();
		if (dataFont == null) dataFont = (Font)UIManager.get("Table.font");
		
		JTextField stringField = new JTextField();
		if (dataFont != null) stringField.setFont(dataFont);
		stringField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		stringField.addMouseListener(new TextComponentMouseListener());
		this.defaultEditor = new DefaultCellEditor(stringField);
		
		JTextField numberField = new JTextField();
		if (dataFont != null)  numberField.setFont(dataFont);
		numberField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		numberField.addMouseListener(new TextComponentMouseListener());
		this.defaultNumberEditor = new DefaultCellEditor(numberField);
		this.addMouseListener(this);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JTableHeader th = this.getTableHeader();
		th.addMouseListener(this);	
		
		this.findAction = new FindAction(this);
		this.findAction.setEnabled(false);
		this.findAction.setCreateMenuSeparator(true);
		this.findAgainAction = new FindAgainAction(this);
		this.findAgainAction.setEnabled(false);
		
		this.dataToClipboard = new DataToClipboardAction(this);
		this.exportDataAction = new SaveDataAsAction(this);
		
		this.addPopupAction(this.exportDataAction, false);
		this.addPopupAction(this.dataToClipboard, false);
		this.addPopupAction(this.findAction, true);
		this.addPopupAction(this.findAgainAction, false);

		InputMap im = this.getInputMap(WHEN_FOCUSED);
		ActionMap am = this.getActionMap();
		this.findAction.addToInputMap(im, am);
		this.findAgainAction.addToInputMap(im, am);
		this.dataToClipboard.addToInputMap(im, am);
		this.exportDataAction.addToInputMap(im, am);
		WbManager.getSettings().addFontChangedListener(this);
	}
	
	public SaveDataAsAction getExportAction()
	{
		return this.exportDataAction;
	}
	
	public DataToClipboardAction getDataToClipboardAction()
	{
		return this.dataToClipboard;
	}
	
	public FindAction getFindAction()
	{
		return this.findAction;
	}
	
	public FindAgainAction getFindAgainAction()
	{
		return this.findAgainAction;
	}
	
	public void reset()
	{
		if (this.dwModel != null)
		{
			this.dwModel.dispose();
			this.dwModel = null;
			//this.sortModel = null;
		}
		this.setModel(EMPTY_MODEL, false);
	}
	
	public void addPopupAction(WbAction anAction, boolean withSep)
	{
		if (this.popup == null) this.popup = new JPopupMenu();
		if (withSep) this.popup.addSeparator();
		this.popup.add(anAction.getMenuItem());
	}
	
	
	public void setModel(TableModel aModel)
	{
		this.setModel(aModel, false);
	}
	
	public void setModel(TableModel aModel, boolean sortIt)
	{
		DataStoreTableModel oldModel = this.dwModel;
		
		if (this.dwModel != null)
		{
			if (this.dwModel != null && this.changeListener != null)
			{
				this.dwModel.removeTableModelListener(this.changeListener);
			}
		}
		
		this.dwModel = null;
		super.setModel(aModel);

		JTableHeader header = this.getTableHeader();
		
		if (sortIt && aModel instanceof DataStoreTableModel)
		{
			this.dwModel = (DataStoreTableModel)aModel;
			if (header != null)
			{
				header.setDefaultRenderer(new SortHeaderRenderer());
				header.addMouseListener(this);
			}
		}
		if (this.sortAscending != null) this.sortAscending.setEnabled(sortIt);
		if (this.sortDescending != null) this.sortDescending.setEnabled(sortIt);
		
		if (this.changeListener != null && this.dwModel != null)
		{
			this.dwModel.addTableModelListener(this.changeListener);
		}
		
		this.initDefaultRenderers();
		this.currentRow = -1;
		this.currentColumn = -1;
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
	
	public String getValueAsString(int row, int column)
		throws IndexOutOfBoundsException
	{
		Object value = this.getValueAt(row, column);
		if (value == null) return null;
		if (value instanceof String) return (String)value;
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
	
	public int getSortColum()
	{
		if (this.dwModel == null) return -1;
		return this.dwModel.getSortColumn();
	}
	
	public void setShowStatusColumn(boolean aFlag)
	{
		if (this.dwModel == null) return;
		if (aFlag == this.dwModel.getShowStatusColumn()) return;
		int column = this.getSelectedColumn();
		int row = this.getSelectedRow();
		
		int sortColumn = -1;
		boolean asc = false;
		if (this.dwModel != null)
		{
			sortColumn = dwModel.getSortColumn();
			asc = this.dwModel.isSortAscending();
		}
			
		this.saveColumnSizes();

		if (row == -1) row = this.currentRow;
		if (column == -1) column = this.currentColumn;
		this.dwModel.setShowStatusColumn(aFlag);
		if (aFlag)
		{
			TableColumn col = this.getColumnModel().getColumn(0);
			col.setCellRenderer(new RowStatusRenderer());
			col.setMaxWidth(20);
			col.setMinWidth(20);
			col.setPreferredWidth(20);
		}
		this.initDefaultEditors();
		this.restoreColumnSizes();
		if (sortColumn > -1 && this.dwModel != null)
		{
			if (aFlag) 
				sortColumn ++;
			else
				sortColumn --;
			this.dwModel.sortByColumn(sortColumn, asc);
		}
		if (row >= 0) 
		{
			this.getSelectionModel().setSelectionInterval(row, row);
			int newColumn = column;
			if (aFlag) 
				newColumn ++;
			else
				newColumn --;
			
			if (newColumn >= 0) this.changeSelection(row, newColumn, true, true);
		}
	}
	

	public int getSortedViewColumnIndex()
	{
		if (this.dwModel == null) return -1;
		int modelIndex = this.dwModel.getSortColumn();
		int viewIndex = this.convertColumnIndexToView(modelIndex);
		return viewIndex;
	}
	
	public boolean isSortedColumnAscending()
	{
		if (this.dwModel == null) return true;
		return this.dwModel.isSortAscending();
	}
	
	public synchronized void sortingStarted()
	{
		this.setIgnoreRepaint(true);
	}

	public synchronized void sortingFinished()
	{
		this.setIgnoreRepaint(false);
	}
	
	public String getDataString(String aLineTerminator)
	{
		return this.getDataString(aLineTerminator, true);
	}
	
	public String getDataString(String aLineTerminator, boolean includeHeaders)
	{
		if (this.dwModel == null) return "";
		DataStore ds = this.dwModel.getDataStore();
		return ds.getDataString(aLineTerminator, includeHeaders);
	}
	
	public boolean canSearchAgain()
	{
		return this.lastFoundRow >= 0;
	}
	
	public int search(String aText)
	{
		return this.search(aText, false);
	}
	
	public int searchNext()
	{
		return this.search(this.lastSearchCriteria, true);
	}
	
	public int search(String aText, boolean doContinue)
	{
		if (aText == null) return -1;
		aText = aText.toLowerCase();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		int foundRow = -1;
		int start = 0;
		if (doContinue && this.lastFoundRow >= 0)
		{
			start = this.lastFoundRow  + 1;
		}
	
		for (int i=start; i < this.dwModel.getRowCount(); i++)
		{
			//int row = sortModel.getRealIndex(i);
			String rowString = this.dwModel.getRowData(i).toString();
			if (rowString == null) continue;
			
			if (rowString.toLowerCase().indexOf(aText) > -1)
			{
				this.getSelectionModel().setSelectionInterval(i,i);
				foundRow = i;
				this.lastSearchCriteria = aText;
				break;
			}
		}
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		this.lastFoundRow = foundRow;
		if (foundRow >= 0)
		{
			this.scrollToRow(foundRow);
		}
			
		return foundRow;
	}
	
	public void saveColumnSizes()
	{
		TableColumnModel colMod = this.getColumnModel();
		int count = colMod.getColumnCount();
		this.savedColumnSizes = new HashMap();
		for (int i=0; i < count; i++)
		{
			TableColumn col = colMod.getColumn(i);
			String name = this.getColumnName(i);
			Integer width = new Integer(col.getPreferredWidth());
			this.savedColumnSizes.put(name, width);
		}
	}
	
	public void restoreColumnSizes()
	{
		if (this.savedColumnSizes == null || this.savedColumnSizes.size() == 0) return;
		Iterator itr = this.savedColumnSizes.entrySet().iterator();
		while (itr.hasNext())
		{
			Entry entry = (Entry)itr.next();
			try
			{
				TableColumn col = this.getColumn(entry.getKey());
				int width = ((Integer)entry.getValue()).intValue();
				col.setPreferredWidth(width);
			}
			catch (Throwable th)
			{
				// ignore errors for columns which do no longer exist
			}
		}
		this.savedColumnSizes = null;
	}
	
	public void initDefaultRenderers()
	{
		if (this.defaultRenderersByColumnClass == null)
			this.createDefaultRenderers();
		
		String format = WbManager.getSettings().getDefaultDateFormat();
		this.setDefaultRenderer(Date.class, new DateColumnRenderer(format));
		int maxDigits = WbManager.getSettings().getMaxFractionDigits();
		if (maxDigits == -1) maxDigits = 10;
		this.setDefaultRenderer(Number.class, new NumberColumnRenderer(maxDigits));
		this.setDefaultRenderer(Object.class, new ToolTipRenderer());
	}
	
	public void initDefaultEditors()
	{
		if (this.dwModel == null) return;
		
		TableColumnModel colMod = this.getColumnModel();
		
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			if (Number.class.isAssignableFrom(this.dwModel.getColumnClass(i)))
			{
				col.setCellEditor(this.defaultNumberEditor);
			}
			else
			{
				col.setCellEditor(this.defaultEditor);
			}
		}
	}
	
	public void adjustColumns()
	{
		if (this.getModel() == null) return;
		Font f = this.getFont();
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.getColumnModel();
		
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			TableColumn col = colMod.getColumn(i);
			int addWidth = this.getAdditionalColumnSpace(0, i);
			int addHeaderWidth = this.getAdditionalColumnSpace(-1, i);

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
				w	= Math.min(w, this.maxColWidth);
				if (w < this.minColWidth) w = this.minColWidth;
				col.setPreferredWidth(w);
			}
		}
	}	

	public synchronized void optimizeColWidth(int aColumn)
	{
		if (this.dwModel == null) return;
		if (aColumn < 0 || aColumn > this.getColumnCount() - 1) return;
		
		Font f = this.getFont();
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
		TableColumnModel colMod = this.getColumnModel();
		TableColumn col = colMod.getColumn(aColumn);
		int addWidth = this.getAdditionalColumnSpace(0, aColumn);
		String s = null;//this.dwModel.getColumnName(aColumn);
		int optWidth = 0;
		for (int row = 0; row < this.getRowCount(); row ++)
		{
			s = this.getValueAsString(row, aColumn);
			if (s == null || s.length() == 0) continue;
			optWidth = Math.max(optWidth, fm.stringWidth(s) + addWidth);
		}
		if (optWidth > 0)
		{
			col.setPreferredWidth(optWidth);
		}
	}	
	
	private int getAdditionalColumnSpace(int aRow, int aColumn)
	{
		TableColumn col = this.getColumnModel().getColumn(aColumn);
		TableCellRenderer rend;
		if (aRow == -1)
			rend = col.getHeaderRenderer();
		else
			rend = col.getCellRenderer();
		
		int addWidth = this.getIntercellSpacing().width * 2;
		if (this.getShowVerticalLines()) addWidth += 4;
		
		if (rend == null) 
		{
			rend = this.getDefaultRenderer(this.getColumnClass(aColumn));
		}
		if (rend != null)
		{
			Component c = rend.getTableCellRendererComponent(this, "", false, false, aRow, aColumn);
			if (c instanceof JComponent)
			{
				JComponent jc = (JComponent)c;
				Insets ins = jc.getInsets();
				if (ins != null)
				{
					addWidth += ins.left + ins.right;
				}
			}
		}
		return addWidth;
	}
	
	public void tableChanged(TableModelEvent evt)
	{
		super.tableChanged(evt);
		if (evt.getFirstRow() == TableModelEvent.HEADER_ROW)
		{
			this.initDefaultEditors();
		}
	}
	public void addTableModelListener(TableModelListener aListener)
	{
		this.changeListener = aListener;
		if (this.dwModel != null) this.dwModel.addTableModelListener(aListener);
	}

	public void removeTableModelListener(TableModelListener aListener)
	{
		this.changeListener = null;
		if (this.dwModel != null) this.dwModel.removeTableModelListener(aListener);
	}

	public void scrollToRow(int aRow)
	{
		Rectangle rect = this.getCellRect(aRow, 1, true);
		this.scrollRectToVisible(rect);
	}
	
	/** Invoked when the mouse button has been clicked (pressed
	 * and released) on a component.
	 *
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (e.getSource() instanceof JTableHeader)
			{
				this.headerPopupX = e.getX();
				this.headerPopupY = e.getY();
				this.headerPopup.show(this.getTableHeader(), e.getX(), e.getY());
			}
			else if (this.popup != null)
			{
				this.findAction.setEnabled(this.getRowCount() > 0);
				this.findAgainAction.setEnabled(this.lastFoundRow > 0);
				this.popup.show(this, e.getX(), e.getY());
			}
		}
		else if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 1 
		         && this.dwModel != null)
		{
			TableColumnModel columnModel = this.getColumnModel();
			int viewColumn = columnModel.getColumnIndexAtX(e.getX());
			int realColumn = this.convertColumnIndexToModel(viewColumn);

			if (realColumn >= 0)
			{
				//sorter.startSorting(tableView, realColumn, sortAscending);
				this.dwModel.sortInBackground(this, realColumn);
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
	}
	
	/** Invoked when a mouse button has been released on a component.
	 *
	 */
	public void mouseReleased(MouseEvent e)
	{
	}
	
	/** Invoked when an action occurs.
	 *
	 */
	public void actionPerformed(ActionEvent e)
	{
		TableColumnModel columnModel = this.getColumnModel();
		int viewColumn = columnModel.getColumnIndexAtX(this.headerPopupX);
		final int column = this.convertColumnIndexToModel(viewColumn);
		if (e.getSource() == this.sortAscending && this.dwModel != null)
		{
			this.dwModel.sortInBackground(this, column, true);
		}
		else if (e.getSource() == this.sortDescending && this.dwModel != null)
		{
			this.dwModel.sortInBackground(this, column, false);
		}
		else if (e.getSource() == this.optimizeCol)
		{
			new Thread(new Runnable()
			{
				public void run()	{ optimizeColWidth(column); }
			}).start();
			
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
			catch (Exception ex2)
			{
			}
		}
	}

	public void resetPopup()
	{
		this.popup = null;
	}
	
	/**
	 *	Open the Find dialog for searching strings in the result set
	 */
	public void findData()
	{
		String criteria;
		criteria = WbSwingUtilities.getUserInput(this, ResourceMgr.getString("MsgEnterSearchCriteria"), this.lastSearchCriteria);
		if (criteria == null) return;
		int row = this.search(criteria, false);
		this.lastSearchCriteria = criteria;
		this.findAgainAction.setEnabled(row >= 0);
	}
	
	public void findNext()
	{
		this.searchNext();
	}

	public void copyDataToClipboard()
	{
		this.copyDataToClipboard(true);
	}
	
	public void copyDataToClipboard(final boolean includeheaders)
	{
		if (this.getRowCount() <= 0) return;
		
		try
		{
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			WbSwingUtilities.showWaitCursorOnWindow(this);
			String data = getDataString("\r", includeheaders);
			StringSelection sel = new StringSelection(data);
			clp.setContents(sel, sel);
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Could not copy text data to clipboard", e);
		}
		WbSwingUtilities.showDefaultCursorOnWindow(this);
	}

	public void copyAsSqlInsert()
	{
		if (this.getRowCount() <= 0) return;
		
		DataStore ds = this.dwModel.getDataStore();
		if (ds == null) return;
		if (!ds.canSaveAsSqlInsert()) return;
		
		try
		{
			Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
			WbSwingUtilities.showWaitCursorOnWindow(this);
			String data = ds.getDataAsSqlInsert();
			StringSelection sel = new StringSelection(data);
			clp.setContents(sel, sel);
		}
		catch (Throwable e)
		{
			LogMgr.logError(this, "Error when copying SQL inserts", e);
		}
		WbSwingUtilities.showDefaultCursorOnWindow(this);
	}
	
	public void saveAsSqlInsert(String aFilename)
	{
		if (this.getRowCount() <= 0) return;
		if (this.dwModel == null) return;
		DataStore ds = this.dwModel.getDataStore();
		if (ds == null) return;
		if (!ds.canSaveAsSqlInsert()) return;

		PrintWriter out = null;
		
		try
		{
			WbSwingUtilities.showWaitCursor(this);
			String contents = ds.getDataAsSqlInsert().replaceAll("\n", "\r\n");
			out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(aFilename)));
			out.print(contents);
			out.close();
		}
		catch (Throwable th)
		{
			LogMgr.logError(this, "Could not save SQL data", th);
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
		WbSwingUtilities.showDefaultCursor(this);
	}
	
	public void saveAsAscii(String aFilename)
	{
		if (this.dwModel == null) return;
		
		PrintWriter out = null;
		
		WbSwingUtilities.showWaitCursor(this.getParent());
		try
		{
			out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(aFilename)));
			String contents = this.getDataString(System.getProperty("line.separator", "\r\n"));
			out.print(contents);
		}
		catch (Throwable th)
		{
			LogMgr.logError(this, "Could not save data", th);
		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
		WbSwingUtilities.showDefaultCursor(this.getParent());
	}
	
	public void saveAs()
	{
		try
		{
			String lastDir = WbManager.getSettings().getLastExportDir();
			JFileChooser fc = new JFileChooser(lastDir);
			fc.addChoosableFileFilter(ExtensionFileFilter.getTextFileFilter());
			DataStore ds = this.dwModel.getDataStore();
			if (ds != null && ds.canSaveAsSqlInsert())
			{
				System.out.println("adding sql filter");
				fc.addChoosableFileFilter(ExtensionFileFilter.getSqlFileFilter());
			}
			int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
			if (answer == JFileChooser.APPROVE_OPTION)
			{
				File fl = fc.getSelectedFile();
				FileFilter ff = fc.getFileFilter();
				if (ff == ExtensionFileFilter.getSqlFileFilter())
				{
					String filename = fl.getAbsolutePath();
					
					String ext = ExtensionFileFilter.getExtension(fl);
					if (ext.length() == 0)
					{
						if (!filename.endsWith(".")) filename = filename + ".";
						filename = filename + "sql";
					}
					final String name = filename;
					EventQueue.invokeLater(new Runnable()
					{
						public void run() { saveAsSqlInsert(name); }
					});
				}
				else
				{
					final String name = fl.getAbsolutePath();
					EventQueue.invokeLater(new Runnable()
					{
						public void run() { saveAsAscii(name); }
					});
				}
				
				lastDir = fc.getCurrentDirectory().getAbsolutePath();
				WbManager.getSettings().setLastExportDir(lastDir);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError(this, "Error exporting data", e);
		}
	}

	/*
	public TableCellEditor getCellEditor(int row, int column)
	{
		this.currentRow = row;
		this.currentColumn = column;
		return super.getCellEditor(row, column);
	}

	public TableCellRenderer getCellRenderer(int row, int column)
	{
		//this.currentRow = row;
		//this.currentColumn = column;
		return super.getCellRenderer(row, column);
	}
	*/
	
	public int getMaxColWidth()
	{
		return maxColWidth;
	}
	
	public void setMaxColWidth(int maxColWidth)
	{
		this.maxColWidth = maxColWidth;
	}
	
	public int getMinColWidth()
	{
		return minColWidth;
	}
	
	public void setMinColWidth(int minColWidth)
	{
		this.minColWidth = minColWidth;
	}
	
	public void fontChanged(String aFontId, Font newFont)
	{
		if (aFontId.equals(Settings.DATA_FONT_KEY))
		{
			this.setFont(newFont);
			this.getTableHeader().setFont(newFont);
		}
	}

	public String toString()
	{
		return getClass().getName() + '@' + Integer.toHexString(hashCode());
	}
}

