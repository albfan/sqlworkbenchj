/*
 * FileMappedSequence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import workbench.interfaces.CharacterSequence;
import workbench.log.LogMgr;

/**
 * An implementatio of CharacterSequence that does not read the
 * entire file but only a part of it into memory
 * @author Thomas Kellerer
 */
public class FileMappedSequence
  implements CharacterSequence
{
  // the current size of the chunk read from the file
  // this will be adjusted dynamically according to the
  // calls to substring
  private int chunkSize;

  // The current chunk that has been read from the file
  private String chunk;

  // The decoder used to convert the bytes from the file
  // into a String object
  private CharsetDecoder decoder;

  // Stores the starting position of the current chunk in the file
  private int chunkByteStart = -1;

  // The character index in the complete sequence where the current chunk starts
  // (for single byte character sets this will be equal to chunkByteStart
  private int chunkCharStart;

  // The length of the underlying file
  private long fileSize;

  // The real length in characters
  private long charLength;

  private FileInputStream input;
  private FileChannel channel;
  private ByteBuffer readBuffer;
  private int increasedChunkSize = -1;

  public FileMappedSequence(File f, String characterSet)
    throws IOException
  {
    this(f, characterSet, 512 * 1024);
  }

  public FileMappedSequence(File f, String characterSet, int size)
    throws IOException
  {
    this.chunkSize = size;
    init(f, characterSet);
    // Read the first chunk in order to initialize the character counters properly
    readFirstChunk();
  }

  private void init(File f, String characterSet)
    throws IOException
  {
    this.fileSize = f.length();
    if (characterSet.toLowerCase().startsWith("utf"))
    {
      charLength = FileUtil.getCharacterLength(f, characterSet);
    }
    else
    {
      charLength = fileSize;
    }
    this.input = new FileInputStream(f);
    this.channel = input.getChannel();
    this.chunk = "";
    readBuffer = ByteBuffer.allocateDirect(chunkSize);
    Charset charset = Charset.forName(characterSet);
    this.decoder = charset.newDecoder();
  }

  @Override
  public int length()
  {
    return (int)this.charLength;
  }

  /**
   * this is only package visible in order to be callable from the unit test
   */
  final void readFirstChunk()
  {
    chunkByteStart = -1;
    readNextChunk();
  }

  /**
   * this is only package visible in order to be callable from the unit test
   */
  final void readNextChunk()
  {
    boolean success = false;
    int tries = 0;

    int oldChunkCharEnd = chunkCharStart + chunk.length();

    int newChunkSize = (increasedChunkSize <= 0 ? chunkSize : increasedChunkSize);
    increasedChunkSize = -1;
    int nextStart = (chunkByteStart == -1 ? 0 : chunkByteStart + chunkSize);

    while (!success)
    {
      try
      {
        _readChunk(nextStart, newChunkSize);
        success = true;
        chunkCharStart = oldChunkCharEnd;
        chunkSize = newChunkSize;
        chunkByteStart = nextStart;
      }
      catch (CharacterCodingException cc)
      {
        tries ++;
        if (tries > 3)
        {
          // then something serious is wrong!
          LogMgr.logError("FileMappedSequence.readNextChunk()", "Error reading file", cc);
          throw new IllegalStateException("Could not read next chunk");
        }
        // This can happen if the chunk ends in the middle
        // of an UTF8 sequence, and the decoder was missing a byte
        // I can't think of any other solution, than to simply increase
        // the window by 1 byte and try again
        newChunkSize ++;
      }
      catch (IOException io)
      {
        LogMgr.logError("FileMappedSequence.ensureWindow()", "Error reading file", io);
        throw new IllegalStateException("Could not read next chunk");
      }
    }
  }

  void readPreviousChunk()
  {
    if (chunkByteStart == 0) return;
    int newStart = chunkByteStart - chunkSize;
    int newSize = chunkSize;
    int oldCharStart = chunkCharStart;
    if (newStart < 0) newStart = 0;
    int tries = 0;
    boolean success = false;
    while (!success)
    {
      try
      {
        _readChunk(newStart, newSize);
        success = true;
        chunkSize = newSize;
        chunkByteStart = newStart;
        chunkCharStart = (newStart > 0 ? oldCharStart - chunk.length() : 0);
      }
      catch (IOException io)
      {
        tries ++;
        if (tries > 3)
        {
          // then something serious is wrong!
          LogMgr.logError("FileMappedSequence.readPreviousChunk()", "Error when reading chunk from: " + newStart + " with size: " + chunkSize, io);
          throw new IllegalStateException("Could not read previous chunk");
        }
        // This can happen if the chunk ends in the middle
        // of an UTF8 sequence, and the decoder was missing a byte
        // I can't think of any other solution, than to simply increase
        // the window by 1 byte and try again
        if (newStart > 0) newStart --;
        newSize ++;
      }
    }
  }

  private void _readChunk(int byteStart, int size)
    throws CharacterCodingException, IOException
  {
    // prepare for requests larger then the chunkSize
    if (size > readBuffer.capacity())
    {
      readBuffer = ByteBuffer.allocateDirect(size);
    }

    if (byteStart + size > this.fileSize)
    {
      chunkSize = (int)(this.fileSize - byteStart);
    }

    readBuffer.clear();
    readBuffer.limit(size);
    int read = this.channel.read(readBuffer, byteStart);
    if (read == -1) return;

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


  @Override
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

  public int getCurrentChunkLength()
  {
    return chunk.length();
  }

  @Override
  public char charAt(int index)
  {
    if (index > this.charLength || index < 0) throw new StringIndexOutOfBoundsException(index + " > " + charLength);

    int chunkLength = chunk.length();
    if (index >= chunkCharStart + chunkLength)
    {
      while (index >= chunkCharStart + chunkLength)	readNextChunk();
    }
    else if (index < chunkCharStart)
    {
      while (index < chunkCharStart) readPreviousChunk();
    }
    int indexInChunk = index - chunkCharStart;

    return this.chunk.charAt(indexInChunk);
  }

  @Override
  public String subSequence(int start, int end)
  {
    if (end > this.charLength) throw new StringIndexOutOfBoundsException(end + " > " + charLength);
    if (start < 0) throw new StringIndexOutOfBoundsException(end + " > " + charLength);

    // ensure the current chunk includes the start index
    charAt(start);

    int startInChunk = start - chunkCharStart;
    int sequenceLength = (end - start);
    int endInChunk = startInChunk + sequenceLength;

    if (endInChunk < chunk.length())
    {
      return chunk.substring(startInChunk, endInChunk);
    }
    // requested sequence extends into the next chunk(s)
    StringBuilder result = new StringBuilder(sequenceLength);
    result.append(chunk.substring(startInChunk));
    int nextStart = start + result.length();
    result.append(subSequence(nextStart, end));
    return result.toString();
  }
}
