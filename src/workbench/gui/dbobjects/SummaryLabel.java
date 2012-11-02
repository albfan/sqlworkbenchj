/*
 * SummaryBar.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Thomas Kellerer
 */
public class SummaryLabel
	extends JLabel
{
	private static final Border DEFAULT_BORDER = new CompoundBorder(new EmptyBorder(2, 0, 0, 0), new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(1, 1, 1, 0)));
	public SummaryLabel(String text)
	{
		super(text);
		Font f = getFont();
		FontMetrics fm = null;
		if (f != null) fm = getFontMetrics(f);
		int height = fm == null ? 0 : fm.getHeight() + 8;
		height = Math.min(22, height);
		Dimension d = new Dimension(80, height);
		setMinimumSize(d);
		setPreferredSize(d);
		setBorder(DEFAULT_BORDER);
	}

}
