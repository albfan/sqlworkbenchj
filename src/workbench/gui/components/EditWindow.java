/*
 * EditWindow.java
 *
 * Created on March 29, 2003, 12:34 AM
 */

package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;

import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;


/**
 *
 * @author  thomas
 */
public class EditWindow
	extends JDialog
	implements ActionListener, WindowListener
{
	
	private EditorPanel editor;
	private JButton okButton = new WbButton(ResourceMgr.getString("LabelOK"));
	private JButton cancelButton = new WbButton(ResourceMgr.getString("LabelCancel"));
	private boolean isCancelled = true;
	
	public EditWindow(Frame owner, String title, String text)
	{
		super(owner, title, true);
		this.getContentPane().setLayout(new BorderLayout());
		this.editor = EditorPanel.createTextEditor();
		this.editor.showFindOnPopupMenu();
		this.editor.showFormatSql();
		
		this.getContentPane().add(editor, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(this.okButton);
		buttonPanel.add(this.cancelButton);
		this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		this.editor.setText(text);
		this.editor.setMinimumSize(new Dimension(100,100));
		this.editor.setPreferredSize(new Dimension(300,200));
		this.editor.setCaretPosition(0);
		
		
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		
		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.setDefaultComponent(editor);
		pol.addComponent(editor);
		pol.addComponent(this.okButton);
		pol.addComponent(this.cancelButton);
		this.setFocusTraversalPolicy(pol);

		InputMap im = new ComponentInputMap(this.getRootPane());
		ActionMap am = new ActionMap();
		EscAction escAction = new EscAction(this);
		im.put(escAction.getAccelerator(), escAction.getActionName());
		am.put(escAction.getActionName(), escAction);
		
		this.getRootPane().setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, im);
		this.getRootPane().setActionMap(am);
		
		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			this.setSize(500,400);
		}

		this.addWindowListener(this);
		
		// pack() needs to be called before center() !!!
		WbSwingUtilities.center(this, owner);
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			this.isCancelled = false;
		}
		else if ("edit-ok".equals(e.getActionCommand()))
		{
			this.isCancelled = false;
		}
		this.hide();
	}

	public boolean isCancelled()
	{
		return this.isCancelled;
	}

	public String getText() 
	{
		return this.editor.getText();
	}

	public static void main(String[] args)
	{
		//WbManager.getInstance();
		EditWindow w = new EditWindow(null, "Test", "Hallo dies ist \nder Text!");
		w.show();
	}
	
	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowClosed(java.awt.event.WindowEvent e)
	{
		WbManager.getSettings().storeWindowSize(this);
	}
	
	public void windowClosing(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}
	
}
