package workbench.gui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.border.AbstractBorder;

public class DividerBorder extends AbstractBorder
{
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int TOP = 2;
	public static final int BOTTOM = 3;
	public static final int MIDDLE = 4;
	public static final int LEFT_RIGHT = 5;
	
	protected int type;
	protected int thickness;
	
	public DividerBorder(int type)
	{
		this(type, 1);
	}
	
	/**
	 * Creates a divider border with the specified type and thickness
	 * @param type (LEFT, RIGHT, TOP, BOTTOM)
	 * @param thickness the thickness of the border
	 * @param roundedCorners whether or not border corners should be round
	 */
	public DividerBorder(int aType, int aThickness)
	{
		this.thickness = aThickness;
		this.type = aType;
	}
	
	public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
	{
		Color oldColor = g.getColor();
		
    Color bg = c.getBackground();
    Color light = bg.brighter();
    Color shade = bg.darker();
		g.setColor(shade);
		switch (this.type)
		{
			case TOP:
				g.drawLine(x, y, x + width, y);
				g.setColor(light);
				g.drawLine(x, y + 1, x  + width, y + 1);
				break;
			case BOTTOM:
				g.drawLine(x, y + height - 2, x + width, y + height - 2);
				g.setColor(light);
				g.drawLine(x, y + height - 1, x  + width, y + height - 1);
				break;
			case RIGHT:
				g.drawLine(x + width - 2, y, x + width - 2, y + height);
				g.setColor(light);
				g.drawLine(x + width - 1, y, x + width - 1, y + height);
				break;
			case LEFT:
				g.drawLine(x, y, x, y + height);
				g.setColor(light);
				g.drawLine(x + 1, y, x + 1, y + height);
				break;
			case LEFT_RIGHT:
				g.drawLine(x + width - 2, y, x + width - 2, y + height);
				g.setColor(light);
				g.drawLine(x + width - 1, y, x + width - 1, y + height);
				g.setColor(shade);
				g.drawLine(x, y, x, y + height);
				g.setColor(light);
				g.drawLine(x + 1, y, x + 1, y + height);
				break;
			case MIDDLE:
				int w2 = (int)width / 2;
				g.drawLine(x + w2, y, x + w2, y + height);
				g.setColor(light);
				g.drawLine(x + w2 + 1, y, x + w2 + 1, y + height);
				break;
				
		}
		
		g.setColor(oldColor);
	}
	
	public Insets getBorderInsets(Component c)
	{
		return new Insets(2, 2, 2, 2);
	}
	
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = 2;
		return insets;
	}
	

}

