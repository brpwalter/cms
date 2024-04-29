<%@ page language="java" 
	 session="false" 
%>
<jsp:useBean id="news" scope="request" class="de.infocrew.cms.CMS" />
<%
  news.setNumberPerPage (3);

  int category = news.getCategory ();
  int startPage = news.getStartPage ();

%>
	  <TABLE BORDER=0>
	    <TR>
	      <TD COLSPAN=2>
		<H1>Redaktionssystem</H1>
	      </TD>
	    </TR>
	    <TR>
	      <TD>
		<FORM ACTION="index.jsp" METHOD="GET">
		  Kategorie: 
		  <SELECT NAME="category">
		    <OPTION VALUE="1"<% if (category==1) {%> SELECTED<% } %>>Aktuelles
		    <OPTION VALUE="2"<% if (category==2) {%> SELECTED<% } %>>Presseinfos
		  </SELECT>
		  <INPUT TYPE="SUBMIT" VALUE="Go!">
		</FORM>
	      </TD>
	      <TD ALIGN="RIGHT">
		  <A HREF="index.jsp?category=<%= category %>&s=<%= startPage %>&new="><IMG SRC="icons/new.gif" WIDTH=32 HEIGHT=32 ALT="neuer Eintrag" BORDER=0></A>
	      </TD>

	    </TR>
<%

  if (news.queryCMS (category, false, startPage)) {

    int counter = 0;
%>
	    <TR>
	      <TD COLSPAN=2>
<jsp:include page="../includes/pager.jsp">
  <jsp:param name="url" value="?category=<%= category %>" />
</jsp:include>
	      </TD>
	    </TR>
	    <TR>
	      <TD COLSPAN=2>
<!-- Uebersichtstabelle im Redaktionssystem -->
                <TABLE BORDER=0>
		  <TR BGCOLOR="gray">
		    <TD>Frei</TD>
		    <TD>Datum/<BR>V&Ouml;-Datum</TD>
		    <TD>&Uuml;berschrift</TD>
		    <TD>Aktion</TD>
		  </TR>
<%
      do {
	counter++;
        int id = news.getId ();
        boolean released = news.isReleased ();
        String news_date = news.getNews_date ();
        String release_date = news.getRelease_date ();
        String headline = news.getHeadline ();
%>
		  <TR BGCOLOR="<% if ((counter % 2) == 0) {%>lightyellow<% } else { %>silver<%}%>">
		    <TD>
		      <A HREF="index.jsp?category=<%= category %>&s=<%= startPage %>&toggle=<%= id %>"><% if (released) { 
                          %><IMG SRC="icons/greenlight.gif" WIDTH=32 HEIGHT=32 BORDER=0><% } else { 
                          %><IMG SRC="icons/redlight.gif" WIDTH=32 HEIGHT=32 BORDER=0><% } %></A>
                    </TD>
		    <TD>
                      <%= news_date %><BR>
                      <FONT COLOR="red"><%= release_date %></FONT>
	            </TD>
		    <TD><%= headline %></TD>
		    <TD><A HREF="index.jsp?category=<%= category %>&s=<%= startPage %>&delete=<%= id %>"><IMG SRC="icons/trash.gif" WIDTH=32 HEIGHT=32 ALT="L&ouml;schen" BORDER=0></A><A HREF="index.jsp?category=<%= category %>&s=<%= startPage %>&edit=<%= id %>"><IMG SRC="icons/edit.gif" WIDTH=32 HEIGHT=32 ALT="Bearbeiten" BORDER=0></TD>
		  </TR>
<% 
      } while (news.nextRecord () && (counter < news.getNumberPerPage ()));
%>
                </TABLE>                
	      </TD>
	    </TR>
	    <TR>
	      <TD COLSPAN=2>
<jsp:include page="../includes/pager.jsp">
  <jsp:param name="url" value="?category=<%= category %>" />
</jsp:include>
	      </TD>
	    </TR>
<% 
    }
%>

	  </TABLE>

