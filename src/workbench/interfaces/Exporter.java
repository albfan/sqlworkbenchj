/*
 * Exporter.java
 *
 * Created on 6. Juli 2002, 21:37
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface Exporter
{
	void saveAs();
	void copyDataToClipboard();
	void copyDataToClipboard(boolean includeHeaders);
}
