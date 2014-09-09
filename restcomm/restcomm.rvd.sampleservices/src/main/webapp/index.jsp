<html>
<body>
<h2>Sample services for RVD applications</h2>
<h3>userDetails.json</h3>
<h5>customerId parameter</h5>
<ul>
	<li>existing customers: 1,2 </li>
	<li>not found: > 2 or "" </li>
	<li>throws error for non integer customerId</li>
</ul>

<h3>echo.json</h3>
<p>Returns url parameters passed as a JSON object</p>
<h6>Request</h6>
http://localhost:8080/sample-services/echo.json?a=1&b=2
<h6>Response</h6>
{"b":"2","a":"1"}


</body>
</html>
