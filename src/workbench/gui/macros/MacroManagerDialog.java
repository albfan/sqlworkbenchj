/*
 * MacroManagerDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionListener;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbCheckBox;
import workbench.gui.sql.SqlPanel;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 * A Dialog that displays a {@link MacroManagerGui} and allows to run 
 * the selected macro.
 */
public class MacroManagerDialog
	extends JDialog
	implements ActionListener, ListSelectionListener, MouseListener, WindowListener
{
	private JPanel dummyPanel;
	private JPanel buttonPanel;
	private JList macroList;
	private JButton okButton;
	private JButton runButton;
	private MacroManagerGui macroPanel;
	private JButton cancelButton;
  private boolean cancelled = true;
	private EscAction escAction;
	private SqlPanel client;
	private JCheckBox replaceEditorText;

	public MacroManagerDialog(Frame parent, SqlPanel aTarget)
	{
		super(parent, true);
		this.client = aTarget;
		this.initComponents();
		this.initWindow(parent);
		boolean connected = this.client.isConnected();
		boolean busy = this.client.isBusy();
		this.runButton.setEnabled(connected && !busy);
		this.runButton.setVisible(connected && !busy);

		this.replaceEditorText.setVisible(connected && !busy);
		this.replaceEditorText.setEnabled(connected && !busy);
		this.initKeys();
		this.addWindowListener(this);
	}

	private void initWindow(Frame parent)
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			this.setSize(600,400);
		}
		WbSwingUtilities.center(this, parent);
		boolean replace = Settings.getInstance().getBoolProperty("workbench.gui.macros.replaceOnRun", false);
		this.replaceEditorText.setSelected(replace);
	}

	private void initKeys()
	{
		if (this.runButton.isEnabled())
		{
			this.getRootPane().setDefaultButton(this.runButton);
		}
		else
		{
			this.getRootPane().setDefaultButton(this.okButton);
		}
		InputMap im = this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap am = this.getRootPane().getActionMap();
		escAction = new EscAction(this);
		im.put(escAction.getAccelerator(), escAction.getActionName());
		am.put(escAction.getActionName(), escAction);
	}

	private void initComponents()
	{
		macroPanel = new MacroManagerGui();
		buttonPanel = new JPanel();
		runButton = new WbButton(ResourceMgr.getString("LblRunMacro"));
		runButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosRun"));

		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		okButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosOK"));

		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
		cancelButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosCancel"));
		dummyPanel = new JPanel();

		setTitle(ResourceMgr.getString("TxtMacroManagerWindowTitle"));
		setModal(true);
		setName("MacroManagerDialog");
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		macroPanel.setBorder(new CompoundBorder(new EmptyBorder(1,1,1,1), new EtchedBorder()));
		getContentPane().add(macroPanel, BorderLayout.CENTER);

		this.replaceEditorText = new WbCheckBox(ResourceMgr.getString("LblReplaceCurrentSql"));
		this.replaceEditorText.setToolTipText(ResourceMgr.getDescription("LblReplaceCurrentSql"));

		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		p.add(this.replaceEditorText);
		p.add(new JPanel());

		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(p);

		runButton.addActionListener(this);
		runButton.setEnabled(false);
		buttonPanel.add(runButton);

		okButton.addActionListener(this);
		buttonPanel.add(okButton);

		cancelButton.addActionListener(this);
		buttonPanel.add(cancelButton);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		dummyPanel.setMaximumSize(new Dimension(2, 2));
		dummyPanel.setMinimumSize(new Dimension(1, 1));
		dummyPanel.setPreferredSize(new Dimension(2, 2));
		getContentPane().add(dummyPanel, BorderLayout.NORTH);

		this.macroList = macroPanel.getMacroList();
		this.macroList.addMouseListener(this);
		this.macroList.addListSelectionListener(this);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == okButton)
		{
			okButtonActionPerformed(e);
		}
		if (e.getSource() == runButton)
		{
			runButtonActionPerformed(e);
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
			this.macroPanel.saveItem();
			this.cancelled = false;
			this.closeDialog();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void runButtonActionPerformed(ActionEvent evt)
	{
		this.runSelectedMacro();
	}

	private void runSelectedMacro()
	{
		try
		{
			this.macroPanel.saveItem();
			this.cancelled = false;
			this.closeDialog();
			if (this.client != null)
			{
				String name = this.macroPanel.getSelectedMacroName();
				this.client.executeMacro(name, this.replaceEditorText.isSelected());
			}
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
		Settings.getInstance().storeWindowSize(this);
		macroPanel.saveSettings();
		Settings.getInstance().setProperty("workbench.gui.macros.replaceOnRun", this.replaceEditorText.isSelected());
		setVisible(false);
	}

	public void valueChanged(javax.swing.event.ListSelectionEvent e)
	{
		if (e.getValueIsAdjusting()) return;
		boolean selected = (e.getFirstIndex() > -1) && this.macroList.getModel().getSize() > 0;
		//this.okButton.setEnabled(selected);
		this.runButton.setEnabled(selected);
	}

	public void mouseClicked(java.awt.event.MouseEvent e)
	{
		if (e.getSource() == this.macroList)
		{
			if (e.getClickCount() == 2 && this.runButton.isEnabled())
			{
				this.runSelectedMacro();
			}
		}
	}

	public void mouseEntered(java.awt.event.MouseEvent e)
	{
	}

	public void mouseExited(java.awt.event.MouseEvent e)
	{
	}

	public void mousePressed(java.awt.event.MouseEvent e)
	{
	}

	public void mouseReleased(java.awt.event.MouseEvent e)
	{
	}

	public void windowOpened(WindowEvent windowEvent)
	{
		// Fix for JDK 6
		// It seems that the macro editor is not repainted 
		// correctly if we load the macro text while
		// it's not visible
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				macroPanel.restoreSettings();
			}
		});
	}

	public void windowClosing(WindowEvent windowEvent)
	{
	}

	public void windowClosed(WindowEvent windowEvent)
	{
	}

	public void windowIconified(WindowEvent windowEvent)
	{
	}

	public void windowDeiconified(WindowEvent windowEvent)
	{
	}

	public void windowActivated(WindowEvent windowEvent)
	{
	}

	public void windowDeactivated(WindowEvent windowEvent)
	{
	}

}
