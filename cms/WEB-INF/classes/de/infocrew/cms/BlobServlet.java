package de.infocrew.cms;

import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import java.util.*;
import java.io.*;

/**
 * BLOB-Servlet
 * <p>
 * serves up binary data like Images, PDF files, etc.
 *
 * @author Stefan Schmidt
 */
public class BlobServlet extends HttpServlet {

    /**
     * servlet service routine
     * <p>
     * this servlet uses the path information 
     * (request.getPathInfo ()) in order to determine which image
     * or file is requested. The path should look like this: <br>
     * /image/id                  <br>in case of an image or <br>
     * /download/id/filename      <br>in case of a file. <br>
     * Replace id with the actual ID of the record. filename is
     * the name that the user is proposed when saving the file to
     * disk.
     *
     * @param  request   the servlet request
     * @param  response  the servlet response
     */
    public void doGet(HttpServletRequest request,
		      HttpServletResponse response)
	throws ServletException, IOException
    {
	// initialize database access bean
	CMS cms = new CMS ();
	cms.initialize (getServletContext (), request);

	try {
	    Blob blob = null;
	    String contentType = null;

	    String path = request.getPathInfo ().toLowerCase ();

	    // we use a StringTokenizer to split the path info
	    // into its parts:
	    StringTokenizer st = new StringTokenizer (path, "/");
	    try {
		String action = st.nextToken ();
		int id = cms.parseInt (st.nextToken ());

		// query database:
		cms.queryCMS (id);

		// fetch image or download data and type:
		if (action.equals ("image")) {
		    blob = cms.getImage ();
		    contentType = cms.getImagetype ();
                }
		else if (action.equals ("download")) {
		    blob = cms.getDownload ();
		    contentType = cms.getDownloadtype ();
		}
	    } catch (NoSuchElementException e) {
	    }


	    // now, did we get the data? if yes, copy it
	    // to the response:
	    if ((blob != null) && (contentType != null)) {
		response.setContentType (contentType);

		ServletOutputStream out = 
		    response.getOutputStream ();

		byte b[];
		long toRead = blob.length ();
		long read = 0;
		while (toRead > 0) {
		    b = blob.getBytes (read, 
			(toRead > 1024) ? 1024 : (int)toRead);
		    toRead -= b.length;
		    read += b.length;
		    out.write (b);
		}

		out.flush ();
		out.close ();
	    }
	} catch (Exception e) {
	    CMS.logError ("BLOB-Servlet: ", e);
	}
    }
}
