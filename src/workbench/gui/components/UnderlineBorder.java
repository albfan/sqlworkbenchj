package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.JTextField;
import javax.swing.border.AbstractBorder;

public class UnderlineBorder extends AbstractBorder
{
	protected JTextField label;
	private Insets insets = new Insets(0, 0, 0, 0);
	public UnderlineBorder(JTextField aLabel)
	{
		this.label = aLabel;
	}
	
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		
    Color fg = this.label.getForeground();
		g.setColor(fg);
		Font f = this.label.getFont();
		FontMetrics fm = this.label.getFontMetrics(f);
		int size = fm.stringWidth(this.label.getText());
		g.drawLine(x, y + height - 1, x + size, y + height -1);
		g.setColor(oldColor);
	}
	
	public Insets getBorderInsets(Component c)
	{
		return insets;
	}
	
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = 0;
		return insets;
	}
	

}

