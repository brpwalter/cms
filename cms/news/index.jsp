<%@ page language="java" session="false"%>
<% 
  String prefix = request.getContextPath ();
%>
<jsp:include page="../header.jsp"/>
<h1>Content Management</h1>
<p>
Dies ist ein einfaches Content Management System mit JSP-Technik.
</p>
<p>
Es gibt zwei Ausgabeseiten und ein Redaktionssystem.
</p>
<jsp:include page="../footer.jsp"/>
