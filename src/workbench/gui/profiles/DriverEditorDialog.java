/*
 * DriverEditorDialog.java
 *
 * Created on 9. Juli 2002, 13:10
 */

package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import workbench.WbManager;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.resource.ResourceMgr;

public class DriverEditorDialog extends JDialog
	implements ActionListener
{
	private JPanel dummyPanel;
	private JPanel buttonPanel;
	private JButton okButton;
	private DriverlistEditorPanel driverListPanel;
	private JButton cancelButton;
  private boolean cancelled = true;
	private EscAction escAction;

	/** Creates new form DriverEditorDialog */
	public DriverEditorDialog(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();

		this.getRootPane().setDefaultButton(this.okButton);
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getRootPane().getActionMap();
		escAction = new EscAction(this);
		im.put(escAction.getAccelerator(), escAction.getActionName());
		am.put(escAction.getActionName(), escAction);

		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			this.setSize(600,400);
		}
		driverListPanel.restoreSettings();
	}

	private void initComponents()
	{
		driverListPanel = new workbench.gui.profiles.DriverlistEditorPanel();
		buttonPanel = new JPanel();
		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
		dummyPanel = new JPanel();

		setTitle(ResourceMgr.getString("TxtDriverEditorWindowTitle"));
		setModal(true);
		setName("DriverEditorDialog");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});


		driverListPanel.setBorder(new EtchedBorder());
		getContentPane().add(driverListPanel, BorderLayout.CENTER);

		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

		okButton.setFont(null);
		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		dummyPanel.setMaximumSize(new Dimension(2, 2));
		dummyPanel.setMinimumSize(new Dimension(1, 1));
		dummyPanel.setPreferredSize(new Dimension(2, 2));
		getContentPane().add(dummyPanel, BorderLayout.NORTH);

	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == okButton)
		{
			okButtonActionPerformed(e);
		}
		else if (e.getSource() == cancelButton || e.getActionCommand().equals(escAction.getActionName()))
		{
			cancelButtonActionPerformed(e);
		}
	}
	private void cancelButtonActionPerformed(ActionEvent evt)
	{
    this.cancelled = true;
		this.closeDialog();
	}

	private void okButtonActionPerformed(ActionEvent evt)
	{
		try
		{
			this.driverListPanel.saveItem();
      this.cancelled = false;
			this.closeDialog();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

  public boolean isCancelled() { return this.cancelled; }

	/** Closes the dialog */
	private void closeDialog(WindowEvent evt)
	{
		this.closeDialog();
	}

	public void closeDialog()
	{
		WbManager.getSettings().storeWindowSize(this);
		driverListPanel.saveSettings();
		setVisible(false);
	}
	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[])
	{
		new DriverEditorDialog(new JFrame(), true).show();
	}
}