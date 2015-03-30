/*
 * FileMappedSequenceTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
import java.io.Writer;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class FileMappedSequenceTest
	extends WbTestCase
{
  private TestUtil util;

	public FileMappedSequenceTest()
	{
		super("FileMappedSequenceTest");
		util = getTestUtil();
	}

	@Test
  public void testLastChar()
    throws Exception
  {
		File f = new File(util.getBaseDir(), "maxtest.txt");
		String content = "this is a test";

		Writer w = EncodingUtil.createWriter(f, "UTF-8", false);
		w.write(content);
		FileUtil.closeQuietely(w);
		FileMappedSequence sequence = new FileMappedSequence(f, "UTF-8", 57);
		int charLength = sequence.length();
    assertEquals(content.length(), charLength);
    assertEquals(content, sequence.subSequence(0, charLength));
		sequence.done();
		assertTrue(f.delete());
  }

	@Test
	public void testLength()
		throws Exception
	{
		File f = new File(util.getBaseDir(), "test.txt");
		String content =
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00c3\u00b6\u00c3\u00a4\u00c3\u00bc\u00c3\u2013\u00c3\u201e\u00c3\u0153" +
		"abcdefghijkklmnopqrstuvwxyz" +
		"\u00F6\u00E4\u00FC\u00D6\u00C4\u00DC\u00DF";

		Writer w = EncodingUtil.createWriter(f, "UTF-8", false);
		w.write(content);
		FileUtil.closeQuietely(w);

		int contentLength = content.length();

		FileMappedSequence sequence = new FileMappedSequence(f, "UTF-8", 57);
		int charLength = sequence.length();
		sequence.readFirstChunk();
		sequence.readNextChunk();
		sequence.readPreviousChunk();

		for (int i=0; i < charLength; i++)
		{
			char c = sequence.charAt(i);
			assertEquals(content.charAt(i), c);
		}

		for (int i=charLength - 1; i >= 0; i--)
		{
			char c = sequence.charAt(i);
			assertEquals("Wrong character at index: " + i, content.charAt(i), c);
		}

		sequence.readPreviousChunk();
		String value = sequence.subSequence(1,40);
		String expected = content.substring(1, 40);
		assertEquals(expected, value);
		sequence.readPreviousChunk();

		int len = sequence.getCurrentChunkLength();

		value = sequence.subSequence(0, len);
		expected = content.subSequence(0, len).toString();
		assertEquals(expected, value);
		sequence.done();

		for (int chunkSize =  contentLength / 3; chunkSize < contentLength * 2; chunkSize += contentLength / 4)
		{
			sequence = new FileMappedSequence(f, "UTF-8", chunkSize);

			len = sequence.getCurrentChunkLength();

			charLength = sequence.length();
			assertEquals(content.length(), charLength);
			for (int i=0; i < charLength; i++)
			{
				char c = sequence.charAt(i);
				assertEquals(content.charAt(i), c);
			}

			value = sequence.subSequence(len - 2, len - 1);
			expected = content.subSequence(len - 2, len - 1).toString();
			assertEquals(expected, value);

			value = sequence.subSequence(0, 30);
			expected = content.subSequence(0, 30).toString();
			assertEquals(expected, value);

			if (len < contentLength - 2)
			{
				value = sequence.subSequence(0, len + 1);
				expected = content.subSequence(0, len + 1).toString();
				assertEquals(expected, value);
			}

			value = sequence.subSequence(0, contentLength - 1);
			expected = content.subSequence(0, contentLength - 1).toString();
			assertEquals(expected, value);

			if (len < contentLength - 3)
			{
				value = sequence.subSequence(len, len +  2);
				expected = content.subSequence(len, len + 2).toString();
				assertEquals(expected, value);

				value = sequence.subSequence(len - 2, len +  2);
				expected = content.subSequence(len - 2, len + 2).toString();
				assertEquals(expected, value);
			}
			sequence.done();
		}
		assertTrue(f.delete());
	}

}
