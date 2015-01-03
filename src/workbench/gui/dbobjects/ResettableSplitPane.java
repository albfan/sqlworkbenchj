/*
 * ResettableSplitPane.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dbobjects;

import java.util.HashSet;
import java.util.Set;
import workbench.gui.components.WbSplitPane;
import workbench.interfaces.Resettable;

/**
 * @author Thomas Kellerer
 */
public class ResettableSplitPane
	extends WbSplitPane
	implements Resettable
{
	private Set<Resettable> clients = new HashSet<>();
	
	public ResettableSplitPane(int type)
	{
		super(type);
	}
	
	public void addClient(Resettable r)
	{
		clients.add(r);
	}
	
	public void reset()
	{
		for (Resettable r : clients)
		{
			r.reset();
		}
	}
	
	
}
