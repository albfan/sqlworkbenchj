/*
 * VariablePrompter.java
 *
 * Created on August 21, 2004, 11:53 AM
 */

package workbench.gui.sql;

import java.util.Set;
import workbench.sql.SqlParameterPool;
import workbench.storage.DataStore;

/**
 *
 * @author  workbench@kellerer.org
 */
public class VariablePrompter
{
	private Set toPrompt = null;
	private	SqlParameterPool pool = SqlParameterPool.getInstance();
	private String sql;

	public VariablePrompter(String input)
	{
		this.sql = input;
	}
	
	public boolean needsInput()
	{
		if (this.toPrompt == null)
		{
			this.toPrompt = this.pool.getVariablesNeedingPrompt(this.sql);
		}
		return (this.toPrompt.size() > 0);
	}
	
	public boolean getPromptValues()
	{
		if (this.toPrompt == null)
		{
			this.toPrompt = this.pool.getVariablesNeedingPrompt(this.sql);
		}
		if (this.toPrompt.size() == 0) return true;
		
		DataStore vars = this.pool.getVariablesDataStore(this.toPrompt);
		
		return VariablesEditor.showVariablesDialog(vars);
	}
	
}
