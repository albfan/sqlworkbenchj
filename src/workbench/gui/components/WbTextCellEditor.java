/*
 * WbTextCellEditor.java
 *
 * Created on March 1, 2003, 4:46 PM
 */

package workbench.gui.components;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.EditWindow;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  thomas
 */
public class WbTextCellEditor
	extends DefaultCellEditor
	implements MouseListener
{
	
	private JTextField textField;
	private WbTable parentTable;
	
	public static final WbTextCellEditor createInstance()
	{
		return createInstance(null);
	}
	
	public static final WbTextCellEditor createInstance(WbTable parent)
	{
		JTextField field = new JTextField();
		WbTextCellEditor editor = new WbTextCellEditor(field);
		editor.parentTable = parent;
		return editor;
	}
	
	private WbTextCellEditor(final JTextField aTextField)
	{
		super(aTextField);
		this.textField = aTextField;
		this.textField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.textField.addMouseListener(this);
		this.textField.addMouseListener(new TextComponentMouseListener());
	}
	
	public void setFont(Font aFont)
	{
		this.textField.setFont(aFont);
	}
	
	public void mouseClicked(java.awt.event.MouseEvent evt)
	{
		if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1)
		{
			this.openEditWindow();
		}
	}
	
	public void mouseEntered(java.awt.event.MouseEvent mouseEvent)
	{
	}
	
	public void mouseExited(java.awt.event.MouseEvent mouseEvent)
	{
	}
	
	public void mousePressed(java.awt.event.MouseEvent mouseEvent)
	{
	}
	
	public void mouseReleased(java.awt.event.MouseEvent mouseEvent)
	{
	}

	private void openEditWindow()
	{
		if (this.parentTable == null)
		{
			Frame owner = (Frame)SwingUtilities.getWindowAncestor(this.textField);
			String title = ResourceMgr.getString("TxtEditWindowTitle");

			EditWindow w = new EditWindow(owner, title, this.textField.getText());
			w.show();
			if (!w.isCancelled())
			{
				this.textField.setText(w.getText());
			}
			w.dispose();
		}
		else
		{
			this.parentTable.openEditWindow();
		}
	}
}
