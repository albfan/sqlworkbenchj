/*
 * WbTextCellEditor.java
 *
 * Created on March 1, 2003, 4:46 PM
 */

package workbench.gui.components;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;
import workbench.gui.WbSwingUtilities;

/**
 *
 * @author  thomas
 */
public class WbTextCellEditor
	extends DefaultCellEditor
	implements MouseListener
{
	
	private JTextField textField;
	
	public static final WbTextCellEditor createInstance()
	{
		JTextField field = new JTextField();
		WbTextCellEditor editor = new WbTextCellEditor(field);
		return editor;
	}
	
	private WbTextCellEditor(final JTextField aTextField)
	{
		super(aTextField);
		this.textField = aTextField;
		this.textField.setBorder(WbSwingUtilities.EMPTY_BORDER);
		this.textField.addMouseListener(this);
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
	}
}
