/*
 * BlobHandler.java
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
package workbench.gui.components;

import java.awt.Dialog;
import java.awt.Frame;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.SQLException;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.dialogs.BlobInfoDialog;
import workbench.gui.dialogs.ImageViewer;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobHandler
{
	private File uploadFile;
	private byte[] newValue;

	private boolean setToNull = false;

	public StringBuilder getByteDisplay(Object value)
	{
		long l = getBlobSize(value);
		return getByteDisplay(l);
	}

	public boolean setToNull()
	{
		return this.setToNull;
	}

	public Object getValueToUse()
	{
		if (this.setToNull) return null;
		if (this.uploadFile != null) return uploadFile;
		return newValue;
	}

	public File getUploadFile()
	{
		return uploadFile;
	}

	public StringBuilder getByteDisplay(long l)
	{
		StringBuilder result = new StringBuilder(32);

		if (l < 1024)
		{
			result.append(Long.toString(l));
			result.append(' ');
		}
		else if (l < 1024*1024)
		{
			result.append(Long.toString(l/1024));
			result.append(" K");
		}
		else
		{
			result.append(Long.toString(l/(1024*1024)));
			result.append(" M");
		}
		result.append('B');
		return result;
	}

	public byte[] getBlobAsArray(Object value)
	{
		if (value == null)
		{
			return null;
		}

		if (value instanceof Blob)
		{
			Blob blob = (Blob)value;
			try
			{
				byte[] buffer = blob.getBytes(1, (int)blob.length());
				return buffer;
			}
			catch (Exception e)
			{
				LogMgr.logError("BlobHandler.getBlobAsArray()", "Error retrieving blob value", e);
				return null;
			}
		}
		else if (value instanceof byte[])
		{
			return (byte[])value;
		}
		else if (value instanceof File)
		{
			InputStream in = null;
			try
			{
				File f = (File)value;
				in = new BufferedInputStream(new FileInputStream(f));
				byte[] buff = FileUtil.readBytes(in);
				return buff;
			}
			catch (Exception e)
			{
				LogMgr.logError("BlobHandler.getBlobAsArray()", "Error retrieving blob value", e);
				return null;
			}
			finally
			{
				FileUtil.closeQuietely(in);
			}

		}
		return null;
	}

	public long getBlobSize(Object value)
	{
		if (value == null) return 0;
		if (value instanceof Blob)
		{
			Blob blob = (Blob)value;
			try
			{
				return blob.length();
			}
			catch (Exception e)
			{
				LogMgr.logError("BlobHandler.getBlobSize()", "Could not retrieve blob size",e);
			}
		}
		else if (value instanceof File)
		{
			// can happen if a file has been uploaded through the BlobInfoDialog
			return ((File)value).length();
		}
		else if (value instanceof byte[])
		{
			byte[] b = (byte[])value;
			return b.length;
		}
		return -1;
	}


	private String convertArray(byte[] value, String encoding)
	{
		String data = null;
		try
		{
			data = new String( value,encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			LogMgr.logError("BlobHandler.convertArray()", "Could not convert binary to string using encoding: " + encoding, e);
			data = new String(value);
		}
		return data;
	}

	public String getBlobAsString(Object value, String encoding)
	{
		if (value == null) return null;
		if (getBlobSize(value) == 0) return StringUtil.EMPTY_STRING;

		if (encoding == null) encoding = Settings.getInstance().getDefaultBlobTextEncoding();

		if (value instanceof Blob)
		{
			Blob blob = (Blob)value;
			try
			{
				byte[] buffer = blob.getBytes(1, (int)blob.length());
				return convertArray(buffer, encoding);
			}
			catch (Exception e)
			{
				LogMgr.logError("BlobHandler.getBlobAsString()", "Error retrieving blob value", e);
				return "";
			}
		}
		else if (value instanceof byte[])
		{
			return convertArray((byte[])value, encoding);
		}
		else if (value instanceof File)
		{
			Reader in = null;
			try
			{
				File f = (File)value;
				in = EncodingUtil.createReader(f, encoding);
				String s = FileUtil.readCharacters(in);
				return s;
			}
			catch (Exception e)
			{
				LogMgr.logError("BlobHandler.getBlobAsString()", "Error retrieving blob value", e);
				return "";
			}
		}
		return value.toString();
	}

	public static long saveBlobToFile(Object data, String file)
		throws IOException, SQLException
	{
		OutputStream out = new FileOutputStream(file);
		return saveBlobToFile(data, out);
	}

	public static long saveBlobToFile(Object data, OutputStream out)
		throws IOException, SQLException
	{
		InputStream in = null;
		if (data instanceof java.sql.Blob)
		{
			java.sql.Blob blob = (java.sql.Blob)data;
			in = blob.getBinaryStream();
		}
		else if (data instanceof byte[])
		{
			in = new ByteArrayInputStream((byte[])data);
		}
		else if (data instanceof File)
		{
			in = new FileInputStream((File)data);
		}
		else if (data instanceof InputStream)
		{
			in = (InputStream)data;
		}

		if (in == null)
		{
			LogMgr.logError("WbTable.saveBlobContent", "No valid BLOB data found, got " + data.getClass().getName() + " instead", null);
			throw new IOException("No LOB data found");
		}
		return FileUtil.copy(in, out);
	}

	public void showBlobAsImage(Object value)
	{
		showBlobAsImage(null, value);
	}

	public void showBlobAsImage(Dialog parent, Object value)
	{
		ImageViewer v;
		if (parent != null)
		{
			v = new ImageViewer(parent, ResourceMgr.getString("TxtBlobData"));
		}
		else
		{
			v = new ImageViewer(WbManager.getInstance().getCurrentWindow(), ResourceMgr.getString("TxtBlobData"));
		}

		v.setData(value);
		v.setVisible(true);
	}

	public void showBlobAsText(Object value)
	{
		showBlobAsText(null, value, Settings.getInstance().getDefaultBlobTextEncoding());
	}

	/**
	 * Display the blob content as a text with the specified encoding.
	 * The window will allow editing of the data, if the user changed the
	 * data and closed the window using the OK button, this method will
	 * return true. In this case the blob value will be updated with
	 * the binary representation of the text using the specified encoding
	 */
	public boolean showBlobAsText(Dialog parent, Object value, final String encoding)
	{
		String data = getBlobAsString(value, encoding);
		String title = ResourceMgr.getString("TxtBlobData");
		this.newValue = null;
		final EditWindow w;
		boolean result = false;

		if (parent != null)
		{
			w = new EditWindow(parent, title, data, false, false);
		}
		else
		{
			w = new EditWindow(WbManager.getInstance().getCurrentWindow(), title, data, false, false);
		}

		WbSwingUtilities.invoke(new Runnable()
		{
			@Override
			public void run()
			{
				w.setInfoText(ResourceMgr.getString("LblFileEncoding") + ": " + encoding);
				w.setVisible(true);
			}
		});

		if (!w.isCancelled())
		{
			data = w.getText();
			try
			{
				if (encoding != null)
				{
					this.newValue = data.getBytes(encoding);
					this.uploadFile = null;
					result = true;
				}
			}
			catch (Exception e)
			{
				this.newValue = null;
				result = false;
				LogMgr.logError("BlobHandler.showBlobAsText", "Error converting text to blob", e);
			}
		}

		return result;
	}

	public boolean isChanged()
	{
		return this.newValue != null;
	}

	/**
	 * Returns thenew value after the user changed the data in the text window.
	 * Returns null if the user did not change the data.
	 * @see #showBlobAsText(Dialog, Object, String)
	 */
	public byte[] getNewValue()
	{
		return this.newValue;
	}

	public void showBlobInfoDialog(Frame parent, Object blobValue, boolean readOnly)
	{
		BlobInfoDialog d = new BlobInfoDialog(parent, true, readOnly);
		d.setBlobValue(blobValue);
		d.setVisible(true);
		this.uploadFile = d.getUploadedFile();
		this.setToNull = d.setToNull();
		this.newValue = d.getNewValue();
	}
}
