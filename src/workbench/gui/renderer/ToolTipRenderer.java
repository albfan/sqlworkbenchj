package workbench.gui.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

import workbench.gui.components.WbTable;
import workbench.util.StringUtil;

/**
 * Displays a string in a table cell and shows a tool
 * tip if the string is too long to fit in the cell.
 */
public class ToolTipRenderer
	extends JComponent
	implements TableCellRenderer
{
	protected String[] displayResult = new String[] { StringUtil.EMPTY_STRING, null };

	private Color selectedForeground;
	private Color selectedBackground;
	private Color unselectedForeground;
	private Color unselectedBackground;

	private Rectangle paintIconR = new Rectangle();
	private Rectangle paintTextR = new Rectangle();
	private Rectangle paintViewR = new Rectangle();
	private Insets paintViewInsets = new Insets(0, 0, 0, 0);
	private Insets emptyInsets = new Insets(0, 0, 0, 0);
	
	public static final ToolTipRenderer DEFAULT_TEXT_RENDERER = new ToolTipRenderer();
	
	private static Insets focusedInsets;
	static
	{
		int thick = WbTable.FOCUSED_CELL_BORDER.getThickness();
		focusedInsets = new Insets(thick, thick, thick, thick);
	}

	private String displayText = StringUtil.EMPTY_STRING;
	private boolean selected;
	private boolean focus;
	private int valign = SwingConstants.TOP; 
	private int halign = SwingConstants.LEFT;
	
	public static final String[] EMPTY_DISPLAY = new String[] { StringUtil.EMPTY_STRING, null };
	
	public ToolTipRenderer()
	{
	}
	
	public void setVerticalAlignment(int align)
	{
		this.valign = align;
	}
	
	public void setHorizontalAlignment(int align)
	{
		this.halign = align;
	}

	public Component getTableCellRendererComponent(	JTable table,
																									Object value,
																									boolean isSelected,
																									boolean hasFocus,
																									int row,
																									int col)
	{
		this.focus = hasFocus;
		if (isSelected)
		{
			if (selectedForeground == null)
			{
				selectedForeground = table.getSelectionForeground();
				selectedBackground = table.getSelectionBackground();
			}
		}
		else
		{
			if (selectedForeground == null)
			{
				unselectedForeground = table.getForeground();
				unselectedBackground = table.getBackground();
			}
		}
		this.selected = isSelected;
		
		String[] displayValue = this.getDisplay(value);
		this.setToolTipText(displayValue[1]);
		this.displayText = displayValue[0];
		return this;
	}
	
	public void paint(Graphics g)
	{
		FontMetrics fm = g.getFontMetrics();

		Insets insets;
		
		if (focus)
		{
			insets = focusedInsets;
		}
		else
		{
			insets = emptyInsets;
		}
			
		int w = this.getWidth();
		int h = this.getHeight();
		paintViewR.x = insets.left;
		paintViewR.y = insets.top;
		paintViewR.width = w - (insets.left + insets.right);
		paintViewR.height = h - (insets.top + insets.bottom);
		
		
		paintIconR.x = paintIconR.y = paintIconR.width = paintIconR.height = 0;
		paintTextR.x = paintTextR.y = paintTextR.width = paintTextR.height = 0;
		
		Icon ic = null;
		
		String clippedText = 
        SwingUtilities.layoutCompoundLabel(this,fm,this.displayText,ic
						,this.valign
						,this.halign
						,SwingConstants.TOP
						,SwingConstants.RIGHT
						,paintViewR, paintIconR, paintTextR, 0);
		
		int textX = paintTextR.x;
		int textY = paintTextR.y + fm.getAscent();
		
		
		if (this.selected)
		{
			g.setColor(selectedBackground);
			g.fillRect(0,0, w, h);
			g.setColor(selectedForeground);
		}
		else 
		{
			g.setColor(unselectedBackground);
			g.fillRect(0,0, w, h);
			g.setColor(unselectedForeground);
		}
		g.drawString(clippedText, textX, textY);
		if (focus) 
		{
			WbTable.FOCUSED_CELL_BORDER.paintBorder(this, g, 0, 0, w, h);
		}
	}
	
  protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
  public boolean isOpaque() { return true; }
	
	public String[] getDisplay(Object aValue)
	{
		if (aValue == null)
		{
			return EMPTY_DISPLAY;
		}
		else
		{
			String display;
			String tooltip = null;
			
			display = aValue.toString();
			if (display.trim().length() == 0)
				tooltip = null;
			else
				tooltip = display;
			/* HTML parsing no longer need as we are using 
			 * MultiLineToolTip now which (I think) is 
			 * faster than building a new StringBuffer with 
			 * HTML code
			 */
			/*
			int len = display.length();
			if (len > 0 && len < 100)
			{
				Matcher m = StringUtil.PATTERN_CRLF.matcher(display);
				if (m.find())
				{
					StringBuffer tip = new StringBuffer(display.length() + 50);
					tip.append("<html>");
					tip.append(m.replaceAll("<br>"));
					tip.append("</html>");
					tooltip = tip.toString();
				}
				else
				{
					tooltip = display;
				}
			}
			*/
			displayResult[0] = display;
			displayResult[1] = tooltip;
		}
		return displayResult;
	}
	
}
