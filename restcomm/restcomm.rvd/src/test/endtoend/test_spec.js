var frisby = require('frisby');

var URL = 'http://192.168.1.39:8080/';
var URL_AUTH = 'http://administrator%40company.com:RestComm@192.168.1.39:8080/';
var projectName = "ggg1";

frisby.globalSetup({ // globalSetup is for ALL requests
  request: {
    headers: { 'Content-Type': 'application/json;charset=utf-8' }
  }
});

frisby.create('Login RVD')
	.post( 
		URL + 'restcomm-rvd/services/auth/login', 
		{"username":"administrator@company.com","password":"RestComm"},
		{json:true}
	)
	.expectStatus(200)
	.after(function (err, res, body) {
		var rvdCookie = res.headers['set-cookie'][0].split(';')[0];
		console.log("Using rvdCookie: " + rvdCookie);
		
		// Basic project management
		frisby.create('Create new RVD project')
			.put( URL + 'restcomm-rvd/services/projects/' + projectName + '/?kind=voice')
			.addHeader('Cookie', rvdCookie)
			.expectStatus(200)
		.toss();		
		frisby.create('Fail creating duplicate RVD project')
			.put( URL + 'restcomm-rvd/services/projects/' + projectName + '/?kind=voice')
			.addHeader('Cookie', rvdCookie)
			.expectStatus(409)
		.toss();		
		frisby.create('Remove RVD project')
			.delete( URL + 'restcomm-rvd/services/projects/' + projectName )
			.addHeader('Cookie', rvdCookie)
			.expectStatus(200)
		.toss();	
		frisby.create('Fail removing non-existing RVD project')
			.delete( URL + 'restcomm-rvd/services/projects/' + projectName )
			.addHeader('Cookie', rvdCookie)
			.expectStatus(404)
		.toss();
		
		
		frisby.create('Create new RVD project')
			.put( URL + 'restcomm-rvd/services/projects/' + projectName + '/?kind=voice')
			.addHeader('Cookie', rvdCookie)
			.expectStatus(200)
		.toss();
		frisby.create('Retrieve default RVD project')
			.get( URL + 'restcomm-rvd/services/projects/' + projectName )
			.addHeader('Cookie', rvdCookie)
			.expectStatus(200)
			.expectJSON({
				"lastStepId": 1,
				"nodes": [
					{
						"name": "start",
						"label": "Welcome",
						"kind": "voice",
						"steps": [
							{
								"phrase": "Welcome to Telestax Restcom Visual Designer Demo",
								"kind": "say",
								"label": "say",
								"title": "say",
								"name": "step1"
							}
						]
					}
				],
				"lastNodeId": 0,
				"header": {
					"projectKind": "voice",
					"startNodeName": "start",
					"version": "1.3",
					"owner": "administrator@company.com"
				}
			})
			//.afterJSON(function (json) {
			//	console.log(json);
			//	// add another step
			//	json.nodes[0].
			//})
		.toss();
		frisby.create('Remove RVD project')
			.delete( URL + 'restcomm-rvd/services/projects/' + projectName )
			.addHeader('Cookie', rvdCookie)
			.expectStatus(200)
		.toss();		

		
	})
.toss();


