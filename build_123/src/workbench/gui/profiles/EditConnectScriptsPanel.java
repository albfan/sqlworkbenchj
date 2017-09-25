/*
 * EditConnectScriptsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionProfile;
import workbench.db.KeepAliveDaemon;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbTraversalPolicy;
import workbench.gui.sql.EditorPanel;

/**
 * @author Thomas Kellerer
 */
public class EditConnectScriptsPanel
	extends JPanel
	implements ValidatingComponent
{
	private EditorPanel postConnectEditor;
	private EditorPanel preDisconnectEditor;
	private EditorPanel keepAliveScriptEditor;
	private JTextField keepAliveInterval;

	public EditConnectScriptsPanel(ConnectionProfile profile)
	{
		super();
		if (profile == null) throw new NullPointerException("Null profile specified!");

		this.setLayout(new GridBagLayout());

		JPanel p1 = new JPanel(new BorderLayout(0,5));
		JLabel l1 = new JLabel(ResourceMgr.getString("LblPostConnectScript"));
		postConnectEditor = EditorPanel.createSqlEditor();
		postConnectEditor.setText(profile.getPostConnectScript());
		Dimension d = new Dimension(200,200);
		postConnectEditor.setPreferredSize(d);
		p1.add(l1, BorderLayout.NORTH);
		p1.add(postConnectEditor, BorderLayout.CENTER);

		JPanel p2 = new JPanel(new BorderLayout(0,5));
		JLabel l2 = new JLabel(ResourceMgr.getString("LblPreDisconnectScript"));
		preDisconnectEditor = EditorPanel.createSqlEditor();
		preDisconnectEditor.setText(profile.getPreDisconnectScript());
		preDisconnectEditor.setPreferredSize(d);
		preDisconnectEditor.setCaretVisible(false);
		p2.add(preDisconnectEditor, BorderLayout.CENTER);
		p2.add(l2, BorderLayout.NORTH);

		JPanel p3 = new JPanel(new BorderLayout(0,5));
		JLabel l3 = new JLabel(ResourceMgr.getString("LblIdleScript"));
		keepAliveScriptEditor = EditorPanel.createSqlEditor();
		keepAliveScriptEditor.setText(profile.getIdleScript());
		keepAliveScriptEditor.setPreferredSize(d);
		keepAliveScriptEditor.setCaretVisible(false);
		p3.add(keepAliveScriptEditor, BorderLayout.CENTER);
		p3.add(l3, BorderLayout.NORTH);
		JPanel p4 = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel time = new JLabel(ResourceMgr.getString("LblIdleScriptTime"));
		time.setToolTipText(ResourceMgr.getDescription("LblIdleScriptTime"));
		p4.add(time);

		keepAliveInterval = new JTextField(8);
		keepAliveInterval.setText(KeepAliveDaemon.getTimeDisplay(profile.getIdleTime()));
		p4.add(keepAliveInterval);
		p3.add(p4, BorderLayout.SOUTH);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 0.5;
		c.fill = GridBagConstraints.BOTH;
		this.add(p1, c);

		c.gridy ++;
		c.insets = WbSwingUtilities.getEmptyInsets();
		this.add(p2, c);

		c.gridy ++;
		this.add(p3, c);

		WbTraversalPolicy pol = new WbTraversalPolicy();
		pol.addComponent(this.postConnectEditor);
		pol.addComponent(this.preDisconnectEditor);
		pol.addComponent(this.keepAliveScriptEditor);
		pol.addComponent(this.keepAliveInterval);
		pol.setDefaultComponent(this.postConnectEditor);
		this.setFocusTraversalPolicy(pol);
		this.setFocusCycleRoot(false);
	}

	public String getKeepAliveScript()
	{
		return keepAliveScriptEditor.getText().trim();
	}

	private long getIdleTime()
	{
		String s = this.keepAliveInterval.getText().trim().toLowerCase();
		return KeepAliveDaemon.parseTimeInterval(s);
	}

	public String getPostConnectScript()
	{
		return postConnectEditor.getText().trim();
	}

	public String getPreDisconnectScript()
	{
		return preDisconnectEditor.getText().trim();
	}


	public static boolean editScripts(Dialog owner, ConnectionProfile profile)
	{
		EditConnectScriptsPanel p = new EditConnectScriptsPanel(profile);
		ValidatingDialog d = new ValidatingDialog(owner, ResourceMgr.getString("LblEditConnScripts"), p);
    p.keepAliveScriptEditor.addKeyBinding(d.getESCAction());
    p.postConnectEditor.addKeyBinding(d.getESCAction());
    p.preDisconnectEditor.addKeyBinding(d.getESCAction());

		boolean hasSize = Settings.getInstance().restoreWindowSize(d,"workbench.gui.connectscripts.window");
		if (!hasSize)
		{
				d.setSize(600,550);
		}
		WbSwingUtilities.center(d, owner);
		d.setVisible(true);
		Settings.getInstance().storeWindowSize(d, "workbench.gui.connectscripts.window");
		if (d.isCancelled()) return false;
		profile.setPreDisconnectScript(p.getPreDisconnectScript());
		profile.setPostConnectScript(p.getPostConnectScript());
		profile.setIdleScript(p.getKeepAliveScript());
		profile.setIdleTime(p.getIdleTime());
		return true;
	}

	@Override
	public boolean validateInput()
	{
		return true;
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	@Override
	public void componentDisplayed()
	{
		EventQueue.invokeLater(postConnectEditor::requestFocusInWindow);
	}
}
