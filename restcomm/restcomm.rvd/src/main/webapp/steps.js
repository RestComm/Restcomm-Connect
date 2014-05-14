angular.module('Rvd')
.service('stepRegistry', function() {
	this.lastStepId = 0;
	this.name = function () {
		return 'step' + (++this.lastStepId);
	};
	this.reset = function (newIndex) {
		if (!newIndex)
			this.lastStepId = 0;
		else
			this.lastStepId = newIndex;
	}
	this.current = function() {
		return this.lastStepId;
	}
})
.service('stepPacker', ['$injector', function($injector) {
	this.unpack = function(source) {
		var unpacked = $injector.invoke([source.kind+'Model', function(model){
			return model.from(source);
		}]);
		return unpacked;
	}
}])
.factory('stepPrototype', function () {
	return {
		test: function () {
			console.log('testing from stepPrototype: ' + this.kind);
		},
		pack: function () {
			console.log("stepPrototype:pack() - "  + this.name);
			var clone = angular.copy(this);
			return clone;
		},
		validate: function () {
			if (!this.iface)
			this.iface = {};
		}
	}
})
.factory('sayModel', ['stepPrototype', function SayModelFactory(stepPrototype) {
	function SayModel(name) {
		if (name)
			this.name = name;
		this.kind = 'say';
		this.label = 'say';
		this.title = 'say';
		this.phrase = '';
		this.voice = undefined;
		this.language = undefined;
		this.loop = undefined;
		this.iface = {};
	}
	
	SayModel.from = function(existing) {
		var model = new SayModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}
	
	SayModel.prototype = stepPrototype;
	
	return SayModel;
}])
.factory('playModel', ['stepPrototype', function PlayModelFactory(stepPrototype) {
	function PlayModel(name) {
		if (name)
			this.name = name;
		this.kind = 'play';
		this.label = 'play';
		this.title = 'play';
		this.loop = undefined;
		this.playType = 'local';
		this.local = {wavLocalFilename:''};
		this.remote = {wavUrl:''};
		this.iface = {};
		
		this.pack = PlayModel.pack;
		this.validate = PlayModel.validate;
	}
	
	PlayModel.from = function(existing) {
		var model = new PlayModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}
	
	// do not call this directly from the PlayModel  
	PlayModel.validate = function() {
		if (!this.iface)
			this.iface = {};
		if (this.playType == "local")
			this.remote = {wavUrl:''};
		else if (this.playType == "remote")
			this.local = {wavLocalFilename:''};
	}
	PlayModel.pack = function () {
			if (this.playType == "local")
				delete this.remote;
			else if (this.playType == "remote")
				delete this.local;
	}
	
	PlayModel.prototype = stepPrototype;
	
	return PlayModel;
}])
.factory('gatherModel', ['sayModel', 'stepPrototype', function GatherModelFactory(sayModel, stepPrototype) {
	function GatherModel(name) {
		if (name)
			this.name = name;
		this.kind = 'gather';
		this.label = 'gather';
		this.title = 'collect';
		this.action = undefined;
		this.method = 'GET';
		this.timeout = undefined;
		this.finishOnKey = undefined;
		this.numDigits = undefined;
		this.steps = [];
		this.validation = {messageStep: new sayModel(), pattern: "", iface:{userPattern:'', userPatternType:"One of"}};
		this.gatherType = "menu";
		this.menu = {mappings:[] }; /*{digits:1, next:"welcome.step1"}*/
		this.collectdigits = {collectVariable:'',next:'', scope:"module"};
		this.iface = {}	;
		
		this.pack = GatherModel.pack;
		this.validate = GatherModel.validate;
	}	
	
	GatherModel.from = function(existing) {
		var model = new GatherModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}
	
	GatherModel.validate = function(existing) {
		if (!existing.validation)
				existing.validation = {messageStep: new sayModel(), pattern: "", iface:{userPattern:'', userPatternType:"One of"}};
		if (!existing.validation.iface || angular.equals({},existing.validation.iface) )
			existing.validation.iface = {userPattern:existing.validation.pattern, userPatternType:"Regex"};
		if (!existing.menu)
			existing.menu = {mappings:[] };
		if (!existing.collectdigits)
			existing.collectdigits = {collectVariable:'',next:'', scope:"module"};
	}
	GatherModel.pack = function () {
		console.log("gatherModel:pack() - " + this.name);
		var clone = angular.copy(this);
		if (clone.gatherType == "menu")
			delete clone.collectdigits;
		else
		if (clone.gatherType == "collectdigits")
			delete clone.menu;
		return clone;
	}
	
	GatherModel.prototype = stepPrototype;
	
	return GatherModel;
}])
.factory('dialModel', ['stepPrototype', 'numberNounModel', 'clientNounModel', 'conferenceNounModel', 'sipuriNounModel', function DialModelFactory(stepPrototype, NumberNounModel, ClientNounModel, ConferenceNounModel, SipuriNounModel ) {
	function DialModel(name) {
		if (name)
			this.name = name;
		this.kind = 'dial';
		this.label = 'dial';
		this.title = 'dial';
		this.dialNouns = [];
		this.nextModule = undefined;
		this.action = undefined;
		this.method = undefined;
		this.timeout = undefined;
		this.timeLimit = undefined;
		this.callerId = undefined;
		this.record = undefined;
		this.iface = {};
	}
	
	DialModel.from = function(existing) {
		var model = new DialModel();
		angular.extend(model, existing);
		for (var i=0; i<existing.dialNouns.length; i++) {
			var noun = existing.dialNouns[i];
			if ( noun.dialType == 'number' )
				existing.dialNouns[i] = NumberNounModel.from(noun);
			else if ( noun.dialType == 'client' ) 
				existing.dialNouns[i] = ClientNounModel.from(noun);
			else if ( noun.dialType == 'conference' ) 
				existing.dialNouns[i] = ConferenceNounModel.from(noun);
			else if ( noun.dialType == 'sipuri' ) 
				existing.dialNouns[i] = SipuriNounModel.from(noun);
		}
		model.validate(model);
		return model;	
	}
	
	DialModel.prototype = stepPrototype;
	
	return DialModel;
}])
.factory('numberNounModel',[function NumberNounModelFactory() {
	function NumberNounModel() {
		this.dialType = 'number';
		this.destination = '';
		this.sendDigits = undefined;
		this.beforeConnectModule = undefined;
	}
	NumberNounModel.from = function(existing) {
		var model = new NumberNounModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}
	return NumberNounModel;
}])
.factory('clientNounModel',[function ClientNounModelFactory() {
	function ClientNounModel() {
		this.dialType = 'client';
		this.destination = '';
	}
	ClientNounModel.from = function(existing) {
		var model = new ClientNounModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}
	return ClientNounModel;
}])
.factory('conferenceNounModel',[function ConferenceNounModelFactory() {
	function ConferenceNounModel() {
		this.dialType = 'conference';
		this.destination = '';
		this.nextModule = undefined;
		this.muted = undefined;
		this.beep = undefined;
		this.startConferenceOnEnter = undefined;
		this.endConferenceOnExit = undefined;
		this.waitUrl = undefined;
		this.waitModule = undefined;
		this.waitMethod = undefined;
		this.maxParticipants = undefined;
	}
	ConferenceNounModel.from = function(existing) {
		var model = new ConferenceNounModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}	
	
	return ConferenceNounModel;
}])
.factory('sipuriNounModel',[function SipuriNounModelFactory() {
	function SipuriNounModel() {
		this.dialType = 'sipuri';
		this.destination = '';
	}
	SipuriNounModel.from = function(existing) {
		var model = new SipuriNounModel();
		angular.extend(model, existing);
		model.validate(model);
		return model;	
	}	
	return SipuriNounModel;
}])

;
