<%@ page language="java" session="false"%>
<jsp:useBean id="news" scope="request" class="de.infocrew.cms.CMS" />
<% 
  String prefix = request.getContextPath ();

  news.initialize (application, request);

  int nextPage = news.processRequest ();

%>
<jsp:include page="../../header.jsp" />
<%
  switch (nextPage) {
    case 1: 
      %>
      <jsp:include page="list.jsp" />
      <%
      break;
    case 2: 
      %>
      <jsp:include page="edit.jsp" />
      <%
      break;

  }
%>
<jsp:include page="../../footer.jsp" />

