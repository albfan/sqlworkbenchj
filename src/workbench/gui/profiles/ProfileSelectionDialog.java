/*
 * ProfileSelectionDialog.java
 *
 * Created on 1. Juli 2002, 22:55
 */

package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;


/**
 *
 * @author  workbench@kellerer.org
 */
public class ProfileSelectionDialog extends JDialog implements ActionListener, WindowListener
{
  private JPanel buttonPanel;
  private JButton okButton;
  private JButton cancelButton;
	private ProfileEditorPanel profiles;
	private ConnectionProfile selectedProfile;
	private int selectedIndex = -1;
	private boolean cancelled = false;
	private String escActionCommand;

	/** Creates new form ProfileSelectionDialog */
	public ProfileSelectionDialog(Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		
		JRootPane root = this.getRootPane();
		root.setDefaultButton(okButton);		
		InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = root.getActionMap();
		EscAction esc = new EscAction(this);
		escActionCommand = esc.getActionName();
		im.put(esc.getAccelerator(), esc.getActionName());
		am.put(esc.getActionName(), esc);
		
		//this.getRootPane().setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, im);
		//this.getRootPane().setActionMap(am);
		
		
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				checkProfiles();
			}
		});

	}

	private void checkProfiles()
	{
		int count = this.profiles.getProfileCount();
		if (count == 0)
		{
			try
			{
				this.profiles.newItem(false);
			}
			catch (Exception e)
			{
			}
		}
	}

  private void initComponents()
  {
		profiles = new ProfileEditorPanel();
    buttonPanel = new JPanel();
    okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
    cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));

		addWindowListener(this);
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

    buttonPanel.add(okButton);
		okButton.addActionListener(this);

    buttonPanel.add(cancelButton);
		cancelButton.addActionListener(this);

		// dummy panel to create small top border...
		JPanel dummy = new JPanel();
		dummy.setMinimumSize(new Dimension(1, 1));
		profiles.addListMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent evt)
			{
				profileListClicked(evt);
			}
		});

		BorderLayout bl = new BorderLayout();
		this.getContentPane().setLayout(bl);
		getContentPane().add(dummy, BorderLayout.NORTH);
		getContentPane().add(profiles, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		
		setTitle(ResourceMgr.getString(ResourceMgr.TXT_SELECT_PROFILE));
		//this.setFocusTraversalPolicy(null);
		this.restoreSize();
  }

	/** Closes the dialog */
	private void closeDialog()
	{
		this.profiles.saveSettings();
		this.setVisible(false);
		//this.dispose();
	}

	public ConnectionProfile getSelectedProfile()
	{
		return this.selectedProfile;
	}

	public void restoreSize()
	{
		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			this.setSize(600,500);
		}
	}

	public void saveSize()
	{
		Settings s = WbManager.getSettings();
		s.storeWindowSize(this);
	}

	public void selectProfile()
	{
		this.selectedProfile = this.profiles.getSelectedProfile();
		this.cancelled = false;
		this.closeDialog();
	}

	public void profileListClicked(MouseEvent evt)
	{
		if (evt.getClickCount() == 2)
		{
			EventQueue.invokeLater(new Runnable()
			{
				public void run()
				{
					selectProfile();
				}
			});
		}
	}
	public void setInitialFocus()
	{
		profiles.setInitialFocus();
	}
	
	/** Invoked when an action occurs.
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.okButton)
		{
			this.selectProfile();
		}
		else if (e.getSource() == this.cancelButton ||
						e.getActionCommand().equals(escActionCommand))
		{
			this.selectedProfile = null;
			this.closeDialog();
		}
		this.saveSize();
	}

	public boolean isCancelled() { return this.cancelled;	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void setVisible(boolean aFlag)
	{
		super.setVisible(aFlag);
		if (aFlag) 
		{
			this.setInitialFocus();
		}
	}
	public void windowClosed(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		this.cancelled = true;
    this.closeDialog();
	}

	public void windowDeactivated(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowOpened(WindowEvent e)
	{
		this.cancelled = true;
		this.setInitialFocus();
	}

}
