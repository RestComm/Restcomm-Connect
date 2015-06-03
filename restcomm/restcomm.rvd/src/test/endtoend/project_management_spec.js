var frisby = require('frisby');

var config = {
	username: "administrator@company.com", // avoid using url encoded values here
	password: "tri$k0l0",
	projectName: "projectManagementTest",
	projectRenamed: "projectManagementTest2",
	rvdProjectVersion: "1.3"
}

var URL = 'http://192.168.1.39:8080/';
//var URL_AUTH = 'http://administrator%40company.com:RestComm@192.168.1.39:8080/';


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
		
		
		// Basic project management
		frisby.create('Create a voice project')
			.put( URL + 'restcomm-rvd/services/projects/' + config.projectName + '/?kind=voice')
			.expectStatus(200)
			.after(function (err,res,body) {
				
				frisby.create('Retrieve voice project')
					.get( URL + 'restcomm-rvd/services/projects/' + config.projectName )
					.expectStatus(200)
					.expectJSON( "header", { projectKind: 'voice', startNodeName: 'start', version: config.rvdProjectVersion, owner: config.username })
					.after(function () {
						frisby.create('Rename project')
							.put( URL + 'restcomm-rvd/services/projects/' + config.projectName + '/rename?newName=' + config.projectRenamed )
							.expectStatus(200)
							.after(function () {
								frisby.create('Remove project')
									.delete( URL + 'restcomm-rvd/services/projects/' + config.projectRenamed )
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


