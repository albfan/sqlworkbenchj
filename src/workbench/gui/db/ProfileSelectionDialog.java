/*
 * ProfileSelectionDialog.java
 *
 * Created on 1. Juli 2002, 22:55
 */

package workbench.gui.db;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import workbench.db.ConnectionProfile;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  thomas.kellerer@web.de
 */
public class ProfileSelectionDialog 
	extends javax.swing.JDialog
	implements ActionListener
{
  private javax.swing.JPanel buttonPanel;
  private javax.swing.JButton okButton;
  private javax.swing.JButton cancelButton;
	private ProfileEditorPanel profiles;
	private ConnectionProfile selectedProfile;
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

    okButton.setText("OK");
    buttonPanel.add(okButton);
		okButton.addActionListener(this);
		
    cancelButton.setText("Cancel");
    buttonPanel.add(cancelButton);
		cancelButton.addActionListener(this);
		
		this.setSize(400, 300);
		getContentPane().add(profiles, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);
		setTitle(ResourceMgr.getString(ResourceMgr.TXT_SELECT_PROFILE));
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
	}

	public boolean isCancelled() { return this.cancelled;	}
	
}

