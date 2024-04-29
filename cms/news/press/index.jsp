<%@ page language="java" session="false"%>
<jsp:useBean id="news" scope="request" class="de.infocrew.cms.CMS" />
<% 
  String prefix = request.getContextPath ();

  news.initialize (application, request);

%>
<jsp:include page="../../header.jsp" />
<%
  if (news.queryCMS (2, true, 0)) {
%>
    <TABLE BORDER=0>
      <TR>
        <TD COLSPAN=2>
   	  <H1>Presseinfos</H1>       
        </TD>
      </TR>
<%
    do {
%>
      <TR BGCOLOR="silver">
        <TD>
          <H4><%= news.getHeadline () %></H4>
        </TD>
        <TD ALIGN="RIGHT">
          <H4><%= news.getNews_date ()%></H4>
        </TD>
      </TR>
      <TR>
        <TD COLSPAN=2>
          <TABLE BORDER=0>
            <TR>
              <TD VALIGN="TOP" WIDTH="100%"><%= news.formatText (news.getContent ()) %></TD>
<% if (news.hasImage ()) { %>
              <TD ALIGN="RIGHT">
                <IMG SRC="<%=prefix%>/blob/image/<%=news.getId ()%>" WIDTH=300><BR>
                <SMALL><%= news.getImagetext () %></SMALL> 
              </TD>
<% } %>
            </TR>
          </TABLE>
<% if (news.hasDownload ()) { %>
          <P>
            <A HREF="<%=request.getContextPath ()%>/blob/download/<%=news.getId ()%>/<%=news.getDownloadname ()%>"><%= news.getDownloadtext () %></A>
          </P>
<% } %>

        </TD>
      </TR>


<%
    } while (news.nextRecord ());
%>
    </TABLE>
<%
  }
%>
<jsp:include page="../../footer.jsp" />

