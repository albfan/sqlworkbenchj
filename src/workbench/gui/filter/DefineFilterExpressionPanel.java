/*
 * DefineFilterExpressionPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.filter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.ExtensionFileFilter;
import workbench.gui.components.FlatButton;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbFileChooser;
import workbench.gui.components.WbTable;
import workbench.gui.components.WbToolbar;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.storage.ResultInfo;
import workbench.storage.filter.AndExpression;
import workbench.storage.filter.ColumnComparator;
import workbench.storage.filter.ComplexExpression;
import workbench.storage.filter.ExpressionValue;
import workbench.storage.filter.FilterExpression;
import workbench.storage.filter.OrExpression;
import workbench.util.ExceptionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
 * A Panel to display a filter dialog for a {@link workbench.storage.DataStore}
 * @author Thomas Kellerer
 */
public class DefineFilterExpressionPanel
	extends JPanel
	implements ActionListener, ValidatingComponent
{
	private ResultInfo columnInfo;
	private List<PanelEntry> panels = new ArrayList<PanelEntry>();
	private JButton addLineButton;
	private JRadioButton andButton;
	private JRadioButton orButton;
	private JPanel expressions;
	private JScrollPane scroll;
	private	JButton saveButton = new JButton();
	private	JButton loadButton = new JButton();

	public DefineFilterExpressionPanel(ResultInfo source)
	{
		this(source,true);
	}

	public DefineFilterExpressionPanel(ResultInfo source, boolean allowSave)
	{
		super();
		columnInfo = source;
		expressions = new JPanel();
		this.expressions.setLayout(new GridBagLayout());

		this.setLayout(new BorderLayout(0,2));

		Insets ins = new Insets(0,0,0,0);
		orButton = new JRadioButton(ResourceMgr.getString("LblFilterOrOption"));
		orButton.setToolTipText(ResourceMgr.getDescription("LblFilterOrOption"));
		orButton.setMargin(ins);
		andButton = new JRadioButton(ResourceMgr.getString("LblFilterAndOption"));
		andButton.setToolTipText(ResourceMgr.getDescription("LblFilterAndOption"));
		andButton.setMargin(ins);
		ButtonGroup g = new ButtonGroup();
		g.add(orButton);
		g.add(andButton);
		andButton.setSelected(true);

		JPanel radioPanel = new JPanel();
		radioPanel.setLayout(new FlowLayout(FlowLayout.RIGHT,0,0));
		radioPanel.setBorder(BorderFactory.createEtchedBorder());
		radioPanel.add(andButton);
		radioPanel.add(orButton);

		JPanel p = new JPanel();
		p.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.weighty = 0;
		c.weightx = 0;

		if (allowSave)
		{
			WbToolbar bar = new WbToolbar();
			bar.setBorder(BorderFactory.createEtchedBorder());
			saveButton.setIcon(ResourceMgr.getImage(ResourceMgr.IMG_SAVE));
			saveButton.setMargin(ins);
			saveButton.setToolTipText(ResourceMgr.getDescription("SaveFilter"));

			loadButton.setIcon(ResourceMgr.getImage("Open"));
			loadButton.setMargin(new Insets(0,0,0,0));
			loadButton.setToolTipText(ResourceMgr.getDescription("LoadFilter"));

			loadButton.addActionListener(this);
			saveButton.addActionListener(this);
			bar.add(loadButton);
			bar.addSeparator();
			bar.add(saveButton);
			p.add(bar, c);
		}
		c.anchor = GridBagConstraints.EAST;
		c.gridx ++;
		c.weightx = 1;
		p.add(radioPanel, c);

		addLineButton = new FlatButton(ResourceMgr.getString("LblFilterAddLine"));
		Dimension d = radioPanel.getPreferredSize();
		int ph = (int)d.getHeight();
		addLineButton.setMinimumSize(new Dimension(25,ph));
		d = addLineButton.getPreferredSize();
		addLineButton.setPreferredSize(new Dimension((int)d.getWidth(),ph));
		addLineButton.addActionListener(this);
		c.gridx++;
		c.anchor = GridBagConstraints.NORTHEAST;
		c.weightx = 1;
		c.insets = new Insets(0,15,0,2);
		p.add(addLineButton);


		this.add(p, BorderLayout.NORTH);
		scroll = new JScrollPane(this.expressions);
		scroll.setBorder(BorderFactory.createEtchedBorder());
		this.add(scroll, BorderLayout.CENTER);
		d = addExpressionPanel();
		scroll.getVerticalScrollBar().setUnitIncrement((int)d.getHeight());
		scroll.getHorizontalScrollBar().setUnitIncrement(25);

		JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JLabel infoLabel = new JLabel(ResourceMgr.getString("MsgFilterHelp"));
		infoPanel.add(infoLabel);
		this.add(infoPanel, BorderLayout.SOUTH);

		double w = d.getWidth() + scroll.getHorizontalScrollBar().getPreferredSize().getWidth();
		double h = (d.getHeight() * 3) + andButton.getPreferredSize().getHeight() + scroll.getHorizontalScrollBar().getPreferredSize().getHeight() +
			infoLabel.getPreferredSize().getHeight();
		this.setPreferredSize(new Dimension((int)w,(int)h));
	}

	private void saveFilter()
	{
		FilterExpression filter = this.getExpression();
		if (filter == null)
		{
			WbSwingUtilities.showMessageKey(this, "ErrFilterNotPresent");
			return;
		}

		String lastDir = Settings.getInstance().getLastFilterDir();
		FileFilter ff = ExtensionFileFilter.getXmlFileFilter();
		JFileChooser fc = new WbFileChooser(lastDir);
		fc.addChoosableFileFilter(ff);
		int answer = fc.showSaveDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			String file = fc.getSelectedFile().getAbsolutePath();
			if (!file.toLowerCase().endsWith(".xml"))
			{
				file = file + ".xml";
			}
			String dir = fc.getCurrentDirectory().getAbsolutePath();
			Settings.getInstance().setLastFilterDir(dir);
			try
			{
				WbFile f = new WbFile(file);
				FilterDefinitionManager.getInstance().saveFilter(filter, f);
			}
			catch (IOException e)
			{
				String msg = ResourceMgr.getString("ErrLoadingFilter");
				msg = msg + "\n" + ExceptionUtil.getDisplay(e);
				WbSwingUtilities.showErrorMessage(this, msg);
			}
		}
	}

	private void loadFilter()
	{
		String lastDir = Settings.getInstance().getLastFilterDir();
		FileFilter ff = ExtensionFileFilter.getXmlFileFilter();
		JFileChooser fc = new WbFileChooser(lastDir);
		fc.addChoosableFileFilter(ff);
		int answer = fc.showOpenDialog(SwingUtilities.getWindowAncestor(this));
		if (answer == JFileChooser.APPROVE_OPTION)
		{
			String file = fc.getSelectedFile().getAbsolutePath();
			String dir = fc.getCurrentDirectory().getAbsolutePath();
			Settings.getInstance().setLastFilterDir(dir);
			try
			{
				FilterExpression f = FilterDefinitionManager.getInstance().loadFilter(file);
				if (f != null)
				{
					this.setFilter(f);
				}
			}
			catch (Exception e)
			{
				String msg = ResourceMgr.getString("ErrLoadingFilter");
				msg = msg + "\n" + ExceptionUtil.getDisplay(e);
				WbSwingUtilities.showErrorMessage(this, msg);
			}
		}
	}

	public void selectColumn(String col)
	{
		if (panels.isEmpty()) return;
		panels.get(0).expressionPanel.selectColumn(col);
	}

	private void removeAllPanels()
	{
		this.panels.clear();
		this.expressions.removeAll();
	}

	public void setFilter(FilterExpression filter)
	{
		if (filter == null) return;

		if (filter instanceof AndExpression)
		{
			this.andButton.setSelected(true);
		}
		else if (filter instanceof OrExpression)
		{
			this.orButton.setSelected(true);
		}
		else
		{
			return;
		}
		removeAllPanels();
		ComplexExpression cExp = (ComplexExpression) filter;
		List expList = cExp.getExpressions();
		int count = expList.size();
		for (int i=0; i < count; i++)
		{
			try
			{
				ExpressionValue exp = (ExpressionValue)expList.get(i);
				this.addExpressionPanel(exp);
				PanelEntry item = this.panels.get(this.panels.size() - 1);
				ColumnExpressionPanel panel = item.expressionPanel;
				panel.setExpressionValue(exp);
			}
			catch (ClassCastException e)
			{
				// ignore this as we cannot handle other expressions anyway...
			}
		}
		this.invalidate();
		this.validate();
		this.repaint();
	}

	@Override
	public boolean validateInput()
	{
		for (PanelEntry entry : panels)
		{
			ColumnComparator comp = entry.expressionPanel.getComparator();
			if (comp == null)
			{
				String msg = ResourceMgr.getString("ErrFilterNoComparator");
				WbSwingUtilities.showErrorMessage(this, msg);
				return false;
			}

			if (!entry.expressionPanel.validateInput())
			{
				String msg = ResourceMgr.getString("ErrFilterWrongValue");
				msg = StringUtil.replace(msg, "%value%", entry.expressionPanel.getInputValue());
				msg = StringUtil.replace(msg, "%op%", comp.getOperator());
				WbSwingUtilities.showErrorMessage(this, msg);
				return false;
			}
		}
		return true;
	}

	public FilterExpression getExpression()
	{
		ComplexExpression exp = null;

		for (PanelEntry entry : panels)
		{
			FilterExpression f = (FilterExpression)entry.expressionPanel.getExpressionValue();
			if (f != null)
			{
				if (exp == null)
				{
					if (andButton.isSelected())
						exp = new AndExpression();
					else
						exp = new OrExpression();
				}
				exp.addExpression(f);
			}
		}
		return exp;
	}

	private Dimension addExpressionPanel()
	{
		return addExpressionPanel(null);
	}

	private Dimension addExpressionPanel(ExpressionValue filter)
	{
		final ColumnExpressionPanel exp = new ColumnExpressionPanel(columnInfo, filter);
		JButton b = new FlatButton(ResourceMgr.getImage("Remove"));
		b.setPreferredSize(new Dimension(21,21));
		b.addActionListener(this);
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.gridx = 0;
		c1.gridy = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.WEST;
		c1.weighty = 0.0;
		c1.weightx = 1.0;
		p.add(exp, c1);

		c1.gridx++;
		c1.weightx = 0.0;
		c1.fill = GridBagConstraints.NONE;
		p.add(b, c1);

		PanelEntry item = new PanelEntry(p, exp);
		b.putClientProperty("panel", item);
		this.panels.add(item);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 1.0;
		c.weightx = 1.0;

		GridBagLayout l = (GridBagLayout)expressions.getLayout();
		for (int i=0; i < this.panels.size(); i++)
		{
			PanelEntry entry = panels.get(i);
			GridBagConstraints cons = l.getConstraints(entry.container);
			cons.weighty = 0;
			l.setConstraints(entry.container, cons);
		}
		this.expressions.add(p,c);
		this.invalidate();
		this.validate();
		this.repaint();
		Dimension ps = exp.getPreferredSize();
		Dimension bs = b.getPreferredSize();
		Dimension prefSize = new Dimension((int)(ps.getWidth() + bs.getWidth()), (int)ps.getHeight());
		return prefSize;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == saveButton)
		{
			saveFilter();
		}
		else if (e.getSource() == loadButton)
		{
			loadFilter();
		}
		else if (e.getSource() == addLineButton)
		{
			addExpressionPanel();
		}
		else if (e.getSource() instanceof JButton)
		{
			JButton button = (JButton)e.getSource();
			PanelEntry entry = (PanelEntry)button.getClientProperty("panel");
			//entry.expressionPanel.removeChangeListener(this);
			this.panels.remove(entry);
			this.expressions.remove(entry.container);
			GridBagLayout l = (GridBagLayout)expressions.getLayout();
			int count = this.panels.size();
			for (int i=0; i < count; i++)
			{
				entry = panels.get(i);
				GridBagConstraints cons = l.getConstraints(entry.container);
				if (i < count - 1)
					cons.weighty = 0;
				else
					cons.weighty = 1.0;
				l.setConstraints(entry.container, cons);
			}
			this.invalidate();
			this.validate();
			this.repaint();
		}
	}

	public static void showDialog(WbTable source)
	{
		DataStore ds = source.getDataStore();
		if (ds == null) return;
		ResultInfo info = ds.getResultInfo();
		if (info == null) return;

		DefineFilterExpressionPanel panel = new DefineFilterExpressionPanel(info);
		int col = source.getSelectedColumn();

		FilterExpression lastFilter = source.getLastFilter();
		if (lastFilter != null)
		{
			panel.setFilter(lastFilter);
		}
		else if (col > -1)
		{
			String colname = info.getColumnName(col);
			panel.selectColumn(colname);
		}
		String title = ResourceMgr.getString("MsgFilterWindowTitle");
		boolean showDialog = true;
		while (showDialog)
		{

			boolean result = ValidatingDialog.showConfirmDialog(SwingUtilities.getWindowAncestor(source), panel, title);
			if (result)
			{
				if (panel.validateInput())
				{
					FilterExpression filter = panel.getExpression();
					source.applyFilter(filter);
					showDialog = false;
				}
			}
			else
			{
				showDialog = false;
			}
		}
	}

	public void stateChanged(javax.swing.event.ChangeEvent changeEvent)
	{
		FilterExpression e = this.getExpression();
		this.saveButton.setEnabled(e != null);
	}

	@Override
	public void componentDisplayed()
	{
		if (this.panels.isEmpty()) return;
		PanelEntry entry = panels.get(0);
		if (entry == null || entry.expressionPanel == null) return;
		entry.expressionPanel.setFocusToColumn();
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

