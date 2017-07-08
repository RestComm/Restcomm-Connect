<%@ page import="java.util.Map, java.util.Set"%>
<%
    Set<Map.Entry<String, String[]>> parametersSet = request.getParameterMap().entrySet();
    for(Map.Entry<String, String[]> entry : parametersSet) {
        System.out.println("***********   sip-url-screening-test.jsp  : " + entry.getKey() + " = " + entry.getValue()[0]);
    }
%>
<?xml version="1.0" encoding="UTF-8"?>
<Response><Hangup/></Response>
