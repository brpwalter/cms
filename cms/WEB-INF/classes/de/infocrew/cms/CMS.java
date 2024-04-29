package de.infocrew.cms;

import java.io.*;
import java.sql.*;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.servlet.*;
import javax.servlet.http.*;

// Multipart-Form-Handling class:
import de.infocrew.multipart.MultipartRequestBLOB;

/**
 * Content Management System
 * <p>
 * backend to JSP pages
 *
 * @author Stefan Schmidt
 */
public class CMS {

    /*
     * temporary variables for our database connection,
     * a statement and a result
     */
    private Connection con = null;
    private Statement stmt = null;
    private ResultSet cmsResult = null;

    /*
     * what table does or data come from?
     * (set from the ServletContext's init parameters)
     */
    private String table = null;

    /*
     * temporary variables for our "environment"
     */
    private ServletContext context = null;
    private HttpServletRequest request = null;

    /*
     * temporary storage for one record
     */
    private int id = 0;
    private int category = 1;
    private int display = 0;

    private Date news_date = null;
    private Date release_date = null;

    private String headline = null;
    private String content = null;

    private Blob image = null;
    private String imagetype = null;
    private String imagetext = null;

    private Blob download = null;
    private String downloadtype = null;
    private String downloadtext = null;
    private String downloadname = null;


    /**
     * number of records selected 
     * (used by the paging function)
     */
    private int recordCount = 0;

    /**
     * number of records per page
     * (can be overridden from the calling JSP
     */
    private int numberPerPage = 5;

    /**
     * starting page number
     */
    private int startPage = 0;

    /**
     * this is called by the JSP to establish the database 
     * connection and to set some global variables 
     *
     * @param context    the servlet context, required to gain 
     *                   access to the context init parameters
     * @param request    the servlet request. Since the scope of
     *                   this bean is "request", it doesn't 
     *                   change during this object's lifetime. So
     *                   we keep one global reference here.
     * @return true      if successful 
     *                   (database connection established),
     *         false     otherwise.	       
     */
    public boolean initialize (ServletContext context, 
			       HttpServletRequest request) {
	this.context = context;
	this.request = request;

	try {
	    // get database parameters from servlet context:
	    // (set these in web.xml!)

	    String dbdriver = 
                context.getInitParameter ("DatabaseDriver");
	    String dburl = 
                context.getInitParameter ("DatabaseURL");
	    String dbuser = 
		context.getInitParameter ("DatabaseUser");
	    String dbpasswd = 
		context.getInitParameter ("DatabasePassword");

	    table = context.getInitParameter ("DatabaseTable");

	    // connect to database:
	    Class.forName (dbdriver);
	    con = DriverManager
		.getConnection (dburl, dbuser, dbpasswd);

	    return true;
	} catch (Exception e) {
	    logError ("Error initializing bean: ", e);
	    return false;
	}
    }


    /**
     * queries the database to get a list of articles.
     * 
     * @param category     the category, 0 for all categories
     * @param onlyReleased true to show only records that are
     *                     released (flag display == 1) and
     *                     whose release_date is before current
     *                     date.
     * @param startPage    jump to this page's first record after
     *                     selecting.
     * 
     * @return true        if successful 
     *                     (we are on a valid record)
     *         false       otherwise.
     */
    public boolean queryCMS (int category, 
			     boolean onlyReleased, 
			     int startPage) {
	try {
	    // refuse to work if we haven't been initialized:
	    if (con == null) return false;

	    // build where clause:
	    String where = "";
	    if (category != 0) {
		if (!where.equals ("")) 
		    where += " AND "; 
		else 
		    where = " WHERE ";
		where += "category="+category;
	    }
	    if (onlyReleased) {
		if (!where.equals ("")) 
		    where += " AND "; 
		else 
		    where = " WHERE ";
		where += "display>0 AND release_date<='"
		    +new Date (System.currentTimeMillis())+"'";
	    }

	    // create Statement that allows moving back- and 
	    // forward in the results: (needed for paging)
            stmt = con.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE, 
                ResultSet.CONCUR_READ_ONLY);
	    stmt.setFetchDirection (ResultSet.FETCH_UNKNOWN);

	    // do query:
            cmsResult = 
		stmt.executeQuery ("SELECT * FROM " 
				   + table + where 
				   + " ORDER by news_date DESC");

	    // jump to last record to get number of records,
	    // then jump to first record on requested page:
	    if (cmsResult.last ()) {
		recordCount = cmsResult.getRow ();
		if (!cmsResult.absolute (
                    startPage * numberPerPage + 1))

		    if (startPage > 0) 
			// in case current page is empty 
			// (i.e. after deleting a record)
			// go to previous page
			cmsResult.absolute (
			    --startPage * numberPerPage + 1);

		return readFieldsFromDatabase ();
	    } 
	    else 
		return false;
	} catch (SQLException e) {
	    logError ("Error selecting articles:", e);
	    return false;
	}
    }

    /**
     * queries the database to get a list of articles.
     * 
     * @param category     the category, 0 for all categories
     * @param onlyReleased true to show only records that are
     *                     released (flag display == 1) and
     *                     whose release_date is before current
     *                     date.
     * 
     * @return true        if successful 
     *                     (we are on a valid record)
     *         false       otherwise.
     */
    public boolean queryCMS (int category, 
			     boolean onlyReleased) {
	return queryCMS (category, onlyReleased, 0);
    }

    /**
     * queries the database to select a single article
     * 
     * @param id           ID of the requested record
     * 
     * @return true        if successful 
     *                     (we are on a valid record)
     *         false       otherwise.
     */
    public boolean queryCMS (int id) {
	try {
	    if (con == null) return false;
            stmt = con.createStatement ();

	    String sql = "SELECT * FROM " 
		+ table + " WHERE id="+id;
	    cmsResult = stmt.executeQuery (sql);

	    return nextRecord ();
	} catch (SQLException e) {
	    logError ("Error selecting single article (ID "
		      + id + "):", e);
	    return false;
	}
    }


    /**
     * moves to next record of the result set and reads it in 
     * to the temporary variables.
     *
     * @return true        if successful 
     *                     (we are on a valid record)
     *         false       otherwise.
     */
    public boolean nextRecord () {
	if (cmsResult == null) 
	    return false;
	try {
	    if (!cmsResult.next ()) 
		return false; // if no next record
	    else
		return readFieldsFromDatabase ();
	} catch (SQLException e) {
	    logError ("Error moving to next record:",e);
	    return false;
	}
    }

    /**
     * read in fields from current row of the result set
     * to the temporary variables.
     *
     * @return true        if successful 
     *                     (we are on a valid record)
     *         false       otherwise.
     */
    private boolean readFieldsFromDatabase () {
	if (cmsResult == null) 
	    return false;
	try {
	    if (!cmsResult.relative (0)) 
		return false;
	    id = cmsResult.getInt ("id");
	    category = cmsResult.getInt ("category");
	    display = cmsResult.getInt ("display");
	    news_date = cmsResult.getDate ("news_date");
	    release_date = cmsResult.getDate ("release_date");

	    headline = cmsResult.getString ("headline");
	    content = cmsResult.getString ("content");

	    image = cmsResult.getBlob ("image");
	    imagetype = cmsResult.getString ("imagetype");
	    imagetext = cmsResult.getString ("imagetext");

	    download = cmsResult.getBlob ("download");
	    downloadtype = cmsResult.getString ("downloadtype");
	    downloadtext = cmsResult.getString ("downloadtext");
	    downloadname = cmsResult.getString ("downloadname");

	    return true;
	} catch (SQLException e) {
	    logError ("Error reading current row:",e);
	    return false;
	}
    }


    /*
     * Bean methods providing access to the article's data
     */

    /**
     * @return current article's ID number
     */
    public int getId () {
	return id;
    }

    /**
     * @return current article's category number
     */
    public int getCategory () {
	return category;
    }

    /**
     * @return true if article is released, false if not
     *         (from database field display)
     */
    public boolean isReleased () {
	return (display == 1);
    }


    /**
     * @return number of articles selected
     */
    public int getRecordCount () {
	return recordCount;
    }

    /**
     * @return number of articles per page
     */
    public int getNumberPerPage () {
	return numberPerPage;
    }

    /**
     * @param numberPerPage how many articles go on one page
     */
    public void setNumberPerPage (int numberPerPage) {
	this.numberPerPage = numberPerPage;
    }

    /**
     * @return current page to show
     */
    public int getStartPage () {
	return startPage;
    }

    /**
     * @param startPage current page to show
     */
    public void setStartPage (int startPage) {
	this.startPage = startPage;
    }


    /**
     * @return current article's headline, 
     *         encoded for HTML output
     */
    public String getHeadline () {
	return encode (headline);
    }

    /**
     * @return current article's body, 
     *         encoded for HTML output
     */
    public String getContent () {
	return encode (content);
    }

    /**
     * @return current article's date,
     *         formatted (german locale)
     */
    public String getNews_date () {
	try {
	    DateFormat df = 
		DateFormat.getDateInstance (DateFormat.MEDIUM, 
					    Locale.GERMAN);
	    return encode (df.format (news_date));
	} catch (Exception e) {
	    return "-";
	}
    }

    /**
     * @return current article's release date,
     *         formatted (german locale)
     */
    public String getRelease_date () {
	try {
	    DateFormat df = 
		DateFormat.getDateInstance (DateFormat.MEDIUM, 
					    Locale.GERMAN);
	    return encode (df.format (release_date));
	} catch (Exception e) {
	    return "-";
	}
     }

    /**
     * @return current article's day of month
     */
    public int getNews_day () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (news_date);
	return g.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @return current article's month
     */
    public int getNews_month () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (news_date);
	return g.get(Calendar.MONTH)+1;
    }

    /**
     * @return current article's year
     */
    public int getNews_year () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (news_date);
	return g.get(Calendar.YEAR);
    }

    /**
     * @return current article's release day of month
     */
    public int getRelease_day () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (release_date);
	return g.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * @return current article's release month
     */
    public int getRelease_month () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (release_date);
	return g.get(Calendar.MONTH)+1;
    }

    /**
     * @return current article's release year
     */
    public int getRelease_year () {
	GregorianCalendar g = new GregorianCalendar ();
	g.setTime (release_date);
	return g.get(Calendar.YEAR);
    }


    /**
     * @return current article's image data
     */
    public Blob getImage () {
	return image;
    }

    /**
     * @return true if article has an image
     */
    public boolean hasImage() {
	try {
	    return (image != null) && (image.length() > 0);
	} catch (Exception e) {
	    return false;
	}
    }

    /**
     * @return current article's image's MIME type
     */
    public String getImagetype () {
	return imagetype;
    }

    /**
     * @return current article's image's description text
     */
    public String getImagetext () {
	return encode (imagetext);
    }

    /**
     * @return current article's download file data
     */
    public Blob getDownload () {
	return download;
    }

    /**
     * @return true if article has a download file
     */
    public boolean hasDownload() {
	try {
	    return (download != null) && (download.length() > 0);
	} catch (Exception e) {
	    return false;
	}
    }

    /**
     * @return current article's download file's MIME type
     */
    public String getDownloadtype () {
	return downloadtype;
    }

    /**
     * @return current article's download file's description text
     */
    public String getDownloadtext () {
	return encode (downloadtext);
    }

    /**
     * @return current article's download file's filename
     *         as provided by the browser during upload
     */
    public String getDownloadname () {
	return downloadname;
    }



    /**
     * processes all requests in the redactionary system.
     *
     * @return 0 in case of some error.
     *         1 if the list of articles is to be shown.
     *         2 if the editing form is to be shown.
     */
    public int processRequest () {

	/*
	 * edit an existing record?
	 */
	if (null != request.getParameter ("edit")) {
	    startPage = parseInt (request.getParameter ("s"));
	    // select article to edit:
	    if (queryCMS (
		parseInt (request.getParameter ("edit"))))
		return 2;
		else return 0;
	} 
	/*
	 * or create a new record?
	 */
	else if (null != request.getParameter ("new")) {
	    /*
	     * create empty record
	     */
	    category = parseInt (
		request.getParameter ("category"));
	    startPage = parseInt (request.getParameter ("s"));

	    id = 0;
	    display = 0;

	    release_date = news_date = 
		new Date (System.currentTimeMillis ());

	    headline = "";
	    content = "";

	    imagetext = "";
	    downloadtext = "";
	    downloadname = "";

	    return 2;
	} 
	/*
	 * or is this the answer to the editing form
	 * (then it's a multipart form)?
	 */
	else if (request.getContentType () != null && 
		 request.getContentType().toLowerCase()
		 .startsWith("multipart/form-data")) {
	    try {
		// parse multipart form:
		MultipartRequestBLOB parser = 
		    new MultipartRequestBLOB(
			 request.getContentType(), 
			 request.getContentLength(), 
			 request.getInputStream(), 
			 "/tmp");

		// load form parameters:
		id = parseInt (parser.getURLParameter ("id"));
		category = parseInt (
			   parser.getURLParameter ("category"));
		startPage = parseInt (
                            parser.getURLParameter ("s"));
		display = parseInt (
                          parser.getURLParameter ("display"));

		// rebuild date fields:
		news_date = 
		    Date.valueOf (
			parser.getURLParameter ("news_year")
			.trim ()+"-"+
			parser.getURLParameter ("news_month")
			.trim ()+"-"+
			parser.getURLParameter ("news_day")
			.trim ());
		release_date = 
		    Date.valueOf (
			parser.getURLParameter ("release_year")
			.trim ()+"-"+
			parser.getURLParameter ("release_month")
			.trim ()+"-"+
			parser.getURLParameter ("release_day")
			.trim ());

		headline = parser.getURLParameter ("headline");
		content = parser.getURLParameter ("content");

		image = parser.getFile ("image");
		imagetype = parser.getContentType ("image");
		imagetext = parser.getURLParameter ("imagetext");

		download = parser.getFile ("download");
		downloadtype = 
		    parser.getContentType ("download");
		downloadname = 
		    parser.getFileSystemName ("download");
		downloadtext = 
		    parser.getURLParameter ("downloadtext");

		boolean delimage = (null != 
		    parser.getURLParameter ("delimage"));
		boolean deldownload = (null != 
		    parser.getURLParameter ("deldownload"));

		// refuse to work if database connection not 
		// available:
		if (con == null) 
		    return 0;
		PreparedStatement pstmt = null;
		try {

		    // is this an edited old record?
		    if (id > 0) { 
			// prepare UPDATE statement
			String sql = "category=?,display=?,news_date=?,release_date=?,headline=?,content=?,imagetext=?,downloadtext=?";

			if ((image != null) && 
			    (image.length() > 0)) {
			    // insert/update image
			    sql += ",image=?,imagetype=?";
			} 
			else if (delimage) 
			    sql += ",image=NULL,imagetype=''";

			if ((download != null) && 
			    (download.length() > 0)) {
			    // insert/update download file
			    sql += ",download=?,downloadtype=?,downloadname=?";
			}
			else if (deldownload)
			    sql += ",download=NULL,downloadtype='',downloadname=''";

			pstmt = con.prepareStatement ("UPDATE "
			    +table+" SET "+sql+" WHERE id=?");
		    } 
		    else {
			// prepare INSERT statement
			String fields = "category,display,news_date,release_date,headline,content,imagetext,downloadtext,image,imagetype,download,downloadtype,downloadname";
			String values = 
			    "?,?,?,?,?,?,?,?,?,?,?,?,?";

			pstmt = con.prepareStatement (
                            "INSERT INTO "+table+" ("+fields
			    +") VALUES ("+values+")");
		    }

		    // fill in the article data:
		    int i = 0;
		    pstmt.setInt (++i, category);
		    pstmt.setInt (++i, display);
		    pstmt.setDate (++i, news_date);
		    pstmt.setDate (++i, release_date);
		    pstmt.setString (++i, headline);
		    pstmt.setString (++i, content);
		    pstmt.setString (++i, imagetext);
		    pstmt.setString (++i, downloadtext);
		    if (id > 0) {
			if ((image != null) && 
			    (image.length() > 0)) {
			    pstmt.setBlob (++i, image);
			    pstmt.setString (++i, imagetype);
			}
			if ((download != null) && 
			    (download.length() > 0)) {
			    pstmt.setBlob (++i, download);
			    pstmt.setString (++i, downloadtype);
			    pstmt.setString (++i, downloadname);
			}
			pstmt.setInt (++i, id);
		    }
		    else {
			if ((image != null) && 
			    (image.length() > 0)) {
			    pstmt.setBlob (++i, image);
			    pstmt.setString (++i, imagetype);
			} else {
			    pstmt.setNull (++i, Types.BLOB);
			    pstmt.setString (++i, "");
			}
			if ((download != null) && 
			    (download.length() > 0)) {
			    pstmt.setBlob (++i, download);
			    pstmt.setString (++i, downloadtype);
			    pstmt.setString (++i, downloadname);
			} else {
			    pstmt.setNull (++i, Types.BLOB);
			    pstmt.setString (++i, "");
			    pstmt.setString (++i, "");
			}
		    }

		    // and update the database:
		    pstmt.execute ();
		    pstmt.close ();
		} catch (SQLException e) {
		    logError ("error processing article update",
			      e);
		    // something went wrong, 
		    // -> return to edit form
		    return 2;
		}
		// done: show list of articles:
		return 1;
	    } catch (IOException e) {
		logError ("error processing file upload",e);
	    }
	}
	/*
	 * otherwise: show list of articles.
	 */
	else {
	    category = parseInt (
		      request.getParameter ("category"));
	    if (category == 0) category++;
	    startPage = parseInt (request.getParameter ("s"));

	    // toggle released flag if requested:
	    processToggleReleasedFlag ();

	    // delete article if requested:
	    processDeleteCommand ();

	    return 1;
	}
	/*
	 * this line is only reached in case of error:
	 */
	return 0;
    }


    /**
     * deletes the article whose ID is passed as request 
     * parameter "delete".
     */
    private void processDeleteCommand () {
	if (con == null) return;
	try {
	    int deleteID = parseInt (
		request.getParameter ("delete"));
	    if (deleteID > 0) {
		stmt = con.createStatement ();
		stmt.executeUpdate ("DELETE FROM "+table
				    +" WHERE id="+deleteID);
	    }
	} catch (SQLException e) {
	    logError ("Error deleting record:", e);
	}
    }

    /**
     * toggles released flag on the article whose ID is passed
     * as request parameter "toggle". It changes the field 
     * "display" in the database.
     */
    private void processToggleReleasedFlag () {
	if (con == null) return;
	try {
	    int toggleID = parseInt (
		request.getParameter ("toggle"));
	    if (toggleID > 0) {
		stmt = con.createStatement ();
		stmt.executeUpdate ("UPDATE "+table
		    +" SET display=1-display WHERE id="
		    +toggleID);
	    }
	} catch (SQLException e) {
	    logError ("Error changing released flag:", e);
	}
    }



    /*
     *
     *
     *
     * Library functions
     *
     *
     *
     *
     */


    /**
     * filters null Strings
     * @param s input String or null
     * @return input string if not null, an empty String "" 
     *         otherwise
     */
    public static String noNull (String s) {
        if (s == null)
            return "";
        else
            return s;
    }


    /**
     * converts HTML special characters <, > and & to their 
     * corresponding HTML Entities.
     * @param s input string
     * @return HTML-escaped string
     */
    public static String encode (String s) {
	if (s == null) return "";

	char c;
	StringBuffer buf = new StringBuffer ();
	for (int i=0; i < s.length(); i++) {
	    switch (c = s.charAt (i)) {
	    case '<':
		buf.append ("&lt;");
		break;
	    case '>':
		buf.append ("&gt;");
		break;
	    case '&':
		buf.append ("&amp;");
		break;
	    default:
		buf.append (c);
	    }
	}
	return buf.toString ();
    }

    /**
     * doubles single quotes ' from a String
     * to use in SQL Statements
     *
     * @param s input String
     * @return input String with ' replaced by ''
     */
    public static String quote (String s) {
        return replace ("'", "''", s);
    }


    /**
     * replaces words in a String using a StringBuffer
     *
     * @param pattern text to search for
     * @param replacement
     * @param input the input String to process
     * @return processed String
     */
    public static String replace (String pattern, 
				  String replacement,
				  String input) {
	if(input == null) return "";
	
	StringBuffer temp = new StringBuffer();
	int i = 0, j;
 
	while((j = input.indexOf(pattern, i)) >=0)
	    {
		temp.append(input.substring(i,j));
		temp.append(replacement);
		i=j+pattern.length();
	    }
	temp.append(input.substring(i));
	return temp.toString();
    }


    /**
     * wrapper around Integer.parseInt ()
     * @param s a string containing a decimal integer
     * @return the parsed integer value,
     *         0 in case the input didn't contain a valid number
     */
    public static int parseInt (String s) {
        if (s==null) return 0;
        try {
            return Integer.parseInt (s.trim ());
        } catch (NumberFormatException e) {
        }
        return 0;
    }

    /**
     * Logs a message,
     * currently on System.err
     * @param message message for log file
     */
    public static void logError (String message) {
	System.err.print ("CMS: ");
	System.err.print (getCurrentDateTime () +" :");
	System.err.println (message);
    }

    /**
     * Logs a message and an exception
     * currently on System.err
     * @param message message for log file
     * @param exception
     */
    public static void logError (String message, 
				 Throwable exception) {
	System.err.print ("CMS: ");
	System.err.print (getCurrentDateTime () +" :");
	System.err.println (message);
        exception.printStackTrace (System.err);
    }

    /**
     * Logs a message,
     * currently on System.out
     * @param message message for log file
     */
    public static void log (String message) {
	System.out.print ("CMS: ");
	System.out.print (getCurrentDateTime ()+" :");
	System.out.println (message);
    }

    /**
     * returns current date and time as a string 
     * (for logging etc.)
     * @return current date and time
     */
    public static String getCurrentDateTime () {
	return 
	    java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.LONG,
		java.text.DateFormat.LONG)
	        .format (new java.util.Date()); 
    }
 

    /**
     * formats text to HTML.
     * allows tags <b> for bold 
     * and <link>href<linktext>description</link> for links.
     * This function also convert \n to <br>.
     * 
     * @param  input        String to format. This is assumed to
     *                      having already been processed by 
     *                      encode())
     * @return formatted HTML String.
     */
    public static String formatText (String input) {
	return (
		newline2br (
		replace ("&lt;b&gt;", "<b>", 
		replace ("&lt;/b&gt;", "</b>",
		replace ("&lt;link&gt;", "<A TARGET=\"_blank\" HREF=\"", 
		replace ("&lt;linktext&gt;","\">",
		replace ("&lt;/link&gt;","</A>",
		input)))))));

    }

    /**
     * converts newlines (\n) to <br>.
     * 
     * @param input String containing newlines
     * @return String with newlines replaced by <br>
     */
    public static String newline2br (String input) {
	return (replace ("\n", "<br>", input));
    }
    
} // end of class
