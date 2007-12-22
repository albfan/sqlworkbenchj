/*
 * TableColumnOptimizer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JLabel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import workbench.gui.renderer.WbRenderer;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A class to adjust the columns of a WbTable to the displayed values.
 *
 * @author support@sql-workbench.net
 */
public class TableColumnOptimizer
{

  private WbTable table;

  public TableColumnOptimizer(WbTable client)
  {
    this.table = client;
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
    int count = this.table.getColumnCount();
    for (int i = 0; i < count; i++)
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
    if (aColumn < 0 || aColumn > this.table.getColumnCount() - 1)
    {
      return;
    }
    TableColumnModel colMod = this.table.getColumnModel();
    TableColumn col = colMod.getColumn(aColumn);
    int addWidth = this.getAdditionalColumnSpace();
    int optWidth = minWidth;

    if (respectColumnName)
    {
      JTableHeader th = this.table.getTableHeader();
      TableCellRenderer rend = col.getCellRenderer();
      if (rend == null)
      {
        rend = th.getDefaultRenderer();
      }
      String colName = table.getColumnName(aColumn);
      Component c = rend.getTableCellRendererComponent(this.table, colName, false, false, 0, aColumn);
      Font headerFont = c.getFont();
      FontMetrics hfm = c.getFontMetrics(headerFont);
      int headerWidth = hfm.stringWidth(colName) + addWidth + 5;
      optWidth = Math.max(minWidth, headerWidth);
    }

    int rowCount = this.table.getRowCount();

    String s = null;
    int stringWidth = 0;


    for (int row = 0; row < rowCount; row++)
    {
      TableCellRenderer rend = this.table.getCellRenderer(row, aColumn);
      Component c = rend.getTableCellRendererComponent(this.table, table.getValueAt(row, aColumn), false, false, row, aColumn);
      Font f = c.getFont();
      FontMetrics fm = c.getFontMetrics(f);
      if (c instanceof WbRenderer)
      {
        s = ((WbRenderer) c).getDisplayValue();
      }
      else if (c instanceof JLabel)
      {
        // DefaultCellRenderer is a JLabel
        s = ((JLabel) c).getText();
      }
      else
      {
        s = this.table.getValueAsString(row, aColumn);
      }

      if (s == null || s.length() == 0)
      {
        stringWidth = 0;
      }
      else
      {
        String visible = StringUtil.rtrim(s);
        stringWidth = fm.stringWidth(visible);
        if (visible.length() < s.length())
        {
          stringWidth += fm.stringWidth("www");
        }
      }

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
    int addWidth = this.table.getIntercellSpacing().width * 2;
    if (this.table.getShowVerticalLines())
    {
      addWidth += 4;
    }
    return addWidth;
  }

  /**
   * Adjusts the columns to the width defined from the
   * underlying tables (i.e. getColumnWidth() for each column)
   * This does not adjust the width of the columns to the content.
   *
   * @see #optimizeAllColWidth()
   */
  public void adjustColumns(boolean adjustToColumnLabel)
  {
    if (this.table.getModel() == null) return;
    DataStoreTableModel dwModel = this.table.getDataStoreTableModel();
    if (dwModel == null) return;
		
    Font f = this.table.getFont();
    FontMetrics fm = this.table.getFontMetrics(f);
    int charWidth = fm.stringWidth("n");
    TableColumnModel colMod = this.table.getColumnModel();
    if (colMod == null) return;
		
    int minWidth = Settings.getInstance().getMinColumnWidth();
    int maxWidth = Settings.getInstance().getMaxColumnWidth();

    for (int i = 0; i < colMod.getColumnCount(); i++)
    {
      TableColumn col = colMod.getColumn(i);
      int addWidth = this.getAdditionalColumnSpace();
      int addHeaderWidth = this.getAdditionalColumnSpace();

      int lblWidth = 0;
      if (adjustToColumnLabel)
      {
        String s = dwModel.getColumnName(i);
        lblWidth = fm.stringWidth(s) + addHeaderWidth;
      }
      int width = (dwModel.getColumnWidth(i) * charWidth) + addWidth;
      int w = Math.max(width, lblWidth);
      if (maxWidth > 0)
      {
        w = Math.min(w, maxWidth);
      }
      if (minWidth > 0)
      {
        w = Math.max(w, minWidth);
      }
      col.setPreferredWidth(w);
    }
  }
}
