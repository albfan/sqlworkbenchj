/*
 * DwTable.java
 *
 * Created on December 1, 2001, 11:41 PM
 */
package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Math;
import java.util.Date;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.OptimizeColumnWidthAction;
import workbench.gui.actions.SetColumnWidthAction;
import workbench.gui.actions.SortAscendingAction;
import workbench.gui.actions.SortDescendingAction;
import workbench.gui.actions.WbAction;
import workbench.gui.renderer.DateColumnRenderer;
import workbench.gui.renderer.NumberColumnRenderer;
import workbench.gui.renderer.RowStatusRenderer;
import workbench.gui.renderer.ToolTipRenderer;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;
import workbench.storage.NullValue;


public class WbTable 
	extends JTable
	implements MouseListener, ActionListener
{
	private WbTableSorter sortModel;
	private JPopupMenu popup;
	private JPopupMenu headerPopup;
	
	private ResultSetTableModel dwModel;
	private TableModel originalModel;
	private int lastFoundRow = -1;
	private String lastSearchText;
	private TableModelListener changeListener;

	private DefaultCellEditor defaultEditor;
	private DefaultCellEditor defaultNumberEditor;
	private SortAscendingAction sortAscending;
	private SortDescendingAction sortDescending;
	private OptimizeColumnWidthAction optimizeCol;
	private SetColumnWidthAction setColWidth;
	
	private boolean adjustToColumnLabel = true;
	public static final LineBorder FOCUSED_CELL_BORDER = new LineBorder(Color.yellow);
	private int headerPopupY = -1;
	private int headerPopupX = -1;
	
	private static final DefaultTableModel EMPTY_MODEL = new DefaultTableModel();
	
	public WbTable()
	{
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
		
		JTextField stringField = new JTextField();
		stringField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		stringField.addMouseListener(new TextComponentMouseListener());
		this.defaultEditor = new DefaultCellEditor(stringField);
		
		JTextField numberField = new JTextField();
		numberField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		numberField.setHorizontalAlignment(SwingConstants.RIGHT);
		numberField.addMouseListener(new TextComponentMouseListener());
		this.defaultNumberEditor = new DefaultCellEditor(numberField);
		this.addMouseListener(this);
		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JTableHeader th = this.getTableHeader();
		th.addMouseListener(this);	
	}
	
	public void reset()
	{
		if (this.dwModel != null)
		{
			this.dwModel.dispose();
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
		if (!sortIt)
		{
			super.setModel(aModel);
			this.sortModel = null;
			this.originalModel = aModel;
			this.checkModel();
			if (this.sortAscending != null) this.sortAscending.setEnabled(false);
			if (this.sortDescending != null) this.sortDescending.setEnabled(false);
		}
		else
		{
			this.setModelForSorting(aModel);
		}
	}
	
	public void setModelForSorting(TableModel aModel)
	{
		this.originalModel = aModel;
		this.sortModel = new WbTableSorter(aModel);
		super.setModel(this.sortModel);
    JTableHeader header = this.getTableHeader();
		if (header != null)
		{
			header.setDefaultRenderer(new SortHeaderRenderer());
			this.sortModel.addMouseListenerToHeaderInTable(this);
		}
		this.sortAscending.setEnabled(true);
		this.sortDescending.setEnabled(true);
		this.checkModel();
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
	private void checkModel()
	{
		if (this.originalModel == null) return;
		if (this.originalModel instanceof ResultSetTableModel)
		{
			this.dwModel = (ResultSetTableModel)this.originalModel;
			if (this.changeListener != null) this.dwModel.addTableModelListener(this.changeListener);
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
	
	public void setShowStatusColumn(boolean aFlag)
	{
		if (this.dwModel == null) return;
		if (aFlag == this.dwModel.getShowStatusColumn()) return;
		int row = this.getSelectedRow();
		this.dwModel.setShowStatusColumn(aFlag);
		if (aFlag)
		{
			TableColumn col = this.getColumnModel().getColumn(0);
			col.setCellRenderer(new RowStatusRenderer());
			col.setMaxWidth(20);
			col.setMinWidth(20);
			col.setPreferredWidth(20);
		}
		if (row >= 0) this.getSelectionModel().setSelectionInterval(row, row);
	}
	
	public int getSortedViewColumnIndex()
	{
		if (this.sortModel == null) return -1;
		int modelIndex = this.sortModel.getColumn();
		int viewIndex = this.convertColumnIndexToView(modelIndex);
		return viewIndex;
	}
	
	public boolean isSortedColumnAscending()
	{
		if (this.sortModel == null) return true;
		return this.sortModel.isAscending();
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
		if (this.sortModel == null) return "";
		if (this.dwModel == null) return "";
		
		int colCount = this.sortModel.getColumnCount();
		int count = this.sortModel.getRowCount();
		StringBuffer result = new StringBuffer(count * 250);
		if (includeHeaders)
		{
			// Start loop at 1 --> ignore status column
			int start = 0;
			if (this.dwModel.getShowStatusColumn()) start = 1;
			for (int i=start; i < colCount; i++)
			{
				result.append(this.sortModel.getColumnName(i));
				if (i < colCount - 1) result.append('\t');
			}
			result.append(aLineTerminator);
		}
		for (int i=0; i < count; i++)
		{
			int row = this.sortModel.getRealIndex(i);
			result.append(this.dwModel.getRowData(row));
			result.append(aLineTerminator);
		}
		return result.toString();
	}
	
	public void saveAsAscii(String aFilename)
		throws IOException
	{
		if (this.sortModel == null || this.dwModel == null) return;
		
		PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(aFilename)));
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		this.getDataString("\r\n");
		out.close();
		this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
		return this.search(this.lastSearchText, true);
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
	
		for (int i=start; i < this.sortModel.getRowCount(); i++)
		{
			int row = sortModel.getRealIndex(i);
			String rowString = this.dwModel.getRowData(row).toString();
			if (rowString == null) continue;
			
			if (rowString.toLowerCase().indexOf(aText) > -1)
			{
				this.getSelectionModel().setSelectionInterval(i,i);
				foundRow = i;
				this.lastSearchText = aText;
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
	
	public void adjustColumns()
	{
		this.adjustColumns(32768);
	}
	
	public void adjustColumns(int maxWidth)
	{
		Font f = this.getFont();
		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(f);
		int charWidth = fm.stringWidth("n");
		TableColumnModel colMod = this.getColumnModel();
		//JTableHeader header = this.getTableHeader();
		//TableColumnModel headerColMod = header.getColumnModel();
		String format = WbManager.getSettings().getDefaultDateFormat();
		TableCellRenderer rend = this.getDefaultRenderer(Integer.class);
		this.setDefaultRenderer(Date.class, new DateColumnRenderer(format));
		int maxDigits = WbManager.getSettings().getMaxFractionDigits();
		if (maxDigits == -1) maxDigits = 10;
		this.setDefaultRenderer(Number.class, new NumberColumnRenderer(maxDigits));
		this.setDefaultRenderer(Object.class, new ToolTipRenderer());
		
		int start = 0;
		if (this.getShowStatusColumn()) start = 1;
		for (int i=0; i < colMod.getColumnCount(); i++)
		{
			int addWidth = this.getAdditionalColumnSpace(0, i);
			int addHeaderWidth = this.getAdditionalColumnSpace(-1, i);
			
			TableColumn col = colMod.getColumn(i);
			//TableColumn headerCol = headerColMod.getColumn(i);
			//col.setCellRenderer(new ToolTipRenderer());

			if (Number.class.isAssignableFrom(this.dwModel.getColumnClass(i)))
			{
				col.setCellEditor(this.defaultNumberEditor);
			}
			else
			{
				col.setCellEditor(this.defaultEditor);
			}
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
				w	= Math.min(w, maxWidth);
				col.setPreferredWidth(w);
			}
		}
	}	

	public void optimizeColWidth(int aColumn)
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
				this.headerPopup.show(this, e.getX(), e.getY());
			}
			else if (this.popup != null)
			{
				this.popup.show(this, e.getX(), e.getY());
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
		int column = this.convertColumnIndexToModel(viewColumn);
		if (e.getSource() == this.sortAscending && this.sortModel != null)
		{
			this.sortModel.startSorting(this, column, true);
		}
		else if (e.getSource() == this.sortDescending && this.sortModel != null)
		{
			this.sortModel.startSorting(this, column, false);
		}
		else if (e.getSource() == this.optimizeCol)
		{
			this.optimizeColWidth(column);
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
	
}

