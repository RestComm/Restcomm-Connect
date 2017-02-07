<%!
    String url= null;
    public void jspInit() {
        ServletConfig config = getServletConfig();
        url= config.getInitParameter("resetPasswordUrl");
    }
%>
<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-cache" );
   response.setDateHeader( "Expires", 0 );
   response.setHeader( "Content-Type", "application/json;charset=UTF-8");
%>{
    "resetPasswordUrl": "<%= url %>"
}