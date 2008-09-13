/*
 * ValidatingDialog.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.border.Border;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.EscAction;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;


/**
 * @author  support@sql-workbench.net
 */
public class ValidatingDialog
	extends JDialog
	implements WindowListener, ActionListener
{
	protected ValidatingComponent validator = null;
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
		init(owner, editor, new String[] { ResourceMgr.getString("LblOK") }, true);
	}
	
	public ValidatingDialog(Frame owner, String title, JComponent editor)
	{
		this(owner, title, editor, true);
	}
	
	public ValidatingDialog(Frame owner, String title, JComponent editor, boolean addCancelButton)
	{
		super(owner, title, true);
		init(owner, editor, new String[] { ResourceMgr.getString("LblOK") }, addCancelButton);
	}

	public ValidatingDialog(Dialog owner, String title, JComponent editor, String[] options)
	{
		this(owner, title, editor, options, true);
	}
	
	public ValidatingDialog(Dialog owner, String title, JComponent editor, String[] options, boolean addCancelButton)
	{
		super(owner, title, true);
		init(owner, editor, options, addCancelButton);
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
	
	private void init(Window owner, JComponent editor, String[] options, boolean addCancelButton)
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

		if (addCancelButton)
		{
			esc = new EscAction(this, this);
			//esc.addToInputMap(editor);
		}
		
		JPanel content = new JPanel();
		content.setLayout(new BorderLayout());
		Border b = BorderFactory.createEmptyBorder(10,10,10,10);
		content.setBorder(b);
		content.add(editor, BorderLayout.CENTER);
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		for (int i = 0; i < optionButtons.length; i++)
		{
			buttonPanel.add(optionButtons[i]);
		}
		if (cancelButton != null) buttonPanel.add(cancelButton);
		b = BorderFactory.createEmptyBorder(2, 0, 0, 0);
		buttonPanel.setBorder(b);
		content.add(buttonPanel, BorderLayout.SOUTH);
		this.getContentPane().add(content);
		this.doLayout();
		this.pack();
		this.addWindowListener(this);
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
	
	public static boolean showConfirmDialog(Window parent, JComponent editor, String title, int defaultButton)
	{
		return showConfirmDialog(parent, editor, title, null, defaultButton, false);
	}
	
	public static boolean showConfirmDialog(Window parent, JComponent editor, String title, Component reference, int defaultButton, boolean centeredButtons)
	{
		ValidatingDialog dialog = null;
		if (parent == null)
		{
			dialog = new ValidatingDialog(WbManager.getInstance().getCurrentWindow(), title, editor);
		}
		else
		{
			if (parent instanceof Frame) 
				dialog = new ValidatingDialog((Frame)parent, title, editor);
			else if (parent instanceof Dialog)
				dialog = new ValidatingDialog((Dialog)parent, title, editor);
			else 
				throw new IllegalArgumentException("Parent component must be Dialog or Frame");
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
		dialog.setVisible(true);
		
		return !dialog.isCancelled();
	}
	
	private void close()
	{
		this.setVisible(false);
		this.dispose();
	}
	
	public void windowActivated(WindowEvent e)
	{
	}
	
	public void windowClosed(WindowEvent e)
	{
	}
	
	public void windowClosing(WindowEvent e)
	{
		this.close();
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
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				editorComponent.grabFocus();
				if (validator != null) validator.componentDisplayed();
			}
		});
	}
	
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
