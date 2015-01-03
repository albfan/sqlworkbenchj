/*
 * DefaultOutputFactory.java
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
package workbench.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

/**
 *
 * @author Thomas Kellerer
 */
public class DefaultOutputFactory
	implements OutputFactory
{

	@Override
	public boolean isArchive()
	{
		return false;
	}

	@Override
	public OutputStream createOutputStream(File output)
		throws IOException
	{
		return new FileOutputStream(output);
	}

	@Override
	public Writer createWriter(File output, String encoding)
		throws IOException
	{
		OutputStream out = createOutputStream(output);
		return EncodingUtil.createWriter(out, encoding);
	}

	@Override
	public Writer createWriter(String filename, String encoding)
		throws IOException
	{
		return createWriter(new File(filename), encoding);
	}

	@Override
	public void done()
		throws IOException
	{
	}
}
