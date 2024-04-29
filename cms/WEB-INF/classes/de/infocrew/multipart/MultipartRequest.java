package de.infocrew.multipart;

import java.util.Hashtable;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.io.File;

/**
	A Multipart form data parser.  Parses an input stream and writes out any files found, 
	making available a hashtable of other url parameters.
	
	<BR>
	<BR>
	Copyright (C)2001 Jason Pell.
	<BR>

	<PRE>
	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.
	<BR>
	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.
	<BR>
	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	<BR>	
	Email: 	jasonpell@hotmail.com
	Url:	http://www.geocities.com/jasonpell
	</PRE>

	@author Jason Pell

	@version 1.15	A new parameter added, intMaxReadBytes, to allow arbitrary length files.  Released under
					the LGPL (Lesser General Public License).  	03/02/2001
	@version 1.14	Fix for IE problem with filename being empty.  This is because IE includes a default Content-Type
					even when no file is uploaded.  16/02/2001
	@version 1.13	If an upload directory is not specified, then all file contents are sent into oblivion, but the
					rest of the parsing works as normal.
	@version 1.12	Fix, was allowing zero length files.  Will not even create the output file until there is
					something to write.  getFile(String) now returns null, if a zero length file was specified.  06/11/2000
	@version 1.11	Fix, in case Content-type is not specified.
	@version 1.1	Removed dependence on Servlets.  Now passes in a generic InputStream instead.
					"Borrowed" readLine from Tomcat 3.1 ServletInputStream class,
    				so we can remove some of the dependencies on ServletInputStream.
					Fixed bug where a empty INPUT TYPE="FILE" value, would cause an exception.
	@version 1.0	Initial Release.
*/

public class MultipartRequest
{
	// If not null, send debugging out here.
	private PrintWriter debug = null;

	private Hashtable htParameters = null;
	private Hashtable htFiles = null;
	
	private String strBoundary = null;
	
 	// If this Directory spec remains null, writing of files will be disabled...
	private File fileOutPutDirectory = null;

	private long intContentLength = -1;
	private long intTotalRead = -1;

	/**
		Prevent a denial of service by defining this, will never read more data, <i>unless ignoreMaxReadBytes
		set to true in constructor</i>.  If Content-Length is specified to be more than this, will throw an exception.
	*/
	public static final int MAX_READ_BYTES = 2 * (1024 * 1024); // 2MB!

	/**
		Defines the number of bytes to read per readLine call. 128K
	*/
	public static final int READ_LINE_BLOCK = 1024 * 128;

	/**
		Store a read from the input stream here.  Global so we do not keep creating new arrays each read.
	*/
	private byte[] blockOfBytes = null;

	/**
		Define the array indexes for the htFiles Object array.
	*/
	private static final int FILENAME = 0;
	private static final int CONTENT_TYPE = 1;
	private static final int SIZE = 2;

	/** 
	 * Constructor.
	 *
	 * @param strContentTypeText 	The &quot;Content-Type&quot; HTTP header value.
	 * @param intContentLength 		The &quot;Content-Length&quot; HTTP header value.
	 * @param in					The InputStream to read and parse.
	 * @param strSaveDirectory		The temporary directory to save the file from where they can then be moved to wherever by the
	 * 								calling process.  <b>If you specify <u>null</u> for this parameter, then any files uploaded
	 *								will be silently ignored.</b>
	 *
	 * @exception IllegalArgumentException 	If the strContentTypeText does not contain a Content-Type of "multipart/form-data" or the boundary is not found.
	 * @exception IOException				If the intContentLength is higher than MAX_READ_BYTES or strSaveDirectory is invalid or cannot be written to.
	 *
	 * @see #MAX_READ_BYTES
	 */
    public MultipartRequest(String strContentTypeText, int intContentLength, InputStream in, String strSaveDirectory) throws IllegalArgumentException, IOException
	{
		// In default instantiation, do not ignore.
		this(null, strContentTypeText, intContentLength, in, strSaveDirectory, MAX_READ_BYTES);
	}
	
	/** 
	 * Constructor.
	 *
	 * @param strContentTypeText 	The &quot;Content-Type&quot; HTTP header value.
	 * @param intContentLength 		The &quot;Content-Length&quot; HTTP header value.
	 * @param in					The InputStream to read and parse.
	 * @param strSaveDirectory		The temporary directory to save the file from where they can then be moved to wherever by the
	 * 								calling process.  <b>If you specify <u>null</u> for this parameter, then any files uploaded
	 *								will be silently ignored.</B>
	 * @param intMaxReadBytes		Overrides the MAX_BYTES_READ value, to allow arbitrarily long files.
	 *
	 * @exception IllegalArgumentException 	If the strContentTypeText does not contain a Content-Type of "multipart/form-data" or the boundary is not found.
	 * @exception IOException				If the intContentLength is higher than MAX_READ_BYTES or strSaveDirectory is invalid or cannot be written to.
	 *
	 * @see #MAX_READ_BYTES
	 */
	public MultipartRequest(String strContentTypeText, int intContentLength, InputStream in, String strSaveDirectory, int intMaxReadBytes) throws IllegalArgumentException, IOException
	{
		this(null, strContentTypeText, intContentLength, in, strSaveDirectory, intMaxReadBytes);
	}

	/** 
	 * Constructor.
	 *
	 * @param debug					A PrintWriter that can be used for debugging.
	 * @param strContentTypeText 	The &quot;Content-Type&quot; HTTP header value.
	 * @param intContentLength 		The &quot;Content-Length&quot; HTTP header value.
	 * @param in					The InputStream to read and parse.
	 * @param strSaveDirectory		The temporary directory to save the file from where they can then be moved to wherever by the
	 * 								calling process.  <b>If you specify <u>null</u> for this parameter, then any files uploaded
	 *								will be silently ignored.</B>
	 *
	 * @exception IllegalArgumentException 	If the strContentTypeText does not contain a Content-Type of "multipart/form-data" or the boundary is not found.
	 * @exception IOException				If the intContentLength is higher than MAX_READ_BYTES or strSaveDirectory is invalid or cannot be written to.
	 *
	 * @see #MAX_READ_BYTES
	 */
	public MultipartRequest(PrintWriter debug, String strContentTypeText, int intContentLength, InputStream in, String strSaveDirectory) throws IllegalArgumentException, IOException
	{
		this(debug, strContentTypeText, intContentLength, in, strSaveDirectory, MAX_READ_BYTES);	
	}

	/** 
	 * Constructor.
	 *
	 * @param debug					A PrintWriter that can be used for debugging.
	 * @param strContentTypeText 	The &quot;Content-Type&quot; HTTP header value.
	 * @param intContentLength 		The &quot;Content-Length&quot; HTTP header value.
	 * @param in					The InputStream to read and parse.
	 * @param strSaveDirectory		The temporary directory to save the file from where they can then be moved to wherever by the
	 * 								calling process.  <b>If you specify <u>null</u> for this parameter, then any files uploaded
	 *								will be silently ignored.</B>
	 * @param intMaxReadBytes		Overrides the MAX_BYTES_READ value, to allow arbitrarily long files.
	 *
	 * @exception IllegalArgumentException 	If the strContentTypeText does not contain a Content-Type of "multipart/form-data" or the boundary is not found.
	 * @exception IOException				If the intContentLength is higher than MAX_READ_BYTES or strSaveDirectory is invalid or cannot be written to.
	 *
	 * @see #MAX_READ_BYTES
	 */
	public MultipartRequest(PrintWriter debug, String strContentTypeText, int intContentLength, InputStream in, String strSaveDirectory, int intMaxReadBytes) throws IllegalArgumentException, IOException
	{
		this.debug = debug;

		if (strContentTypeText!=null && strContentTypeText.startsWith("multipart/form-data") && strContentTypeText.indexOf("boundary=")!=-1)
			strBoundary = strContentTypeText.substring(strContentTypeText.indexOf("boundary=")+"boundary=".length()).trim();
		else
			throw new IllegalArgumentException("Invalid Content Type.");

		this.intContentLength = intContentLength;
		// FIX: 115
		if (intContentLength > intMaxReadBytes)
			throw new IOException("Content Length Error (" + intContentLength + " > " + intMaxReadBytes + ")");

		// Instantiate the hashtable...
	    htParameters = new Hashtable();
		htFiles = new Hashtable();

		blockOfBytes = new byte[READ_LINE_BLOCK];

		// IF strSaveDirectory == NULL, then we should ignore any files uploaded.
		if (strSaveDirectory!=null)
		{
			fileOutPutDirectory = new File(strSaveDirectory);
			if (!fileOutPutDirectory.exists() && !fileOutPutDirectory.canWrite())
				throw new IOException("Directory is invalid or cannot be written to.");
		}

		// Now parse the data.
		parse(new BufferedInputStream(in));

		// No need for this once parse is complete.
		this.blockOfBytes=null;
		this.debug = null;
		this.strBoundary=null;
	}

	/**
    	If an error occurs will return null.  
    */
	public String getURLParameter (String strParameter)
	{
    	return (String) htParameters.get(strParameter);
	}

	/**
		An enumeration of all URL Parameters for the current HTTP Request.
	*/
	public Enumeration getParameterNames()
	{
		return htParameters.keys();
	}

	public Enumeration getFileParameterNames()
	{
		return htFiles.keys();
	}

	/**
		Returns the Content-Type of a file.
	*/
	public String getContentType(String strName)
	{
		Object[] objArray = (Object[])htFiles.get(strName);
		if (objArray!=null && objArray[CONTENT_TYPE]!=null)
			return (String) objArray[CONTENT_TYPE];
		else
			return null;
	}
    
	/**
		Because the strSaveDirectory you specify is really only the temporary location to stream the files
		to, we want a way to get at the files contents to put it in the correct location.  Once you have a 
		File reference, you can copy/move the file elsewhere.  <i>Return a null file reference if a call to
		getFileSize(strName) returns zero.</i>

		@see #getFileSize(java.lang.String)
	*/
	public File getFile(String strName)
	{
		String filename = getFileSystemName(strName);
		// Fix: If fileOutPutDirectory is null, then we are ignoring any file contents, so we must return null.
		if(filename!=null && getFileSize(strName)>0 && fileOutPutDirectory!=null)
			return new File(fileOutPutDirectory, filename);
		else
			return null;
	}

	/**
		Returns the File System name of a file.
	*/
	public String getFileSystemName(String strName)
	{
		Object[] objArray = (Object[])htFiles.get(strName);
		if (objArray!=null && objArray[FILENAME]!=null)
			return (String) objArray[FILENAME];
		else
			return null;
	}

	/**
		Returns the File Size of a file.

		@return -1 if file size not defined.
	*/
	public long getFileSize(String strName)
	{
		Object[] objArray = (Object[])htFiles.get(strName);
		if (objArray!=null && objArray[SIZE]!=null)
			return ((Long)objArray[SIZE]).longValue();
		else
			return (long)-1;
	}

	private void parse(InputStream in) throws IOException
	{
		String strContentType = null;
		String strName = null;
		String strFilename = null;
		String strLine = null;
		int read = -1;

		// First run through, check that the first line is a boundary, otherwise throw a exception as format incorrect.
		read = readLine(in, blockOfBytes);
		strLine = read>0? new String(blockOfBytes, 0, read): null;

		// Must be boundary at top of loop, otherwise we have finished.
		if (strLine==null || strLine.indexOf(this.strBoundary)==-1)
			throw new IOException("Invalid Form Data, no boundary encountered.");

	    // At the top of loop, we assume that the Content-Disposition line is next, otherwise we are at the end.
		while (true)
		{
			// Get Content-Disposition line.
			read = readLine(in, blockOfBytes);
			if (read<=0)
				break; // Nothing to do.
			else
			{
				strLine = new String(blockOfBytes, 0, read);

				// TODO: Improve performance by getting both the name and filename from strLine in one go...
				strName = trimQuotes(getValue("name", strLine));
				// If this is not null, it indicates that we are processing a filename.
				strFilename = trimQuotes(getValue("filename", strLine));
				// Now if not null, strip it of any directory information.

				if (strFilename!=null)
				{
					// Fix: did not check whether filename was empty string indicating FILE contents were not passed.
					if (strFilename.length()>0)
					{
						// Need to get the content type.
						read = readLine(in, blockOfBytes);
						strLine = read>0? new String(blockOfBytes, 0, read): null;
						
						strContentType = "application/octet-stream";
						// Fix 1.11: If not null AND strLine.length() is long enough.
						if (strLine!=null&&strLine.length()>"Content-Type: ".length())
							strContentType = strLine.substring("Content-Type: ".length());// Changed 1.13
					}
					else
					{
						// FIX 1.14: IE problem with empty filename.
						read = readLine(in, blockOfBytes);
						strLine = read>0? new String(blockOfBytes, 0, read): null;
						
						if (strLine!=null && strLine.startsWith("Content-Type:"))
							readLine(in, blockOfBytes);
					}
				}

				// Ignore next line, as it should be blank.
				readLine(in, blockOfBytes);

				// No filename specified at all.
				if (strFilename==null)
				{
					// Fix: In case readParameter(in) returned null.
					String param = readParameter(in);
					if (param!=null)
						htParameters.put(strName, param);
					else
						htParameters.put(strName, "");
				}
				else
				{
					if (strFilename.length()>0)
					{
						// Get the BASENAME version of strFilename.
						strFilename = getBasename(strFilename);

						// Read the file.
						long filesize = readAndWriteFile(in, strFilename);
						if (filesize>0)
							htFiles.put(strName, new Object[] {strFilename, strContentType, new Long(filesize)} );
						else // Zero length file.
							htFiles.put(strName, new Object[] {strFilename, null, new Long(0)} );
					}
					else // Fix: FILE INPUT TYPE, but no file passed as input...
					{
						htFiles.put(strName, new Object[] {null, null, null} );
						readLine(in, blockOfBytes);	
					}
				}
			}
		}// while 
	}
	
	/**
		Read parameters, assume already passed Content-Disposition and blank line.

		@return the value read in.
	*/
	private String readParameter(InputStream in) throws IOException
	{
		StringBuffer buf = new StringBuffer();
		int read=-1;

		while(true)
		{
			read = readLine(in, blockOfBytes);
			if (read<0)
				throw new IOException("Stream ended prematurely.");
			else if (read<blockOfBytes.length && new String(blockOfBytes, 0, read).indexOf(this.strBoundary)!=-1)
				break; // Boundary found, we need to finish up.
			else 
				buf.append(getCharArray(blockOfBytes, read));
		}

		if (buf.length()>0)
			buf.setLength(getLengthMinusEnding(buf));
		return buf.toString();
	}

	/**
		Read a Multipart section that is a file type.  Assumes that the Content-Disposition/Content-Type and blank line
	 	have already been processed.  So we read until we hit a boundary, then close file and return.

		@exception IOException if an error occurs writing the file.

		@return the number of bytes read.
	*/
	private long readAndWriteFile(InputStream in, String strFilename) throws IOException
	{
		// Open the file for writing.
		BufferedOutputStream file = null;
	   	
		long fileSize = 0;
		int read = -1;
		// If not null, write it.
		byte[] endOfLineBytes = null;

		while(true)
		{
			read = readLine(in, blockOfBytes);

			if (read<0)
			{
				// file may not have been created.
				if (file!=null)
				{
					file.flush();
					file.close();
				}
				throw new IOException("Stream ended prematurely.");
			}
			else if (read<blockOfBytes.length && new String(blockOfBytes, 0, read).indexOf(this.strBoundary)!=-1)
			{
				// We have found the boundary, so we need to finish up.
				if (file!=null)
				{
					file.flush();
					file.close();
				}						
				break;
			}
			else
			{
				// Get length of new line minus ending.
				int length = getLengthMinusEnding(blockOfBytes, read);

				// Only create the file if more than zero bytes read. Create file here, as we most likely have something to write.
				// FIX: Do not bother opening a file if output directory is invalid.
				if (file==null && fileOutPutDirectory!=null && (endOfLineBytes!=null || length>0))
					file = new BufferedOutputStream(new FileOutputStream(new File(fileOutPutDirectory, strFilename)));

				// Write previous end of line, if appropriate.
				if (endOfLineBytes!=null)
				{
					// Fix...
					if (file!=null)
					{
						file.write(endOfLineBytes);
						// Update file size.
						fileSize+=endOfLineBytes.length;
					}
				}

				//int length = getLengthMinusEnding(blockOfBytes, read);
				if (length>0)
				{
					// Fix...
					if (file!=null)
					{
						file.write(blockOfBytes, 0, length);
						// Update file size.
						fileSize+=length;
					}

					// Now store the line ending, so we can spew it out if not at the boundary.
					if (endOfLineBytes==null || endOfLineBytes.length!=read-length)
						endOfLineBytes = new byte[read-length];

					System.arraycopy(blockOfBytes, length, endOfLineBytes, 0, endOfLineBytes.length);
				}
			}
		}

		return fileSize;
	}

	/**
		Returns the length of the line minus line ending.

		@param endOfArray 	This is because in many cases the byteLine will have garbage data at the end, so we
							act as though the actual end of the array is this parameter.  If you want to process
							the complete byteLine, specify byteLine.length as the endOfArray parameter.
	*/
	private static final int getLengthMinusEnding(byte byteLine[], int endOfArray)
	{
		if (byteLine==null)
			return 0;
		
		if (endOfArray>=2 && byteLine[endOfArray-2] == '\r' && byteLine[endOfArray-1] == '\n')
			return endOfArray-2;
		else if (endOfArray>=1 && byteLine[endOfArray-1] == '\n' || byteLine[endOfArray-1] == '\r')
			return endOfArray-1;
		else
			return endOfArray;
	}

	private static final int getLengthMinusEnding(StringBuffer buf)
	{
		if (buf.length()>=2 && buf.charAt(buf.length()-2) == '\r' && buf.charAt(buf.length()-1) == '\n')
			return buf.length()-2;
		else if (buf.length()>=1 && buf.charAt(buf.length()-1) == '\n' || buf.charAt(buf.length()-1) == '\r')
			return buf.length()-1;
		else
			return buf.length();
	}

	/** Converts a byte array, byte by byte to a string.
	 * The new String(byte[]) method causes problems, because it does not include the line ending (if it occurs
	 * in the byte[]) in the new String, so we had to replace with getCharArray which does a byte[] by byte
	 * conversion.  The length of the char[] returned will be (length<=0?byteLine.length: length)
	 */
	private static final char[] getCharArray(byte byteLine[], int length)
	{
		if (byteLine==null)
			return null;

		// Get the correct length.
		if (length<=0)
			length=byteLine.length;
			
		char[] charArray = new char[length];
		for (int i=0; i<length; i++)
			charArray[i] = (char)byteLine[i];
		
		return charArray;
	}

	/**
		Reads at most READ_BLOCK blocks of data, or a single line whichever is smaller.
		Returns -1, if nothing to read, or we have reached the specified content-length.

		Assumes that bytToBeRead.length indicates the block size to read.

		@return -1 if stream has ended, before a newline encountered (should never happen) OR
		we have read past the Content-Length specified.  (Should also not happen).  Otherwise
		return the number of characters read.  You can test whether the number returned is less
		than bytesToBeRead.length, which indicates that we have read the last line of a file or parameter or 
		a border line, or some other formatting stuff.
	*/
	private int readLine(InputStream in, byte[] bytesToBeRead) throws IOException 
	{
		// Ensure that there is still stuff to read...
    	if (intTotalRead >= intContentLength) 
			return -1;

		// Get the length of what we are wanting to read.
		int length = bytesToBeRead.length;

		// End of content, but some servers (apparently) may not realise this and end the InputStream, so
		// we cover ourselves this way.
		if (length > (intContentLength - intTotalRead))
        	length = (int) (intContentLength - intTotalRead);  // So we only read the data that is left.

		int result = readLine(in, bytesToBeRead, 0, length);
		// Only if we get actually read something, otherwise something weird has happened, such as the end of stream.
		if (result > 0) 
			intTotalRead += result;

		return result;	
	}

	/**
		This needs to support the possibility of a / or a \ separator.

		Returns strFilename after removing all characters before the last
		occurence of / or \.
	*/
	private static final String getBasename (String strFilename)
	{
		if (strFilename==null)
			return strFilename;

		int intIndex = strFilename.lastIndexOf("/");
		if (intIndex==-1 || strFilename.lastIndexOf("\\")>intIndex)
			intIndex = strFilename.lastIndexOf("\\");

		if (intIndex!=-1)
			return strFilename.substring(intIndex+1);
		else
			return strFilename;
	}

	/**
		trimQuotes trims any quotes from the start and end of a string and returns the trimmed string...
	*/
	private static final String trimQuotes (String strItem)
	{
		// Saves having to go any further....
		if (strItem==null || strItem.indexOf("\"")==-1)
			return strItem;
		
		// Get rid of any whitespace..
	    strItem = strItem.trim();

		if (strItem.charAt(0) == '\"')
			strItem = strItem.substring(1);
	    
	    if (strItem.charAt(strItem.length()-1) == '\"')
			strItem = strItem.substring(0, strItem.length()-1);

		return strItem;
	}

	/**
		Format of string name=value; name=value;

		If not found, will return null.
	*/
	private static final String getValue(String strName, String strToDecode)
	{
		strName = strName + "=";

		int startIndexOf=0;
		while (startIndexOf<strToDecode.length())
		{
			int indexOf = strToDecode.indexOf(strName, startIndexOf);
			// Ensure either first name, or a space or ; precedes it.
			if (indexOf!=-1)
			{
				if (indexOf==0 || Character.isWhitespace(strToDecode.charAt(indexOf-1)) || strToDecode.charAt(indexOf-1)==';')
				{
					int endIndexOf = strToDecode.indexOf(";", indexOf+strName.length());
					if (endIndexOf==-1) // May return an empty string...
						return strToDecode.substring(indexOf+strName.length());
					else
						return strToDecode.substring(indexOf+strName.length(), endIndexOf);
				}
				else
					startIndexOf=indexOf+strName.length();
			}
			else
				return null;
		}
		return null;
	}

	/**
     * <I>Tomcat's ServletInputStream.readLine(byte[],int,int)  Slightly Modified to utilise in.read()</I>
	 * <BR>
     * Reads the input stream, one line at a time. Starting at an
     * offset, reads bytes into an array, until it reads a certain number
     * of bytes or reaches a newline character, which it reads into the
     * array as well.
     *
     * <p>This method <u><b>does not</b></u> returns -1 if it reaches the end of the input
     * stream before reading the maximum number of bytes, it returns -1, if no bytes read.
     *
     * @param b 		an array of bytes into which data is read
     *
     * @param off 		an integer specifying the character at which
     *					this method begins reading
     *
     * @param len		an integer specifying the maximum number of 
     *					bytes to read
     *
     * @return			an integer specifying the actual number of bytes 
     *					read, or -1 if the end of the stream is reached
     *
     * @exception IOException	if an input or output exception has occurred
     *
	 
		Note: We have a problem with Tomcat reporting an erroneous number of bytes, so we need to check this.
		This is the method where we get an infinite loop, but only with binary files.
     */
    private int readLine(InputStream in, byte[] b, int off, int len) throws IOException 
	{
		if (len <= 0) 
		    return 0;

		int count = 0, c;

		while ((c = in.read()) != -1) 
		{
	    	b[off++] = (byte)c;
		    count++;
		    if (c == '\n' || count == len) 
				break;
		}

		return count > 0 ? count : -1;
    }

	/** 
		For debugging.
	 */
	public String getHtmlTable()
	{
		StringBuffer sbReturn = new StringBuffer();

		sbReturn.append("<H1>Parameters</h1>");
		sbReturn.append("\n<table border=2><tr><td><b>Name</b></td><td><b>Value</b></td></tr>");
		for (Enumeration e = getParameterNames() ; e.hasMoreElements() ;) 
		{
			String strName = (String) e.nextElement();
			sbReturn.append("\n<tr>" +
							"<td>" + strName + "</td>" +
							"<td>" + getURLParameter(strName) + "</td>" +
							"</tr>");
        }
		sbReturn.append("</table>");

		sbReturn.append("\n<table border=2><tr><td><b>Name</b></td><td><b>Filename</b></td><td><b>Path</b></td><td><b>Content Type</b></td><td><b>Size</b></td></tr>");
		for (Enumeration e = getFileParameterNames() ; e.hasMoreElements() ;) 
		{
			String strName = (String) e.nextElement();

			sbReturn.append("\n<tr>" +
							"<td>" + strName + "</td>" +
							"<td>" + (getFileSystemName(strName)!=null?getFileSystemName(strName):"") + "</td>" +
							"<td>" + (getFile(strName)!=null?getFile(strName).getAbsolutePath():"") + "</td>" +
							"<td>" + (getContentType(strName)!=null?getContentType(strName):"") + "</td>" +
							"<td>" + (getFileSize(strName)!=-1?getFileSize(strName)+"":"") + "</td>" +
							"</tr>");
        }
		sbReturn.append("</table>");

		return sbReturn.toString();
	}

	//For debug.
	private void debug(String x)
	{
		if (debug!=null)
		{
			debug.println(x);
			debug.flush();
		}
	}
}
