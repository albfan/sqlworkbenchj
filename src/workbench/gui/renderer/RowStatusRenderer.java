/*
 * NumberColumnRenderer.java
 *
 * Created on 1. Juli 2002, 13:22
 */

package workbench.gui.renderer;

import java.awt.Component;
import java.awt.Dimension;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import workbench.resource.ResourceMgr;
import workbench.storage.DataStore;

/**
 *
 * @author  thomas.kellerer@mgm-edv.de
 */
public class RowStatusRenderer extends DefaultTableCellRenderer
{
	private static final ImageIcon STATUS_MODIFIED_ICON = ResourceMgr.getPicture("modifiedrow");
	private static final ImageIcon STATUS_NOT_MODIFIED_ICON = ResourceMgr.getPicture("blank");
	private static final ImageIcon STATUS_NEW_ICON = ResourceMgr.getPicture("newrow");
	
	private JLabel label;
	
	/** Creates a new instance of NumberColumnRenderer */
	public RowStatusRenderer()
	{
		this.label = new JLabel();
		Dimension dim = new Dimension(18, 18);
		this.label.setMaximumSize(dim);
		this.label.setMinimumSize(dim);
		this.label.setPreferredSize(dim);
		this.label.setText(null);
		this.label.setIconTextGap(0);
		this.label.setHorizontalAlignment(JLabel.LEFT);
	}
	
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
	{
		try
		{
			Integer status = (Integer)value;
			if (status == DataStore.ROW_NEW)
			{
				this.label.setIcon(STATUS_NEW_ICON);
			}
			else if (status == DataStore.ROW_MODIFIED)
			{
				this.label.setIcon(STATUS_MODIFIED_ICON);
			}
			else
			{
				this.label.setIcon(STATUS_NOT_MODIFIED_ICON);
			}			
		}
		catch (Exception e)
		{
			this.label.setIcon(STATUS_NOT_MODIFIED_ICON);
		}
		return this.label;
	}
	
}
