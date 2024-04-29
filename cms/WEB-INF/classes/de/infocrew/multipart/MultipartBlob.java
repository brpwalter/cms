/**
 *
 * specialized Blob class, wrapping files uploaded
 * from a multipart/form-data HTML form
 *
 * @author Stefan Schmidt
 */


package de.infocrew.multipart;

import java.io.*;
import java.sql.Blob;


public class MultipartBlob implements Blob {
    private byte[] data;
    public String filename;
    public String contentType;


    /**
     * Constructor.
     * @param data the file's data as a byte array.
     * @param filename the file's system filename
     * @param contentType the file's MIME type
     */
    public MultipartBlob (byte[] data, String filename, String contentType) {
	super ();
	this.data = data;
	this.filename = filename;
	this.contentType = contentType;
    }

    /**
     * get data from the file
     *
     * @param pos    starting offset
     * @param length requested size of the data block in bytes.
     * @return the block of data from the file as a byte array.
     */
    public byte[] getBytes(long pos, int length) {
	if (data == null) return null;

	byte[] bytes = new byte[length];
	System.arraycopy (data, (int)pos, bytes, 0, length);
	return bytes;
    }

    /**
     * @return an InputStream to read the file's data.
     */
    public InputStream getBinaryStream () {
	return new ByteArrayInputStream (data);
    }

    /**
     * @return size of this file in bytes
     */
    public long length() {
	if (data != null)
	    return data.length;
	else 
	    return 0;
    }

    /**
     * search function, not implemented
     */
    public long position(byte[] pattern, long start) { 
	return -1;
    }

    /**
     * search function, not implemented
     */
    public long position(Blob pattern, long start) { 
	return -1;
    }


} // end of class
