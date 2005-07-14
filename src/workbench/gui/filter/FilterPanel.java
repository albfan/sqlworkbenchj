/*
 * FilterPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.Border;
import workbench.resource.ResourceMgr;
import workbench.storage.ResultInfo;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.ComplexExpression;
import workbench.storage.filter.FilterExpression;
import workbench.storage.filter.OrExpression;
import workbench.util.WbPersistence;


/**
 * A Panel to display a filter dialog for a {@link workbench.storage.DataStore}
 * @author support@sql-workbench.net
 */
public class FilterPanel
	extends JPanel
	implements ActionListener
{
	private ResultInfo columnInfo;
	private List panelList = new ArrayList();
	private JPanel expressions = new JPanel();
	private JRadioButton andButton;
	private JRadioButton orButton;
	private JButton addButton;
	
	public FilterPanel(ResultInfo source)
	{
		columnInfo = source;
		this.setLayout(new BorderLayout());
		this.expressions.setLayout(new GridBagLayout());
		JScrollPane scroll = new JScrollPane(expressions);
		this.add(scroll, BorderLayout.CENTER);
		JPanel p = new JPanel(new GridBagLayout());
		andButton = new JRadioButton(ResourceMgr.getString("LabelFilterTypeAnd"));
		orButton = new JRadioButton(ResourceMgr.getString("LabelFilterTypeOr"));
		ButtonGroup g = new ButtonGroup();
		g.add(andButton);
		g.add(orButton);
		andButton.setSelected(true);
		
		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = 0;
		cons.gridy = 0;
		cons.anchor = GridBagConstraints.WEST;
		cons.weightx = 0;
		cons.weighty = 0;
		p.add(andButton, cons);
		
		cons.gridx++;
		p.add(orButton, cons);
		
		addButton = new JButton(ResourceMgr.getString("LabelFilterAddLine"));
		addButton.addActionListener(this);
		cons.gridx++;
		cons.weightx = 1.0;
		cons.anchor = GridBagConstraints.EAST;
		p.add(addButton, cons);
		this.add(p, BorderLayout.NORTH);
	}
	
	public FilterExpression getExpression()
	{
		int count = this.panelList.size();
		if (count == 0) return null;
		ComplexExpression result = null;
		if (andButton.isSelected())
			result = new AndExpression();
		else 
			result = new OrExpression();
		
		for (int i=0; i < count; i++)
		{
			PanelEntry entry = (PanelEntry)panelList.get(i);
			result.addExpression(entry.expressionPanel.getExpression());
		}
		return result;
	}
	

	private void addExpressionPanel()
	{
		ColumnExpressionPanel exp = new ColumnExpressionPanel(columnInfo);
		JButton b = new JButton(ResourceMgr.getString("LabelFilterRemoveLine"));
		Border ib = BorderFactory.createEmptyBorder(0,2,0,2);
		Border ob = BorderFactory.createEtchedBorder();
		b.setBorder(BorderFactory.createCompoundBorder(ob,ib));
		b.addActionListener(this);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 0;
		c.weightx = 1.0;
		p.add(exp, c);
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = 0;
		c.weightx = 0;		
		c.insets = new Insets(0,5,0,0);
		p.add(b, c);
		
		PanelEntry item = new PanelEntry(p, exp);
		b.putClientProperty("panel", item);
		
		this.panelList.add(item);
		
		GridBagLayout l = (GridBagLayout)expressions.getLayout();
		for (int i=0; i < this.panelList.size(); i++)
		{
			PanelEntry entry = (PanelEntry)panelList.get(i);
			GridBagConstraints cons = l.getConstraints(entry.container);
			cons.weighty = 0;
			l.setConstraints(entry.container, cons);
		}
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 1.0;
		c.weightx = 1.0;
		this.expressions.add(p,c);
		
		this.validate();
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.addButton)
		{
			addExpressionPanel();
		}
		else if (e.getSource() instanceof JButton)
		{
			JButton button = (JButton)e.getSource();
			PanelEntry entry = (PanelEntry)button.getClientProperty("panel");
			this.panelList.remove(entry);
			this.expressions.remove(entry.container);
			GridBagLayout l = (GridBagLayout)expressions.getLayout();
			int count = this.panelList.size();
			for (int i=0; i < count; i++)
			{
				entry = (PanelEntry)panelList.get(i);
				GridBagConstraints cons = l.getConstraints(entry.container);
				if (i < count - 1)
					cons.weighty = 0;
				else
					cons.weighty = 1.0;
				l.setConstraints(entry.container, cons);
			}
			this.validate();
		}
	}
	
	public static void main(String args[])
	{
		try
		{
//			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			final JFrame f = new JFrame();
			f.getContentPane().setLayout(new BorderLayout());
			String[] names = new String[] {"firstname","lastname", "age"};
			String[] classes = new String[] {"java.lang.String", "java.lang.String", "java.lang.Integer"};
			int[] types = new int[] {Types.VARCHAR, Types.VARCHAR, Types.INTEGER};
			int[] sizes = new int[] {50, 50, 10};
			ResultInfo info = new ResultInfo(names, types, sizes, classes);
			final FilterPanel panel = new FilterPanel(info);
			panel.addExpressionPanel();
			panel.addExpressionPanel();
			f.getContentPane().add(panel, BorderLayout.CENTER);
			f.pack();
			f.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt)
				{
					FilterExpression filter = panel.getExpression();
					WbPersistence p = new WbPersistence("d:/temp/filter.xml");
					p.writeObject(filter);
					System.exit(0);
				}
			});
			f.show();
		}
		catch (Throwable th)
		{
			th.printStackTrace();
		}
		System.out.println("Done.");
	}	

}

class PanelEntry
{
	JPanel container;
	ColumnExpressionPanel expressionPanel;
	public PanelEntry(JPanel p, ColumnExpressionPanel ep)
	{
		container = p;
		expressionPanel = ep;
	}
}
