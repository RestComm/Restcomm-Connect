var frisby = require('frisby');

var config = {
	username: "administrator@company.com", // avoid using url encoded values here
	password: "tri$k0l0",
	projectName: "wavTestProject",
}

var URL = 'http://192.168.1.39:8080/';

http://192.168.1.39:8080/restcomm-rvd/services/projects/test/wavs


frisby.create('Login RVD')
	.post( 
		URL + 'restcomm-rvd/services/auth/login', 
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
		console.log("Using rvdCookie: " + rvdCookie);
		
		// Basic a project for testing upon
		frisby.create('Create a voice project')
			.put( URL + 'restcomm-rvd/services/projects/' + config.projectName + '/?kind=voice')
			.expectStatus(200)
			.after(function (err,res,body) {
				frisby.create('Retrieve project wav list')
					.get( URL + 'restcomm-rvd/services/projects/' + config.projectName + '/wavs')
					.expectStatus(200)
					.expectJSONTypes( String ) // this does not work!
				.toss();
			})
		.toss();		
		
	})
.toss();


