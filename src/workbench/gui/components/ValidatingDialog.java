/*
 * ValidatingDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.Border;

import workbench.WbManager;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.gui.actions.WbAction;


/**
 * @author  Thomas Kellerer
 */
public class ValidatingDialog
	extends JDialog
	implements WindowListener, ActionListener
{
	protected ValidatingComponent validator;
	protected JComponent editorComponent;
	private JButton[] optionButtons;

	private JButton cancelButton;
	private boolean isCancelled = true;
	private int selectedOption = -1;
	private EscAction esc;
	private JPanel buttonPanel;

	public ValidatingDialog(Dialog owner, String title, JComponent editor)
	{
		super(owner, title, true);
		init(editor, new String[] { ResourceMgr.getString("LblOK") }, true);
	}

	public ValidatingDialog(Frame owner, String title, JComponent editor)
	{
		this(owner, title, editor, true);
	}

	public ValidatingDialog(Frame owner, String title, JComponent editor, boolean addCancelButton)
	{
		super(owner, title, true);
		init(editor, new String[] { ResourceMgr.getString("LblOK") }, addCancelButton);
	}

	public ValidatingDialog(Frame owner, String title, JComponent editor, String[] options, boolean modal)
  {
    super(owner, title, modal);
    init(editor, options, true);
  }

	public ValidatingDialog(Dialog owner, String title, JComponent editor, boolean addCancelButton)
	{
		super(owner, title, true);
		init(editor, new String[] { ResourceMgr.getString("LblOK") }, addCancelButton);
	}

	public ValidatingDialog(Dialog owner, String title, JComponent editor, String[] options)
	{
		this(owner, title, editor, options, true);
	}

	public ValidatingDialog(Dialog owner, String title, JComponent editor, String[] options, boolean addCancelButton)
  {
		super(owner, title, true);
		init(editor, options, addCancelButton);
	}

	public void setDefaultButton(int index)
	{
		JRootPane root = this.getRootPane();
		if (index >= optionButtons.length && cancelButton != null)
		{
			root.setDefaultButton(cancelButton);
		}
		else
		{
			root.setDefaultButton(optionButtons[index]);
		}
	}

	private void init(JComponent editor, String[] options, boolean addCancelButton)
	{
		if (editor instanceof ValidatingComponent)
		{
			this.validator = (ValidatingComponent)editor;
		}
		this.editorComponent = editor;
		this.optionButtons = new JButton[options.length];
		for (int i = 0; i < options.length; i++)
		{
			this.optionButtons[i] = new WbButton(options[i]);
			String label = optionButtons[i].getText();
			if (label.equals("OK"))
			{
				optionButtons[i].setName("ok");
			}
			this.optionButtons[i].addActionListener(this);
		}

		if (addCancelButton)
		{
			this.cancelButton = new WbButton(ResourceMgr.getString("LblCancel"));
			this.cancelButton.setName("cancel");
			this.cancelButton.addActionListener(this);
		}

		JRootPane root = this.getRootPane();
		root.setDefaultButton(optionButtons[0]);

    esc = new EscAction(this, this);

		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		Border b = BorderFactory.createEmptyBorder(5,5,5,5);
		content.setBorder(b);
		content.add(editor, BorderLayout.CENTER);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 5, 0));

		int length = optionButtons.length + (cancelButton == null ? 0 : 1);
		JComponent[] allButtons = new JComponent[length];
		for (int i=0; i < optionButtons.length; i++)
		{
			buttonPanel.add(optionButtons[i]);
			allButtons[i] = optionButtons[i];
		}

		if (cancelButton != null)
		{
			buttonPanel.add(cancelButton);
			allButtons[allButtons.length - 1] = cancelButton;
		}

		b = BorderFactory.createEmptyBorder(2, 0, 0, 0);

		WbSwingUtilities.makeEqualWidth(allButtons);
		buttonPanel.setBorder(b);
		content.add(buttonPanel, BorderLayout.SOUTH);
		this.getContentPane().add(content);
		this.doLayout();
		this.pack();
		this.addWindowListener(this);
	}

  public WbAction getESCAction()
  {
    return esc;
  }

	public int getSelectedOption()
	{
		return this.selectedOption;
	}

	public boolean isCancelled()
	{
		return this.isCancelled;
	}

	public static boolean showConfirmDialog(Window parent, JComponent editor, String title)
	{
		return showConfirmDialog(parent, editor, title, null, 0, false);
	}

	public static boolean showOKCancelDialog(Dialog parent, JComponent editor, String title)
	{
		ValidatingDialog dialog = new ValidatingDialog(parent, title, editor, true);
		WbSwingUtilities.center(dialog, parent);
		dialog.setDefaultButton(0);
		dialog.setVisible(true);
		return !dialog.isCancelled();
	}

	public static boolean showConfirmDialog(Window parent, JComponent editor, String title, int defaultButton)
	{
		return showConfirmDialog(parent, editor, title, null, defaultButton, false);
	}

	public static ValidatingDialog createDialog(Window parent, JComponent editor, String title, Component reference, int defaultButton, boolean centeredButtons)
	{
		ValidatingDialog dialog = null;
		if (parent == null)
		{
			dialog = new ValidatingDialog(WbManager.getInstance().getCurrentWindow(), title, editor);
		}
		else
		{
			if (parent instanceof Frame)
			{
				dialog = new ValidatingDialog((Frame) parent, title, editor);
			}
			else if (parent instanceof Dialog)
			{
				dialog = new ValidatingDialog((Dialog) parent, title, editor);
			}
			else
			{
				throw new IllegalArgumentException("Parent component must be Dialog or Frame");
			}
		}

		if (reference != null)
		{
			WbSwingUtilities.center(dialog, reference);
		}
		else
		{
			WbSwingUtilities.center(dialog, parent);
		}

		dialog.setDefaultButton(defaultButton);
		if (centeredButtons)
		{
			dialog.buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		}
		return dialog;
	}

	public static boolean showConfirmDialog(Window parent, JComponent editor, String title, Component reference, int defaultButton, boolean centeredButtons)
	{
		ValidatingDialog dialog = createDialog(parent, editor, title, reference, defaultButton, centeredButtons);
		dialog.setVisible(true);

		return !dialog.isCancelled();
	}

	public void close()
	{
		this.setVisible(false);
		this.dispose();
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
		if (validator == null) editorComponent.requestFocusInWindow();
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		this.close();
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				if (validator != null)
				{
					validator.componentDisplayed();
				}
				else
				{
					editorComponent.requestFocus();
				}
			}
		});
	}

	public void approveAndClose()
	{
		this.selectedOption = 0;
		this.isCancelled = false;
		this.close();
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.cancelButton || e.getSource() == this.esc)
		{
			this.selectedOption = -1;
			this.isCancelled = true;
			this.close();
		}
		else
		{
			for (int i = 0; i < optionButtons.length; i++)
			{
				if (e.getSource() == optionButtons[i])
				{
					this.selectedOption = i;
					break;
				}
			}
			if (validator == null || this.validator.validateInput())
			{
				this.isCancelled = false;
				this.close();
			}
		}
	}

}
