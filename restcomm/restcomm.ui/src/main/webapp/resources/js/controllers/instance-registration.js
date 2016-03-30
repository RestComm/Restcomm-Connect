angular.module('rcApp').controller('InstanceRegistrationCtrl', function ($scope, IdentityInstances) {
    $scope.info = {
        InitialAccessToken: "",
        RedirectUrl: "",
        KeycloakBaseUrl: ""
    };
    $scope.accountInfo = {
        username: "",
        password: ""
    };

    $scope.registerInstance = function(info, accountInfo) {
        IdentityInstances.register($.param(info));
    }

});