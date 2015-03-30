/*
 * MacroManagerDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.macros;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.components.WbButton;
import workbench.gui.components.WbCheckBox;
import workbench.gui.components.WbLabelField;
import workbench.gui.editor.MacroExpander;
import workbench.gui.sql.SqlPanel;

import workbench.sql.macros.MacroDefinition;
import workbench.sql.macros.MacroManager;

import workbench.util.StringUtil;

/**
 * A Dialog that displays a {@link MacroManagerGui} and allows to run
 * the selected macro.
 *
 * @author Thomas Kellerer
 */
public class MacroManagerDialog
	extends JDialog
	implements ActionListener, TreeSelectionListener, MouseListener, WindowListener
{
	private JList macroList;
	private JButton okButton;
	private JButton runButton;
	private MacroManagerGui macroPanel;
	private JButton cancelButton;
	private boolean cancelled = true;
	private EscAction escAction;
	private SqlPanel client;
	private JCheckBox replaceEditorText;
	private final int macroClient;

	public MacroManagerDialog(Frame parent, SqlPanel aTarget, int clientId)
	{
		super(parent, true);
		this.client = aTarget;
		this.macroClient = clientId;
		this.initComponents();
		this.initWindow(parent);

		boolean connected = false;
		boolean busy = false;
		WbConnection conn = null;
		if (client != null)
		{
			conn = client.getConnection();
			connected = this.client.isConnected();
			busy = this.client.isBusy();
		}

		this.runButton.setEnabled(connected && !busy);
		this.runButton.setVisible(connected && !busy);

		this.replaceEditorText.setVisible(connected && !busy);
		this.replaceEditorText.setEnabled(connected && !busy);

		this.initKeys();
		this.addWindowListener(this);
		macroPanel.addTreeSelectionListener(this);
		macroPanel.setCurrentConnection(conn);
	}

	private void initWindow(Frame parent)
	{
		if (!Settings.getInstance().restoreWindowSize(this))
		{
			this.pack();
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
		escAction = new EscAction(this, this);
	}

	private void initComponents()
	{
		macroPanel = new MacroManagerGui(macroClient);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		runButton = new WbButton(ResourceMgr.getString("LblRunMacro"));
		runButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosRun"));

		okButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_OK));
		okButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosOK"));

		cancelButton = new WbButton(ResourceMgr.getString(ResourceMgr.TXT_CANCEL));
		cancelButton.setToolTipText(ResourceMgr.getDescription("LblManageMacrosCancel"));

		setTitle(ResourceMgr.getString("TxtMacroManagerWindowTitle"));
		setModal(true);
		setName("MacroManagerDialog");
		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		macroPanel.setBorder(new CompoundBorder(new EmptyBorder(1,1,1,1), BorderFactory.createEtchedBorder()));
		getContentPane().add(macroPanel, BorderLayout.CENTER);

		this.replaceEditorText = new WbCheckBox(ResourceMgr.getString("LblReplaceCurrentSql"));
		this.replaceEditorText.setToolTipText(ResourceMgr.getDescription("LblReplaceCurrentSql"));

		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		p.add(this.replaceEditorText);

		buttons.add(p);

		runButton.addActionListener(this);
		runButton.setEnabled(false);
		buttons.add(runButton);

		okButton.addActionListener(this);
		buttons.add(okButton);

		cancelButton.addActionListener(this);
		buttons.add(cancelButton);

		JPanel buttonPanel = new JPanel(new BorderLayout());
		String txt = ResourceMgr.getFormattedString("LblCurrMacros", MacroManager.getInstance().getMacros(macroClient).getCurrentMacroFilename());
		WbLabelField lbl = new WbLabelField(txt);
		lbl.setBorder(new EmptyBorder(0, 5, 0, 0));
		buttonPanel.add(lbl, BorderLayout.LINE_START);
		buttonPanel.add(buttons, BorderLayout.LINE_END);

		getContentPane().add(buttonPanel, BorderLayout.SOUTH);

		WbSwingUtilities.makeEqualWidth(runButton, okButton, cancelButton);
	}

	@Override
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
				MacroDefinition macro = this.macroPanel.getSelectedMacro();
				if (macro.getExpandWhileTyping())
				{
					MacroExpander expander = client.getEditor().getMacroExpander();
					expander.insertMacroText(macro.getText());
				}
				else
				{
					MacroRunner runner = new MacroRunner();
					runner.runMacro(macro, client, this.replaceEditorText.isSelected());
				}

			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isCancelled()
	{
		return this.cancelled;
	}

	private void closeDialog(WindowEvent evt)
	{
		this.closeDialog();
	}

	public void closeDialog()
	{
		Settings.getInstance().storeWindowSize(this);
		macroPanel.saveSettings();
		Settings.getInstance().setProperty("workbench.gui.macros.replaceOnRun", this.replaceEditorText.isSelected());
		macroPanel.dispose();
		setVisible(false);
	}

	@Override
	public void valueChanged(TreeSelectionEvent e)
	{
		TreePath[] paths = e.getPaths();
		int selected = 0;
		boolean selectedIsMacro = true;

		for (TreePath path : paths)
		{
			if (e.isAddedPath(path))
			{
				selected ++;
				MacroTreeNode node = (MacroTreeNode)path.getLastPathComponent();
				selectedIsMacro = selectedIsMacro && !node.getAllowsChildren();
				if (!node.getAllowsChildren())
				{
					MacroDefinition macro = (MacroDefinition)node.getDataObject();
					selectedIsMacro = selectedIsMacro && StringUtil.isNonBlank(macro.getText());
				}
			}
		}
		this.runButton.setEnabled(selected == 1 && selectedIsMacro);
	}

	@Override
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

	@Override
	public void mouseEntered(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mouseExited(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mousePressed(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(java.awt.event.MouseEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent windowEvent)
	{
		// Fix for JDK 6
		// It seems that the macro editor is not repainted
		// correctly if we load the macro text while
		// it's not visible
		EventQueue.invokeLater(macroPanel::restoreSettings);
	}

	@Override
	public void windowClosing(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowClosed(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowIconified(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowActivated(WindowEvent windowEvent)
	{
	}

	@Override
	public void windowDeactivated(WindowEvent windowEvent)
	{
	}

}
