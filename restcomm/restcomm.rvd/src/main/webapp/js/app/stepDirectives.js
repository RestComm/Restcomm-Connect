angular.module('Rvd')
.directive('gatherStep', function (gatherModel) {
	return {
			restrict: 'A',
			link: function (scope, element, attrs) {
				if (scope.step) {
					//gatherModel.validate(scope.step);
				}
				else
					scope.step = new gatherModel();
				
				
				function getEffectiveValidationType() {
					if ( scope.step.validation.userPattern != undefined )
						return "One of";
					else
						return "Regex"
				}
				scope.getEffectiveValidationType = getEffectiveValidationType;
				
				function setValidationTypeOneOf() {
					if ( getEffectiveValidationType() != "One of" ) {
						scope.step.validation.userPattern = "";
						scope.step.validation.regexPattern = undefined;
					}
				}
				scope.setValidationTypeOneOf = setValidationTypeOneOf;
				
				function setValidationTypeRegex() {
					if ( getEffectiveValidationType() != "Regex" ) {
						scope.step.validation.userPattern = undefined;
						scope.step.validation.regexPattern = "";
					}
				}		
				scope.setValidationTypeRegex = 	setValidationTypeRegex;
				
		}
	}
})
.directive('controlStep', function (nodeRegistry) {
    return {
        restict: 'E',
        templateUrl: "templates/directive/controlStep.html",
        scope: {
            stepModel: '=model'
        },
        link: function (scope) {

            // TODO initialize conditionLastId from existing conditions (set it to the max)
            // construction for conditions using a locally unique identifier
            var conditionLastId = 0;
            function newCondition() {
                return {
                   name: "C" + (++conditionLastId),
                   operator : "equals",
                   operand1 : "",
                   operand2 : ""
               }
            }
            scope.conditionJoiner = "all"; // all / any

            var actionLastId = 0;
            function newAction() {
                return {
                   name: "A" + (++actionLastId),
                   kind : "continueTo" // continueTo / assign
               }
            }

            console.log("inside controlStep link()");
            console.log(scope.stepModel);
            // initialize control step members
            var stepModel = scope.stepModel;
            stepModel.conditions = stepModel.conditions || [newCondition()];
            stepModel.actions = stepModel.actions || [];
            if (!stepModel.conditionExpression)
                rebuildConditionExpression();

            scope.addCondition = function () {
                stepModel.conditions.push(newCondition());
                rebuildConditionExpression();
            }
            scope.removeCondition = function (conditionName) {
                for (var i=0; i<stepModel.conditions.length; i++) {
                    if (stepModel.conditions[i].name = conditionName) {
                        stepModel.conditions.splice(i,1);
                        rebuildConditionExpression();
                        return;
                    }
                }
            }
            scope.addAction = function () {
                stepModel.actions.push(newAction());
            }
            scope.removeAction = function (actionName) {
                for (var i=0; i<stepModel.actions.length; i++) {
                    if (stepModel.actions[i].name = actionName) {
                        stepModel.actions.splice(i,1);
                        return;
                    }
                }
            }
            function rebuildConditionExpression() {
                var conditions = stepModel.conditions;
                var joiner = scope.conditionJoiner;
                var expression;

                if (!conditions || conditions.length == 0)
                    expression = undefined; // do nothing - 'expression; stays undefined
                else {
                    var names = [];
                    for (var i=0; i<conditions.length;i++)
                        names.push(conditions[i].name);
                    if (joiner == "all")
                        expression = names.join(" AND ")
                    if (joiner == "any")
                        expression = names.join(" OR ");
                }

                stepModel.conditionExpression = expression;
            }
            scope.rebuildConditionExpression = rebuildConditionExpression;

            scope.getAllTargets = nodeRegistry.getNodes;
        },
        controller: function ($scope) {
            console.log("inside controlStep directive");
            console.log($scope.stepModel);
        }
    }

})
;

angular.module('Rvd').directive('conferenceDialNoun', function (RvdConfiguration) {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            var localWaitType; // we need somewhere to store it but not in scope

            // init scope.waitType
            function initWaitType() {
					if (!!scope.dialnoun.waitModule) {
                    localWaitType = 'Module';
                    scope.dialnoun.waitUrl = undefined;
                } else
                if (scope.dialnoun.waitUrl) {
                    localWaitType = 'URL';
                    scope.dialnoun.waitModule = undefined;
                } else {
                    localWaitType = 'Module';
                    scope.dialnoun.waitUrl = undefined;
                }
            }

            function setWaitType(waitType) {
                if (localWaitType != waitType) {
                    if (waitType == 'Module')
                        scope.dialnoun.waitUrl = undefined;
                    else if (waitType == 'URL')
                        scope.dialnoun.waitModule = undefined;
                    else
                        return // do nothing - this is invalid waitType
                    localWaitType = waitType;
                }
            }

            function getWaitType() {
                return localWaitType;
            }

            function chooseProjectWav(wav) {
				scope.dialnoun.waitUrl = RvdConfiguration.projectsRootPath + '/' + scope.project.projectName + '/wavs/' + wav.filename; // TODO build a proper link here
			}

            // bootstrap
            initWaitType();
            // show advanced view if any property is set
            if (!!scope.dialnoun.muted || !!scope.dialnoun.beep || !!scope.dialnoun.startConferenceOnEnter
				|| !!scope.dialnoun.endConferenceOnExit || !!scope.dialnoun.waitModule || !!scope.dialnoun.waitUrl) {
					scope.dialnoun.iface = {advancedShown: true}
			}

            // public interface
            scope.getWaitType = getWaitType;
            scope.setWaitType = setWaitType;
            scope.chooseProjectWav = chooseProjectWav;
        }
    }
});
