/*
 * MacroEntry.java
 *
 * Created on July 6, 2003, 12:54 AM
 */

package workbench.gui.macros;

public class MacroEntry
{
	private String text;
	private String name;
	
	public MacroEntry(String aName, String aText)
	{
		this.text = aText;
		this.name = aName;
	}
	
	public String toString() { return this.name; }
	public String getName() { return this.name; }
	public void setName(String aName) { this.name = aName; }
	public String getText() { return this.text; }
	public void setText(String aText) { this.text = aText; }
}