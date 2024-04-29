<%@ page language="java" 
	 session="false" 
%>
<jsp:useBean id="news" scope="request" class="de.infocrew.cms.CMS" />
<%
  int category = news.getCategory ();

%>
	<FORM ACTION="index.jsp" METHOD="POST" ENCTYPE="multipart/form-data">
          <INPUT TYPE="HIDDEN" NAME="s" VALUE="<%= news.getStartPage () %>">
          <INPUT TYPE="HIDDEN" NAME="id" VALUE="<%= news.getId () %>">
          <INPUT TYPE="HIDDEN" NAME="display" VALUE="<%= news.isReleased () ? 1 : 0 %>">
	  <TABLE BORDER=0>
	    <TR>
	      <TD COLSPAN=2>
		<H1>Redaktionssystem</H1>
	      </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
		  Kategorie:
              </TD>
              <TD>
		  <SELECT NAME="category">
		    <OPTION VALUE="1"<% if (category==1) {%> SELECTED<% } %>>Aktuelles
		    <OPTION VALUE="2"<% if (category==2) {%> SELECTED<% } %>>Presseinfos
		  </SELECT>
	      </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                Datum
              </TD>
              <TD>
                    <select name="news_day" size="1">
                      <%
		  for (int i = 1; i<=31; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getNews_day()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>.&nbsp;<select name="news_month" size="1">
                      <%
		  for (int i = 1; i<=12; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getNews_month()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>.&nbsp; <select name="news_year" size="1">
                      <%
		  for (int i = news.getNews_year()-3; i<= news.getNews_year()+4; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getNews_year()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>
              </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                V&Ouml;-Datum
              </TD>
              <TD>
                    <select name="release_day" size="1">
                      <%
		  for (int i = 1; i<=31; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getRelease_day()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>.&nbsp;<select name="release_month" size="1">
                      <%
		  for (int i = 1; i<=12; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getRelease_month()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>.&nbsp; <select name="release_year" size="1">
                      <%
		  for (int i = news.getRelease_year()-3; i<= news.getRelease_year()+4; i++) {
		    %> 
                      <OPTION VALUE=<%=i%><%if (i == news.getRelease_year()) {%> SELECTED<%}%>><%=i%><%
		  }
		%> 
                    </select>
              </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                &Uuml;berschrift
              </TD>
              <TD>
                <INPUT TYPE="TEXT" SIZE=55 NAME="headline" VALUE="<%= news.getHeadline () %>">
              </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                Text
              </TD>
              <TD>
                <TEXTAREA NAME="content" WRAP="VIRTUAL" ROWS=20 COLS=55><%= news.getContent () %></TEXTAREA><BR>
	        M&ouml;gliche Attribute:<BR>
                Fettdruck: &lt;b&gt;fetter Text&lt;/b&gt;<BR>
                Link: &lt;link&gt;http://www.abc.xyz/&lt;linktext&gt;Linkbeschreibung&lt;/link&gt;
              </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                Bild
              </TD>
              <TD>
                <% if (news.hasImage()) { %><IMG SRC="<%=request.getContextPath ()%>/blob/image/<%=news.getId ()%>" WIDTH=300><BR><% } %>
                <INPUT TYPE="FILE" name="image"><% if (news.hasImage()) { %>
                <INPUT TYPE="CHECKBOX" NAME="delimage">&nbsp;Bild l&ouml;schen<BR><% } %><BR>
                Beschreibung: <INPUT TYPE="TEXT" SIZE=35 NAME="imagetext" VALUE="<%= news.getImagetext () %>"><BR>
              </TD>
	    </TR>
	    <TR>
	      <TD VALIGN="TOP">
                Download
              </TD>
              <TD>
                <% if (news.hasDownload()) { %><A HREF="<%=request.getContextPath ()%>/blob/download/<%=news.getId ()%>/<%=news.getDownloadname ()%>">Download</A><BR><% } %>
                <INPUT TYPE="FILE" NAME="download"><% if (news.hasDownload()) { %> 
                <INPUT TYPE="CHECKBOX" NAME="deldownload">&nbsp;Download l&ouml;schen<BR><% } %><BR>
                Beschreibung: <INPUT TYPE="TEXT" SIZE=35 NAME="downloadtext" VALUE="<%= news.getDownloadtext () %>"><BR>
              </TD>
	    </TR>
	    <TR>
	      <TD>
                &nbsp;
              </TD>
              <TD>
                <INPUT TYPE="SUBMIT" NAME="OK" VALUE="Speichern">
              </TD>
	    </TR>
	  </TABLE>
         </FORM>

