/*
 * Scripter.java
 *
 * Created on 7. August 2004, 22:01
 */

package workbench.interfaces;

/**
 *
 * @author  thomas
 */
public interface Scripter
{
	public String getScript();
	public void setProgressMonitor(ScriptGenerationMonitor monitor);
}
