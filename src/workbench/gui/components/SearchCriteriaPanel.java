/*
 * SearchCriteriaPanel.java
 *
 * Created on August 20, 2003, 11:20 PM
 */

package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.GridLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import workbench.WbManager;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  thomas
 */
public class SearchCriteriaPanel
	extends JPanel
{
	private static final String PROP_CLASS = "workbench.sql.search";
	private static final String PROP_KEY_CASE = "ignoreCase";
	private static final String PROP_KEY_CRIT = "lastValue";
	private JCheckBox ignoreCase;
	
	private JTextField criteria;
	private JLabel label;
	
	public SearchCriteriaPanel()
	{
		this(null);
	}
	
	public SearchCriteriaPanel(String initialValue)
	{
		this.ignoreCase = new JCheckBox(ResourceMgr.getString("LabelSearchIgnoreCase"));
		this.ignoreCase.setSelected("true".equals(WbManager.getSettings().getProperty(PROP_CLASS, PROP_KEY_CASE, "true")));
	
		this.label = new JLabel(ResourceMgr.getString("LabelSearchCriteria"));
		this.criteria = new JTextField();
		this.criteria.setColumns(40);
		if (initialValue == null)
		{
			initialValue = WbManager.getSettings().getProperty(PROP_CLASS, PROP_KEY_CRIT, null);
		}
		this.criteria.setText(initialValue);
		if (initialValue != null)
		{
			this.criteria.selectAll();
		}
		this.criteria.addMouseListener(new TextComponentMouseListener());
		
		this.setLayout(new BorderLayout());
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout(5,0));
		p.add(this.label, BorderLayout.WEST);
		p.add(this.criteria, BorderLayout.CENTER);
		this.add(p,BorderLayout.CENTER);
		this.add(this.ignoreCase, BorderLayout.SOUTH);
	}

	public String getCriteria()
	{
		return this.criteria.getText();
	}
	
	public boolean getIgnoreCase()
	{
		return this.ignoreCase.isSelected();
	}

	public void setSearchCriteria(String aValue)
	{
		this.criteria.setText(aValue);
		if (aValue != null)
		{
			this.criteria.selectAll();
		}
	}
	
	public boolean showFindDialog(Component caller)
	{
		EventQueue.invokeLater(new Runnable() {
			public void run()
			{
				criteria.grabFocus();
			}
		});
		String title = ResourceMgr.getString("TxtWindowTitleSearchText");
		int choice = JOptionPane.showConfirmDialog(caller, this, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		WbManager.getSettings().setProperty(PROP_CLASS, PROP_KEY_CASE, Boolean.toString(this.getIgnoreCase()));
		WbManager.getSettings().setProperty(PROP_CLASS, PROP_KEY_CRIT, this.getCriteria());
		if (choice == JOptionPane.CANCEL_OPTION) return false;
		return true;
	}
	
}
