/*
 * FileMappedSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import workbench.interfaces.CharacterSequence;
import workbench.log.LogMgr;

/**
 * An implementatio of CharacterSequence that does not read the 
 * entire file but only a part of it into memory
 * @author support@sql-workbench.net
 */
public class FileMappedSequence
	implements CharacterSequence
{
	// the current size of the chunk read from the file
	// this will be adjusted dynamically according to the
	// calls to substring
	private int chunkSize = 128 * 1024;
	
	// The current chunk that has been read from the file
	// its length will be equal to chunkSize 
	private String chunk;
	
	// The decoder used to convert the bytes from the file
	// into a String object
	private CharsetDecoder decoder;
	
	// Stores the starting position of the current chunk in the file
	private int chunkStart;
	
	// Stores the end position of the current chunk in the file
	private int chunkEnd;
	
	private long fileSize;
	
	private FileInputStream input;
	private FileChannel channel;
	private ByteBuffer readBuffer;
	
	public FileMappedSequence(File f, String characterSet)
		throws IOException
	{
		this.fileSize = f.length();
		this.input = new FileInputStream(f);
		this.channel = input.getChannel();
		this.chunkStart = 0;
		this.chunkEnd = 0;
		this.chunk = "";
		readBuffer = ByteBuffer.allocateDirect(chunkSize);
		Charset charset = Charset.forName(characterSet);
		this.decoder = charset.newDecoder();
	}

	public int length()
	{
		return (int)this.fileSize;
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
			
			// prepare for requests larger then the chunkSize
			if (chunkSize > readBuffer.capacity())
			{
				readBuffer = ByteBuffer.allocateDirect(chunkSize);
			}
			readBuffer.clear();
			readBuffer.limit(chunkSize);
			int read = this.channel.read(readBuffer, chunkStart);
			
			// Rewind is necessary because the decoder starts at the 
			// current position
			readBuffer.rewind();
			
			// Setting the limit to the number of bytes read
			// is also necessary because the decoder uses that
			// information to find out how many bytes it needs
			// to decode from the buffer
			readBuffer.limit(read);
			
			CharBuffer cb = decoder.decode(readBuffer);
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
			LogMgr.logError("FileMappedSequence.done()", "Error closing input stream", e);
		}
	}

	public char charAt(int index)
	{
		this.ensureWindow(index, index + 1);
		int indexInChunk = index - chunkStart; 
		return this.chunk.charAt(indexInChunk);
	}

	public String subSequence(int start, int end)
	{
		this.ensureWindow(start, end);
		int startInChunk = start - chunkStart; 
		int endInChunk = end - chunkStart;
		return this.chunk.substring(startInChunk, endInChunk);
	}
}
