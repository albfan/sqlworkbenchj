/*
 * WbMouseListener.java
 *
 * Created on July 26, 2002, 11:22 AM
 */

package workbench.gui.components;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.text.JTextComponent;
import workbench.gui.menu.TextPopup;
import workbench.interfaces.ClipboardSupport;

/**
 *
 * @author  Thomas Kellerer
 */
public class TextComponentMouseListener implements MouseListener
{
	
	/** Creates a new instance of WbMouseListener */
	public TextComponentMouseListener()
	{
	}
	
	/** Invoked when the mouse button has been clicked (pressed
	 * and released) on a component.
	 *
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getButton() == MouseEvent.BUTTON3)
		{
			if (e.getSource() instanceof JTextComponent)
			{
				JTextComponent text = (JTextComponent)e.getSource();
				final ClipboardWrapper wrapp = new ClipboardWrapper(text);
				TextPopup pop = new TextPopup(wrapp);
				boolean edit = text.isEditable();
				boolean selected = text.getSelectionEnd() > text.getSelectionStart();
				pop.getCutAction().setEnabled(edit);
				pop.getClearAction().setEnabled(edit);
				pop.getPasteAction().setEnabled(edit);
				pop.getCopyAction().setEnabled(selected);
				//Component parent = text.getParent();
				pop.show(text,e.getX(),e.getY());
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

	private static class ClipboardWrapper 
		implements ClipboardSupport
	{
		JTextComponent client;
		public ClipboardWrapper(JTextComponent aClient)
		{
			this.client = aClient;
		}
		public void copy() { this.client.copy(); }
		public void clear() 
		{ 
			if (this.client.isEditable())
				this.client.replaceSelection(""); 
		}
		public void cut() { this.client.cut(); }
		public void paste() { this.client.paste(); }
		public void selectAll() 
		{ 
			this.client.select(0, this.client.getText().length()); 
		}
		
	}
}
