angular.module('Rvd').service('ModelBuilder', ['$injector', function ($injector) {
	var modelBuilder = {};
	modelBuilder.build = function (modelName) {
		var builtModel = $injector.invoke([modelName, function(model) {
			var newInstance = new model;
			return newInstance;
		}]);
		return builtModel;
	}
	
	return modelBuilder;
}]);

/*
angular.module('Rvd').factory('ExceptionHandlingInfo', ['rvdModel', function(rvdModel) {
	function ExceptionHandlingInfo() {
		this.exceptionMappings = [];
		this.defaultNext = undefined;
	}
	ExceptionHandlingInfo.prototype = new rvdModel();
	ExceptionHandlingInfo.prototype.constructor = ExceptionHandlingInfo;
	ExceptionHandlingInfo.prototype.pack = function () {
		if ( this.defaultNext === undefined && this.exceptionMappings.length == 0 )
			return undefined;
		else
			return angular.copy(this);
	}
 
	return ExceptionHandlingInfo;
}]);
*/

angular.module('Rvd').factory('CcInfo', ['rvdModel', function(rvdModel) {
	function CcInfo() {
		// maybe review these undefined properties and change them to ""
		//this.apiServer = {username:undefined,pass:undefined};
		this.lanes = [{startPoint: {rcmlUrl:undefined, to:undefined, from:undefined}}]; 
	}
	CcInfo.prototype = new rvdModel();
	CcInfo.prototype.constructor = CcInfo;
	return CcInfo;
}]);

angular.module('Rvd').factory('nodeModel', ['rvdModel', 'nodeRegistry', 'stepPacker', function (rvdModel, nodeRegistry, stepPacker) {
	function NodeModel() {
			NodeModel.NodeModel(this);
	}	
	NodeModel.prototype = new rvdModel();
	NodeModel.prototype.constructor = NodeModel;
	NodeModel.prototype.setName = function (newname) {
		this.name = newname;
	}
	NodeModel.prototype.init = function(from) {
		angular.extend(this, from);
		for (var j=0; j<this.steps.length; j++) {
			var step = stepPacker.unpack(this.steps[j]);
			this.steps[j] = step;
		}
		return this;
	}
	NodeModel.NodeModel = function (o) {
		o.name = undefined;
		o.label = 'Untitled module';
		o.steps = [];
		o.iface = {edited:false,editLabel:false};
	}
	return NodeModel;
}]);

angular.module('Rvd').factory('voiceNodeModel', ['nodeModel', function (nodeModel) {
	function VoiceNodeModel() {
		nodeModel.NodeModel(this);
		this.kind = "voice";
			
	}
	VoiceNodeModel.prototype = new nodeModel();
	VoiceNodeModel.prototype.constructor = VoiceNodeModel;
	
	return VoiceNodeModel;
}]);

angular.module('Rvd').factory('smsNodeModel', ['nodeModel', function (nodeModel) {
	function SmsNodeModel() {
		nodeModel.NodeModel(this);
		this.kind = "sms";
			
	}
	SmsNodeModel.prototype = new nodeModel();
	SmsNodeModel.prototype.constructor = SmsNodeModel;
	
	return SmsNodeModel;
}]);

angular.module('Rvd').factory('ussdNodeModel', ['nodeModel', function (nodeModel) {
	function UssdNodeModel() {
		nodeModel.NodeModel(this);
		this.kind = "ussd";
			
	}
	UssdNodeModel.prototype = new nodeModel();
	UssdNodeModel.prototype.constructor = UssdNodeModel;
	
	return UssdNodeModel;
}]);
