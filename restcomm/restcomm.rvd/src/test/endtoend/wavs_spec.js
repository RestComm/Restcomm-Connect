var frisby = require('frisby');
var config = require('./common.js').getConfig();

// apply customizations
config.projectName = "test_wavsProject";

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
		frisby.create('Create a voice project')
			.put( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/?kind=voice')
			.expectStatus(200)
			.after(function (err,res,body) {
				frisby.create('Retrieve project wav list')
					.get( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName + '/wavs')
					.expectStatus(200)
					.expectJSONTypes( String ) // this does not work!
				.toss();
			})
			.after(function () {
				frisby.create('Remove project')
					.delete( config.baseURL + 'restcomm-rvd/services/projects/' + config.projectName )
					.expectStatus(200)
				.toss();								
			})
		.toss();		
		
	})
.toss();


