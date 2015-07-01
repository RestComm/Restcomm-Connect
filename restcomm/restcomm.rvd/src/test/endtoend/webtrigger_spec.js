var frisby = require('frisby');
var config = require('./common.js').getConfig();

// apply customizations
config.projectName = "test_webTrigger";
config.toAddress = "client:bob";

frisby.create('Login RVD')
	.post( 
		config.baseURL + 'restcomm-rvd/services/auth/login', 
		{"username": config.username,"password": config.password},
		{json:true}
	)
	.expectStatus(200)
	.after(function (err, res, body) {
		var rvdCookie = res.headers['set-cookie'][0].split(';')[0];
		frisby.globalSetup({
		  request: {
			headers: { 
				'Content-Type': 'application/json;charset=utf-8',  
				'Cookie': rvdCookie
			}
		  }
		});		
		//console.log("Using rvdCookie: " + rvdCookie);
		
		// Create a project for testing upon
		frisby.create('Create WebTrigger test project')
			.put( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/?kind=voice')
			.expectStatus(200)
			.after(function () {
				frisby.create('Retrieve WebTrigger info  for project with no WebTrigger yet')
					.get( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/cc')
					.expectStatus(404)
					.after( function () {
						
						frisby.create('Save WebTrigger info')
							.post( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/cc',
								{"lanes":[{"startPoint":{"to":config.toAddress,"from":""}}]},
								{"json":true})
							.expectStatus(200)
							.after( function () {
								
								frisby.create('Retrieve WebTrigger info')
									.get( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/cc')
									.expectStatus(200)
									.expectJSON( "lanes", [ {startPoint: { to: config.toAddress}} ] )
									.after(function () {
										frisby.create('Remove WebTrigger test project')
											.delete( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName )
											.expectStatus(200)
										.toss();	
									})
								.toss();
								
							})
						.toss();			
					})
				.toss();
			})
		.toss();		
		
	})
.toss();


