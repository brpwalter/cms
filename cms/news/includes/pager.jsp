<%@ page language="java" 
	 session="false"
%>
<jsp:useBean id="news" scope="request" class="de.infocrew.cms.CMS" />
<%-- 
  paging function 

  needs the following parameters:
    url: this is used to construct the page links
    recordCount: number of records in the result
    numberPerPage: number of records per page

  this one is taken from the request:
    s: the current page number (counting starts at 0)

--%>
<% 
    int recordCount = news.getRecordCount ();
    int numberPerPage = news.getNumberPerPage ();
    int s = news.getStartPage ();

    String url = news.noNull (request.getParameter ("url"));
    if (0 <= url.indexOf ("?"))
      url += "&s=";
    else
      url += "?s=";

    if (recordCount > numberPerPage) { //paging needed

      %>Seite: <b><%

      if (s > 0) { %><a href="<%= url + (s-1) %>">&lt;</A>&nbsp;<% } 

      for (int pg=0; pg <= (recordCount - 1) / numberPerPage; pg++) {
        if (pg > 0) { %>&nbsp;<% }

        if (pg == s) { %><%= pg + 1 %><% }
        else { %><a href="<%= url + pg %>"><%= pg+1 %></a><% }

      }

      if (s != (recordCount - 1) / numberPerPage) { %>&nbsp;<a href="<%= url+(s+1) %>">&gt;</a><% } %></b><%


    }
%>
