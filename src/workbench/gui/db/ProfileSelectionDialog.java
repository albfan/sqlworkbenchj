/*
 * ProfileSelectionDialog.java
 *
 * Created on 1. Juli 2002, 22:55
 */

package workbench.gui.db;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JPanel;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  thomas.kellerer@web.de
 */
public class ProfileSelectionDialog 
	extends javax.swing.JDialog
	implements ActionListener, WindowListener
{
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton okButton;
  private javax.swing.JButton cancelButton;
	private ProfileEditorPanel profiles;
	private ConnectionProfile selectedProfile;
	private int selectedIndex = -1;
	private boolean cancelled = false;
	
	/** Creates new form ProfileSelectionDialog */
	public ProfileSelectionDialog(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
	}

  private void initComponents()
  {
		profiles = new ProfileEditorPanel();
    buttonPanel = new javax.swing.JPanel();
    okButton = new javax.swing.JButton();
    cancelButton = new javax.swing.JButton();

    addWindowListener(new java.awt.event.WindowAdapter()
    {
      public void windowClosing(java.awt.event.WindowEvent evt)
      {
        closeDialog(evt);
      }
    });

    buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

    okButton.setText(ResourceMgr.getString(ResourceMgr.TXT_OK));
    buttonPanel.add(okButton);
		okButton.addActionListener(this);
		
    cancelButton.setText(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
    buttonPanel.add(cancelButton);
		cancelButton.addActionListener(this);

		// dummy panel to create small top border...
		JPanel dummy = new JPanel();
		dummy.setMinimumSize(new Dimension(1, 1));
		
		BorderLayout bl = new BorderLayout();
		this.getContentPane().setLayout(bl);
		getContentPane().add(dummy, BorderLayout.NORTH);
		getContentPane().add(profiles, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
		getRootPane().setDefaultButton(okButton);
		setTitle(ResourceMgr.getString(ResourceMgr.TXT_SELECT_PROFILE));
		this.restoreSize();
  }

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		setVisible(false);
	}

	public ConnectionProfile getSelectedProfile()
	{
		return this.selectedProfile;
	}
	
	public void restoreSize()
	{
		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			this.setSize(400,400);
		}
	}
	
	public void saveSize()
	{
		Settings s = WbManager.getSettings();
		s.storeWindowSize(this);
	}

	
	/** Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			this.selectedProfile = this.profiles.getSelectedProfile();
			this.cancelled = false;
			this.setVisible(false);
		}
		else if (e.getSource() == this.cancelButton)
		{
			this.selectedProfile = null;
			this.cancelled = true;
			this.setVisible(false);
		}
		this.saveSize();
	}

	public boolean isCancelled() { return this.cancelled;	}
	
	/** Invoked when the Window is set to be the active Window. Only a Frame or
	 * a Dialog can be the active Window. The native windowing system may
	 * denote the active Window or its children with special decorations, such
	 * as a highlighted title bar. The active Window is always either the
	 * focused Window, or the first Frame or Dialog that is an owner of the
	 * focused Window.
	 */
	public void windowActivated(WindowEvent e)
	{
	}
	
	/** Invoked when a window has been closed as the result
	 * of calling dispose on the window.
	 */
	public void windowClosed(WindowEvent e)
	{
	}
	
	/** Invoked when the user attempts to close the window
	 * from the window's system menu.  If the program does not
	 * explicitly hide or dispose the window while processing
	 * this event, the window close operation will be cancelled.
	 */
	public void windowClosing(WindowEvent e)
	{
	}
	
	/** Invoked when a Window is no longer the active Window. Only a Frame or a
	 * Dialog can be the active Window. The native windowing system may denote
	 * the active Window or its children with special decorations, such as a
	 * highlighted title bar. The active Window is always either the focused
	 * Window, or the first Frame or Dialog that is an owner of the focused
	 * Window.
	 */
	public void windowDeactivated(WindowEvent e)
	{
	}
	
	/** Invoked when a window is changed from a minimized
	 * to a normal state.
	 */
	public void windowDeiconified(WindowEvent e)
	{
	}
	
	/** Invoked when a window is changed from a normal to a
	 * minimized state. For many platforms, a minimized window
	 * is displayed as the icon specified in the window's
	 * iconImage property.
	 * @see java.awt.Frame#setIconImage
	 */
	public void windowIconified(WindowEvent e)
	{
	}
	
	/** Invoked the first time a window is made visible.
	 */
	public void windowOpened(WindowEvent e)
	{
		this.cancelled = true;
	}
	
}

