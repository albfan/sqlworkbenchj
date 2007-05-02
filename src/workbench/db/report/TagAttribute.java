/*
 * TagAttribute.java
 *
 * Created on Apr 26, 2007, 9:00:56 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package workbench.db.report;

/**
 *
 * @author tkellerer
 */
public class TagAttribute
{
	private final String tagText; 
	
	public TagAttribute(String name, String value)
	{
		StringBuilder b = new StringBuilder(name.length() + value.length() + 1);
		b.append(name);
		b.append("=\"");
		b.append(value);
		b.append('"');
		tagText = b.toString();
	}
	
	public String getTagText()
	{
		return tagText;
	}
	
}
