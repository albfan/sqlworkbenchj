/*
 * SummaryBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import workbench.gui.sql.DwStatusBar;

/**
 *
 * @author Thomas Kellerer
 */
public class SummaryBar
	extends JPanel
{
	private static final Border DEFAULT_BORDER = new CompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), BorderFactory.createEtchedBorder());

	public SummaryBar(JLabel infoLabel)
	{
		super(new BorderLayout());
		Dimension d = new Dimension(80, DwStatusBar.BAR_HEIGHT + 1);
		setMinimumSize(d);
		setPreferredSize(d);
		setBorder(DEFAULT_BORDER);
		infoLabel.setBorder(new EmptyBorder(2, 1, 1, 0));
		add(infoLabel, BorderLayout.CENTER);
	}

}
