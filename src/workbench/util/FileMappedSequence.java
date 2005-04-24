/*
 * FileMappedSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import workbench.interfaces.CharacterSequence;
import workbench.log.LogMgr;

/**
 * An implementatio of CharacterSequence that does not read the 
 * entire file into memory. Only a part of the file is read into 
 * memory.
 * @author info@sql-workbench.net
 */
public class FileMappedSequence
	implements CharacterSequence
{
	private int chunkSize = 32768;
	private String chunk;
	private CharsetDecoder decoder;
	
	// Stores the starting position of the current chunk in the file
	private int chunkStart;
	// Stores the end position of the current chunk in the file
	private int chunkEnd;
	
	private long fileSize;
	
	private FileInputStream input;
	private FileChannel channel;
	
	public FileMappedSequence(File f, String characterSet)
		throws IOException
	{
		if (characterSet == null) throw new NullPointerException("Empty encoding not allowed");
		this.fileSize = f.length();
		this.input = new FileInputStream(f);
		this.channel = input.getChannel();
		this.chunkStart = 0;
		this.chunkEnd = 0;
		this.chunk = "";
		Charset charset = Charset.forName(characterSet);
		this.decoder = charset.newDecoder();
	}

	public boolean available()
	{
		return (this.chunkEnd < fileSize);
	}
	
	private void ensureWindow(int start, int end)
	{
		if (this.chunkStart <= start && this.chunkEnd > end) return;

		this.chunkStart = start;
		if ((end - start) > this.chunkSize)
		{
			this.chunkSize = end - start;
		}
		this.chunkEnd = start + chunkSize;
		try
		{
			if (chunkStart + chunkSize > this.fileSize)
			{
				chunkSize = (int)(this.fileSize - chunkStart);
			}
			MappedByteBuffer bb = this.channel.map(FileChannel.MapMode.READ_ONLY, chunkStart, chunkSize);
			CharBuffer cb = decoder.decode(bb);
			this.chunk = cb.toString(); 
		}
		catch (Exception e)
		{
			LogMgr.logError("FileMappedSequence.ensureWindow", "Error reading chunk", e);
		}
	}
	
	public void done()
	{
		try
		{
			if (this.input != null) input.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public char charAt(int index)
	{
		this.ensureWindow(index, index + 1);
		int indexInChunk = index - chunkStart; 
		return this.chunk.charAt(indexInChunk);
	}

	public String substring(int start, int end)
	{
		this.ensureWindow(start, end);
		StringBuffer result = new StringBuffer(end - start);
		int startInChunk = start - chunkStart; 
		int endInChunk = end - chunkStart;
		result.append(this.chunk.substring(startInChunk, endInChunk));
		return result.toString();
	}
}
