/*
 * DriverEditorDialog.java
 *
 * Created on 9. Juli 2002, 13:10
 */

package workbench.gui.profiles;

import workbench.WbManager;
import workbench.exception.WbException;
import workbench.resource.ResourceMgr;

public class DriverEditorDialog extends javax.swing.JDialog
{
	private javax.swing.JPanel dummyPanel;
	private javax.swing.JPanel buttonPanel;
	private javax.swing.JButton okButton;
	private workbench.gui.profiles.DriverlistEditorPanel driverListPanel;
	private javax.swing.JButton cancelButton;

	/** Creates new form DriverEditorDialog */
	public DriverEditorDialog(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			this.setSize(300,300);
		}
	}

	private void initComponents()
	{
		driverListPanel = new workbench.gui.profiles.DriverlistEditorPanel();
		buttonPanel = new javax.swing.JPanel();
		okButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();
		dummyPanel = new javax.swing.JPanel();

		setTitle(ResourceMgr.getString("TxtDriverEditorWindowTitle"));
		setModal(true);
		setName("DriverEditorDialog");
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});


		driverListPanel.setBorder(new javax.swing.border.EtchedBorder());
		getContentPane().add(driverListPanel, java.awt.BorderLayout.CENTER);

		buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		okButton.setFont(null);
		okButton.setMnemonic('O');
		okButton.setText(ResourceMgr.getString(ResourceMgr.TXT_OK));
		okButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				okButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(okButton);

		cancelButton.setText(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cancelButtonActionPerformed(evt);
			}
		});

		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, java.awt.BorderLayout.SOUTH);

		dummyPanel.setMaximumSize(new java.awt.Dimension(2, 2));
		dummyPanel.setMinimumSize(new java.awt.Dimension(1, 1));
		dummyPanel.setPreferredSize(new java.awt.Dimension(2, 2));
		getContentPane().add(dummyPanel, java.awt.BorderLayout.NORTH);

	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		this.closeDialog();
	}

	private void okButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		try
		{
			this.driverListPanel.saveItem();
			this.closeDialog();
		}
		catch (WbException e)
		{
			e.printStackTrace();
		}
	}

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)
	{
		this.closeDialog();
	}

	public void closeDialog()
	{
		WbManager.getSettings().storeWindowSize(this);
		setVisible(false);
		dispose();
	}
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		new DriverEditorDialog(new javax.swing.JFrame(), true).show();
	}
}
