/*
 * DateColumnRenderer.java
 *
 * Created on 15. Juli 2002, 20:38
 */

package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbTable;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DateColumnRenderer
	extends DefaultTableCellRenderer
{
	private SimpleDateFormat formatter;
	private HashMap displayCache = new HashMap();
	public static final String DEFAULT_FORMAT = "yyyy-MM-dd";
	public DateColumnRenderer()
	{
		this(DEFAULT_FORMAT);
	}
	/** Creates a new instance of DateColumnRenderer */
	public DateColumnRenderer(String aDateFormat)
	{
		if (aDateFormat == null)
		{
			aDateFormat = DEFAULT_FORMAT;
		}
		this.formatter = new SimpleDateFormat(aDateFormat);
    this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
	}

  public void setValue(Object value)
	{
		Date aDate = null;
		String newVal = null;
		String tip = null;

		if (value != null )
		{
			try
			{
				aDate = (Date)value;
				tip = aDate.toString();
				newVal = (String)this.displayCache.get(aDate);
				if (newVal == null)
				{
					newVal = this.formatter.format(aDate);
					this.displayCache.put(aDate, newVal);
				}
			}
			catch (ClassCastException cc)
			{
				newVal = "";
				tip = "";
			}
		}
		else
		{
			newVal = "";
			tip = "";
		}
		this.setToolTipText(tip);
		super.setValue(newVal);
  }
	public Component getTableCellRendererComponent(	JTable table,
																									Object value,
																									boolean isSelected,
																									boolean hasFocus,
																									int row,
																									int col)
	{
		Component result = super.getTableCellRendererComponent(table, value, isSelected, false, row, col);
		if (hasFocus)
		{
			this.setBorder(WbTable.FOCUSED_CELL_BORDER);
		}
		else
		{
			this.setBorder(WbSwingUtilities.EMPTY_BORDER);
		}
		return result;
	}

}
