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

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class DateColumnRenderer
	extends DefaultTableCellRenderer
{
	private SimpleDateFormat formatter;
	private HashMap displayCache = new HashMap();

	public DateColumnRenderer()
	{
		this("yyyy-MM-dd");
	}
	/** Creates a new instance of DateColumnRenderer */
	public DateColumnRenderer(String aDateFormat)
	{
		this.formatter = new SimpleDateFormat(aDateFormat);
	}

  public void setValue(Object value)
	{
    this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
		Date aDate = null;
		String newVal = null;
		if (value != null)
		{
			aDate = (Date)value;
			newVal = (String)this.displayCache.get(aDate);
			if (newVal == null)
			{
				newVal = this.formatter.format(aDate);
				this.displayCache.put(aDate, newVal);
			}
		}
		else
		{
			newVal = "";
		}
		super.setValue(newVal);
  }
	/*
	public Component getTableCellRendererComponent(
			JTable aTable,
			Object aValue,
			boolean isSelected,
			boolean hasFocus,
			int aRow,
			int aColumn)
	{
		JComponent jcomp = (JComponent)super.getTableCellRendererComponent(aTable, aValue, isSelected, hasFocus, aRow, aColumn);
		if(isSelected)
		{
			jcomp.setBackground(aTable.getSelectionBackground());
			jcomp.setForeground(aTable.getSelectionForeground());
		}
		else
		{
			jcomp.setBackground(Color.white);
			jcomp.setForeground(Color.black);
		}
		jcomp.setOpaque(true);

		return jcomp;
	}
	 */

}
