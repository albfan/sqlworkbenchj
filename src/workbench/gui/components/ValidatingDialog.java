/*
 * ValidatingDialog.java
 *
 * Created on August 21, 2004, 12:47 PM
 */

package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ValidatingDialog
	extends JDialog
	implements WindowListener, ActionListener
{
	private ValidatingComponent validator = null;
	private JButton okButton;
	private JButton cancelButton;
	private boolean isCancelled = true;
	
	/** Creates a new instance of ValidatingDialog */
	public ValidatingDialog(Frame owner, String title, JPanel editor)
	{
		super(owner, title, true);
		if (!(editor instanceof ValidatingComponent))
		{
			throw new IllegalArgumentException("The supplied panel does not implement the ValidatingComponent interface");
		}
		this.validator = (ValidatingComponent)editor;
		this.okButton = new WbButton(ResourceMgr.getString("LabelOK"));
		this.okButton.addActionListener(this);
		this.cancelButton = new WbButton(ResourceMgr.getString("LabelCancel"));
		this.cancelButton.addActionListener(this);
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		Border b = BorderFactory.createEmptyBorder(10,10,10,10);
		content.setBorder(b);
		content.add(editor, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(okButton);
		buttonPanel.add(cancelButton);
		b = BorderFactory.createEmptyBorder(2, 0, 0, 0);
		buttonPanel.setBorder(b);
		content.add(buttonPanel, BorderLayout.SOUTH);
		this.getContentPane().add(content);
		this.doLayout();
		this.pack();
		this.addWindowListener(this);
		WbSwingUtilities.center(this, owner);
	}
	
	public boolean isCancelled()
	{
		return this.isCancelled;
	}
	
	public static boolean showConfirmDialog(Frame parent, JPanel editor, String title)
	{
		ValidatingDialog dialog = new ValidatingDialog(parent, title, editor);
		dialog.show();
		return !dialog.isCancelled();
	}
	
	private void close()
	{
		this.hide();
		this.dispose();
	}
	
	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}
	
	public void windowClosing(java.awt.event.WindowEvent e)
	{
		this.close();
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
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				validator.componentDisplayed();
			}
		});
	}
	
	public void actionPerformed(java.awt.event.ActionEvent e)
	{
		if (e.getSource() == this.cancelButton)
		{
			this.isCancelled = true;
			this.close();
		}
		if (e.getSource() == this.okButton)
		{
			if (this.validator.validateInput())
			{
				this.isCancelled = false;
				this.close();
			}
		}
	}
	
}
