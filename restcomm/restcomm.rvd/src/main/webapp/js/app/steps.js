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
		if (source.kind == 'control') // TODO do the same for the rest of the steps once directives are created
		    return source;
		var unpacked = $injector.invoke([source.kind+'Model', function(model){
			var newStep = new model().init(source);
			return newStep;
		}]);
		return unpacked;
	}
}])
.factory('rvdModel', function () {
	function RvdModel() {
		this.pack = function () {
			var clone = angular.copy(this);
			return clone;
		}
		this.validate = function () {
			if (!this.iface)
			this.iface = {};
		}
		this.init = function (from) {
			angular.extend(this,from);
			return this;
		}
	}
	return RvdModel;
})
.factory('sayModel', ['rvdModel', function SayModelFactory(rvdModel) {
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

	SayModel.prototype = new rvdModel();
	SayModel.prototype.constructor = SayModel;
	// Add Say methods here
	// SayModel.prototype.method1 - function ()
	// ...
	
	return SayModel;
}])
.factory('logModel', ['rvdModel', function LogModelFactory(rvdModel) {
	function LogModel(name) {
		if (name)
			this.name = name;
		this.kind = 'log';
		this.label = 'log';
		this.title = 'log';
		this.message = '';
		this.iface = {};
	}

	LogModel.prototype = new rvdModel();
	LogModel.prototype.constructor = LogModel;
	// Add Say methods here
	// SayModel.prototype.method1 - function ()
	// ...
	
	return LogModel;
}])
.factory('playModel', ['rvdModel', function PlayModelFactory(rvdModel) {
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
	}
	PlayModel.prototype = new rvdModel();
	PlayModel.prototype.constructor = PlayModel; 
	PlayModel.prototype.validate = function() {
		if (!this.iface)
			this.iface = {};
		if (this.playType == "local")
			this.remote = {wavUrl:''};
		else if (this.playType == "remote")
			this.local = {wavLocalFilename:''};
	}
	PlayModel.prototype.pack = function () {
		var clone = angular.copy(this);
		if (clone.playType == "local")
			delete clone.remote;
		else if (clone.playType == "remote")
			delete clone.local;
		return clone;
	}
	
	return PlayModel;
}])
.factory('gatherModel', ['rvdModel','sayModel','playModel', function GatherModelFactory(rvdModel, sayModel, playModel) {
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
		this.validation = {userPattern: "", regexPattern: undefined};
		this.invalidMessage = new sayModel();
		this.gatherType = "menu";
		this.menu = {mappings:[] }; //{digits:1, next:"welcome.step1"}
		this.collectdigits = {collectVariable:'',next:'', scope:"module"};
		this.iface = {}	;
	}	
	GatherModel.prototype = new rvdModel();
	GatherModel.prototype.constructor = GatherModel;
	GatherModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.steps.length; i++) {
			var step;
			if (from.steps[i].kind == 'say')
				step = new sayModel().init(from.steps[i]);
			else
			if (from.steps[i].kind == 'play')
				step = new playModel().init(from.steps[i]);
			this.steps[i] = step;
		}
		this.validate();
		return this;
	}
	GatherModel.prototype.validate = function() {
		if (!this.validation || (!this.validation.userPattern && !this.validation.regexPattern))
			this.validation = {userPattern: "", regexPattern: undefined};
		if (!this.invalidMessage)
			this.invalidMessage = new sayModel();
		if (!this.menu)
			this.menu = {mappings:[] };
		if (!this.collectdigits)
			this.collectdigits = {collectVariable:'',next:'', scope:"module"};
	}
	GatherModel.prototype.pack = function () {
		//console.log("gatherModel:pack() - " + this.name);
		var clone = angular.copy(this);
		if (clone.gatherType == "menu")
			delete clone.collectdigits;
		else
		if (clone.gatherType == "collectdigits")
			delete clone.menu;
		if (!clone.validation.userPattern && !clone.validation.regexPattern)
			delete clone.validation;
		if (clone.invalidMessage.phrase == "")
			delete clone.invalidMessage;
		for (var i=0; i<clone.steps.length; i++) {
			var step;
			if (clone.steps[i].kind == 'say')
				step = clone.steps[i].pack();
			else
			if (clone.steps[i].kind == 'play')
				step = clone.steps[i].pack();
			clone.steps[i] = step;
		}		
		return clone;
	}
	return GatherModel;
}])
.factory('dialModel', ['rvdModel', 'numberNounModel', 'clientNounModel', 'conferenceNounModel', 'sipuriNounModel', function DialModelFactory(rvdModel, NumberNounModel, ClientNounModel, ConferenceNounModel, SipuriNounModel ) {
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
	DialModel.prototype = new rvdModel();
	DialModel.prototype.constructor = DialModel;	
	DialModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.dialNouns.length; i++) {
			var noun = from.dialNouns[i];
			if ( noun.dialType == 'number' )
				this.dialNouns[i] = new NumberNounModel().init(noun);
			else if ( noun.dialType == 'client' ) 
				this.dialNouns[i] = new ClientNounModel().init(noun);
			else if ( noun.dialType == 'conference' ) 
				this.dialNouns[i] = new ConferenceNounModel().init(noun);
			else if ( noun.dialType == 'sipuri' ) 
				this.dialNouns[i] = new SipuriNounModel().init(noun);
		}
		this.validate();
		return this;
	}
	
	return DialModel;
}])
.factory('numberNounModel',['rvdModel', function NumberNounModelFactory(rvdModel) {
	function NumberNounModel() {
		this.dialType = 'number';
		this.destination = '';
		this.sendDigits = undefined;
		this.beforeConnectModule = undefined;
	}
	NumberNounModel.prototype = new rvdModel();
	NumberNounModel.prototype.contructor = NumberNounModel;
	return NumberNounModel;
}])
.factory('clientNounModel',['rvdModel', function ClientNounModelFactory(rvdModel) {
	function ClientNounModel() {
		this.dialType = 'client';
		this.destination = '';
		this.beforeConnectModule = undefined;
	}
	ClientNounModel.prototype = new rvdModel();
	ClientNounModel.prototype.contructor = ClientNounModel;
	return ClientNounModel;
}])
.factory('conferenceNounModel',['rvdModel', function ConferenceNounModelFactory(rvdModel) {
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
	ConferenceNounModel.prototype = new rvdModel();
	ConferenceNounModel.prototype.contructor = ConferenceNounModel;
	return ConferenceNounModel;
}])
.factory('sipuriNounModel',['rvdModel', function SipuriNounModelFactory(rvdModel) {
	function SipuriNounModel() {
		this.dialType = 'sipuri';
		this.destination = '';
	}
	SipuriNounModel.prototype = new rvdModel();
	SipuriNounModel.prototype.contructor = SipuriNounModel;
	return SipuriNounModel;
}])
.factory('redirectModel', ['rvdModel', function RedirectModelFactory(rvdModel) {
	function RedirectModel(name) {
		if (name)
			this.name = name;
		this.kind = 'redirect';
		this.label = 'redirect';
		this.title = 'redirect';
		this.url  = null;
		this.method = null;
		this.iface = {};
	}
	RedirectModel.prototype = new rvdModel();
	RedirectModel.prototype.contructor = RedirectModel;
	return RedirectModel;
}])
.factory('hungupModel', ['rvdModel', function HungupModelFactory(rvdModel) {
	function HungupModel(name) {
		if (name)
			this.name = name;
		this.kind = 'hungup';
		this.label = 'hang up';
		this.title = 'hang up';
		this.iface = {};
	}
	HungupModel.prototype = new rvdModel();
	HungupModel.prototype.contructor = HungupModel;
	return HungupModel;
}])
.value('accessOperationKinds',['object','array','value'])
.value('objectActions', ['propertyNamed'])
.value('arrayActions', ['itemAtPosition'])

.factory('esValueExtractor',['rvdModel',function (rvdModel) {
	var accessOperationProtos = {
		object:{kind:'object',fixed:false, terminal:false},
		array:{kind:'array',fixed:false, terminal:false},
		value:{kind:'value',fixed:false, terminal:true}
	};
	function EsValueExtractor() {
		this.accessOperations = [];
		this.lastOperation = angular.copy( accessOperationProtos.object );
	}
	EsValueExtractor.prototype = new rvdModel();
	EsValueExtractor.prototype.constructor = EsValueExtractor;
	EsValueExtractor.prototype.init = function(from) {
		angular.extend(this, from);
		if (!from.lastOperation)  // aimed to replace  undefined values with null
			this.lastOperation = null;
		return this;
	}
	EsValueExtractor.prototype.addOperation = function () {
		//console.log("adding operation");
		this.lastOperation.fixed = true;
		this.lastOperation.expression = this.operationExpression( this.lastOperation );
		this.accessOperations.push(this.lastOperation);
		this.lastOperation = angular.copy(accessOperationProtos.object)
	}
	EsValueExtractor.prototype.operationExpression = function (operation) {
		switch (operation.kind) {
		case 'object':
			switch (operation.action) {
			case 'propertyNamed':
				return "."+operation.property;
			}
		break;
		case 'array':
			switch (operation.action) {
			case 'itemAtPosition':
				return "[" + operation.position + "]";
			}
		break;
		case 'value':
			return " value";
		break;	
		}
		return "UNKNOWN";
	}
	EsValueExtractor.prototype.extractorModelExpression = function () {
		var expr = '';
		for ( var i=0; i < this.accessOperations.length; i++ ) {
			expr += this.operationExpression(this.accessOperations[i]);
		} 
		return expr;
	}
	EsValueExtractor.prototype.isTerminal = function (kind) {
		if (kind == null)
			return false;
		return accessOperationProtos[kind].terminal;
	}
	EsValueExtractor.prototype.doneAddingOperations = function () {
		this.addOperation();
		this.lastOperation = null;
	}
	EsValueExtractor.prototype.popOperation = function () { // removes last operation
		if ( this.accessOperations.length > 0 ) {
			this.lastOperation = this.accessOperations.pop();
			this.lastOperation.fixed = false;
		}
	}	
	EsValueExtractor.prototype.setLastOperationKind = function (kind) {
		this.lastOperation = angular.copy(accessOperationProtos[kind]);
		// if the operation is 'value' auto 'press Done' automatically
		if (this.lastOperation.kind == 'value')
		    this.doneAddingOperations();
	}
	return EsValueExtractor;
}])

.factory('esAssignment',['rvdModel','esValueExtractor',function (rvdModel,esValueExtractor) {
	function EsAssignment() {
		this.moduleNameScope = null;
		this.destVariable = '';
		this.scope = 'module';
		this.valueExtractor = new esValueExtractor(); 
	}
	EsAssignment.prototype = new rvdModel();
	EsAssignment.prototype.constructor = EsAssignment;
	EsAssignment.prototype.init = function(from) {
		angular.extend(this, from);
		this.valueExtractor = new esValueExtractor().init(from.valueExtractor);
		return this;
	}
	return EsAssignment;
}]) 
.factory('externalServiceModel', ['rvdModel','esAssignment','esValueExtractor', function ExternalServiceModelFactory(rvdModel,esAssignment,esValueExtractor) {
	function ExternalServiceModel(name) {
		if (name)
			this.name = name;
		this.kind = 'externalService';
		this.label = 'externalService';
		this.title = 'external service';
		this.url = '';
		this.method = undefined;
		this.contentType = undefined; 
		this.requestBody = undefined;
		this.populatePostBodyFromParams = undefined;
		this.username = undefined;
		this.password = undefined;
		this.urlParams = [];
		this.assignments = [];
		this.next = '';
		this.doRouting = false;
		this.nextType = 'fixed';
		this.nextValueExtractor = new esValueExtractor();
		this.routeMappings = []; // [{value:undefined,next:undefined}]
		this.defaultNext = undefined;
		this.exceptionNext = undefined;
		this.iface = {};		
	}
	ExternalServiceModel.prototype = new rvdModel();
	ExternalServiceModel.prototype.contructor = ExternalServiceModel;
	ExternalServiceModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.assignments.length; i++) {
			var assignment = new esAssignment().init(from.assignments[i]);
			this.assignments[i] = assignment;
		}
		this.nextValueExtractor = new esValueExtractor().init(from.nextValueExtractor);
		this.validate();
		return this;
	}
	ExternalServiceModel.prototype.pack = function () {
		var clone = angular.copy(this);
		if (clone.nextType != "mapped")
			delete clone.routeMappings
		return clone;
	}
	ExternalServiceModel.prototype.validate = function() {
		if (!this.routeMappings)
			this.routeMappings = [];
	}	
	ExternalServiceModel.prototype.addAssignment = function () {
		this.assignments.push(new esAssignment());
	}
	ExternalServiceModel.prototype.addRouteMapping = function () {
		this.routeMappings.push({value:undefined,next:undefined});
	}
	ExternalServiceModel.prototype.removeRouteMapping = function (mapping) {
		this.routeMappings.splice(this.routeMappings.indexOf(mapping), 1);
	}	
	return ExternalServiceModel;
}])
.factory('rejectModel', ['rvdModel', function RejectModelFactory(rvdModel) {
	function RejectModel(name) {
		if (name)
			this.name = name;
		this.kind = 'reject';
		this.label = 'reject';
		this.title = 'reject';
		this.reason = undefined;
		this.iface = {};
	}
	RejectModel.prototype = new rvdModel();
	RejectModel.prototype.contructor = RejectModel;
	return RejectModel;
}])
.factory('pauseModel', ['rvdModel', function PauseModelFactory(rvdModel) {
	function PauseModel(name) {
		if (name)
			this.name = name;
		this.kind = 'pause';
		this.label = 'pause';
		this.title = 'pause';
		this.length = undefined;
		this.iface = {};
	}
	PauseModel.prototype = new rvdModel();
	PauseModel.prototype.contructor = PauseModel;
	return PauseModel;
}])
.factory('smsModel', ['rvdModel', function SmsModelFactory(rvdModel) {
	function SmsModel(name) {
		if (name)
			this.name = name;
		this.kind = 'sms';
		this.label = 'sms';
		this.title = 'sms';
		this.text = '';
		this.to = undefined;
		this.from = undefined;
		this.statusCallback = undefined;
		this.next = null;
		this.iface = {};
	}
	SmsModel.prototype = new rvdModel();
	SmsModel.prototype.contructor = SmsModel;
	return SmsModel;
}])
.factory('faxModel', ['rvdModel', function FaxModelFactory(rvdModel) {
	function FaxModel(name) {
		if (name)
			this.name = name;
		this.kind = 'fax';
		this.label = 'fax';
		this.title = 'fax';
		this.text = '';
		this.to = undefined;
		this.from = undefined;
		this.statusCallback = undefined;
		this.next = null;
		this.iface = {};
	}
	FaxModel.prototype = new rvdModel();
	FaxModel.prototype.contructor = FaxModel;
	return FaxModel;
}])
.factory('emailModel', ['rvdModel', function EmailModelFactory(rvdModel) {
	function EmailModel(name) {
		if (name)
			this.name = name;
		this.kind = 'email';
		this.label = 'email';
		this.title = 'email';
		this.text = '';
		this.to = undefined;
		this.from = undefined;
		this.cc = undefined;
  		this.bcc = undefined;
		this.subject = undefined;
		this.statusCallback = undefined;
		this.next = null;
		this.iface = {};
	}
	EmailModel.prototype = new rvdModel();
	EmailModel.prototype.contructor = EmailModel;
	return EmailModel;
}])
.factory('recordModel', ['rvdModel', function RecordModelFactory(rvdModel) {
	function RecordModel(name) {
		if (name) {
			this.name = name;
			this.maxLength = 20;
		} else {
		    this.maxLength = undefined;
		}
		this.kind = 'record';
		this.label = 'record';
		this.title = 'record';
		this.next = null;
		this.method = 'GET';
		this.timeout = undefined;
		this.finishOnKey = undefined;
		//this.maxLength = undefined;
		this.transcribe = undefined;
		this.transcribeCallback = undefined;
		this.playBeep = undefined;
		this.iface = {};
	}
	RecordModel.prototype = new rvdModel();
	RecordModel.prototype.contructor = RecordModel;
	return RecordModel;
}])
.factory('ussdSayModel', ['rvdModel', function UssdSayModelFactory(rvdModel) {
	function UssdSayModel(name) {
		if (name)
			this.name = name;
		this.kind = 'ussdSay';
		this.label = 'USSD Message';
		this.title = 'USSD Message';
		this.text = '';
		this.language = null;
		this.iface = {};
	}
	UssdSayModel.prototype = new rvdModel();
	UssdSayModel.prototype.constructor = UssdSayModel;	
	return UssdSayModel;
}])
.factory('ussdCollectModel', ['rvdModel','ussdSayNestedModel', function UssdCollectModelFactory(rvdModel,ussdSayNestedModel) {
	function UssdCollectModel(name) {
		if (name)
			this.name = name;
		this.kind = 'ussdCollect';
		this.label = 'USSD Collect';
		this.title = 'USSD Collect';
		this.gatherType = "menu";
		this.menu = {mappings:[]};
		this.collectdigits = {collectVariable:null,next:'',scope:"module"};
		this.text = '';
		this.language = null;
		this.messages = [];
		this.iface = {};
	}
	UssdCollectModel.prototype = new rvdModel();
	UssdCollectModel.prototype.constructor = UssdCollectModel;	
	UssdCollectModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var i=0; i<from.messages.length; i++) {
			var message = new ussdSayNestedModel().init(from.messages[i]);
			this.messages[i] = message;
		}
		this.validate();
		return this;
	}
	UssdCollectModel.prototype.validate = function() {
		//if (!this.validation)
		//		this.validation = {messageStep: new sayModel(), pattern: "", iface:{userPattern:'', userPatternType:"One of"}};
		//if (!this.validation.iface || angular.equals({},this.validation.iface) )
		//	this.validation.iface = {userPattern:this.validation.pattern, userPatternType:"Regex"};

		if (!this.menu)
			this.menu = {mappings:[] };
		if (!this.collectdigits)
			this.collectdigits = {collectVariable:null,next:'', scope:"module"};
	}
	UssdCollectModel.prototype.pack = function () {
		//console.log("ussdCollectModel:pack() - " + this.name);
		var clone = angular.copy(this);
		if (clone.gatherType == "menu")
			delete clone.collectdigits;
		else
		if (clone.gatherType == "collectdigits")
			delete clone.menu;
		return clone;
	}	
	return UssdCollectModel;
}])
.factory('ussdSayNestedModel', ['rvdModel', function UssdSayNestedModelFactory(rvdModel) {
	function UssdSayNestedModel() {
		this.text = '';
	}
	UssdSayNestedModel.prototype = new rvdModel();
	UssdSayNestedModel.prototype.constructor = UssdSayNestedModel;	
	return UssdSayNestedModel;
}])
.factory('ussdLanguageModel', ['rvdModel', function UssdLanguageModelFactory(rvdModel) {
	function UssdLanguageModel(name) {
		if (name)
			this.name = name;
		this.kind = 'ussdLanguage';
		this.label = 'Language';
		this.title = 'Language';
		this.language = null;
		this.iface = {};
	}
	UssdLanguageModel.prototype = new rvdModel();
	UssdLanguageModel.prototype.constructor = UssdLanguageModel;	
	return UssdLanguageModel;
}])
;
