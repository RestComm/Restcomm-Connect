<%
   response.setHeader( "Pragma", "no-cache" );
   response.setHeader( "Cache-Control", "no-cache" );
   response.setDateHeader( "Expires", 0 );
   response.setHeader( "Content-Type", "application/json;charset=UTF-8");
%>{
    "resetPasswordUrl": "https://accounts.restcomm.com/resetPassword.jsp"
}