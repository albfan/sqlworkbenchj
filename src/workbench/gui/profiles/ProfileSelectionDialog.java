/*
 * ProfileSelectionDialog.java
 *
 * Created on 1. Juli 2002, 22:55
 */

package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import workbench.WbManager;
import workbench.db.ConnectionProfile;
import workbench.gui.actions.EscAction;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author  sql.workbench@freenet.de
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
	private String escActionCommand;

	/** Creates new form ProfileSelectionDialog */
	public ProfileSelectionDialog(java.awt.Frame parent, boolean modal)
	{
		super(parent, modal);
		initComponents();
		InputMap im = new ComponentInputMap(this.profiles);
		ActionMap am = new ActionMap();
		EscAction esc = new EscAction(this);
		escActionCommand = esc.getActionName();
		im.put(esc.getAccelerator(), esc.getActionName());
		am.put(esc.getActionName(), esc);
		this.profiles.setInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW, im);
		this.profiles.setActionMap(am);
	}

  private void initComponents()
  {
		profiles = new ProfileEditorPanel();
    buttonPanel = new javax.swing.JPanel();
    okButton = new javax.swing.JButton();
    cancelButton = new javax.swing.JButton();

		/*
    addWindowListener(new java.awt.event.WindowAdapter()
    {
      public void windowClosing(java.awt.event.WindowEvent evt)
      {
        closeDialog(evt);
      }
    });
		*/
		addWindowListener(this);
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
		profiles.addListMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(java.awt.event.MouseEvent evt)
			{
				profileListClicked(evt);
			}
		});		
		
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
		this.setVisible(false);
		this.dispose();
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

	public void selectProfile()
	{
		this.selectedProfile = this.profiles.getSelectedProfile();
		this.cancelled = false;
		this.setVisible(false);
		this.dispose();
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
			this.cancelled = true;
			this.setVisible(false);
		}
		else
		{
			System.out.println("command=" + e.getActionCommand());
		}
		this.saveSize();
	}

	public boolean isCancelled() { return this.cancelled;	}

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
		this.cancelled = true;
    this.closeDialog(e);
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

