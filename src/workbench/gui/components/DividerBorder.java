package workbench.gui.components;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Component;
import javax.swing.border.AbstractBorder;

public class DividerBorder extends AbstractBorder
{
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int TOP = 2;
	public static final int BOTTOM = 3;
	
	protected int type;
	protected int thickness;
	
	/**
	 * Creates a line border with the specified color and a
	 * thickness = 1.
	 * @param color the color for the border
	 */
	public DividerBorder(int type)
	{
		this(type, 1);
	}
	
	/**
	 * Creates a line border with the specified color, thickness,
	 * and corner shape.
	 * @param color the color of the border
	 * @param thickness the thickness of the border
	 * @param roundedCorners whether or not border corners should be round
	 * @since 1.3
	 */
	public DividerBorder(int aType, int aThickness)
	{
		this.thickness = aThickness;
		this.type = aType;
	}
	
	/**
	 * Paints the border for the specified component with the
	 * specified position and size.
	 * @param c the component for which this border is being painted
	 * @param g the paint graphics
	 * @param x the x position of the painted border
	 * @param y the y position of the painted border
	 * @param width the width of the painted border
	 * @param height the height of the painted border
	 */
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
		}
		
		g.setColor(oldColor);
	}
	
	/**
	 * Returns the insets of the border.
	 * @param c the component for which this border insets value applies
	 */
	public Insets getBorderInsets(Component c)
	{
		return new Insets(2, 2, 2, 2);
	}
	
	/**
	 * Reinitialize the insets parameter with this Border's current Insets.
	 * @param c the component for which this border insets value applies
	 * @param insets the object to be reinitialized
	 */
	public Insets getBorderInsets(Component c, Insets insets)
	{
		insets.left = insets.top = insets.right = insets.bottom = 2;
		return insets;
	}
	

}

