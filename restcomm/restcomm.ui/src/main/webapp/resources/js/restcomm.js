var rcMod = angular.module('rcApp', [
  'ngRoute',
  'rcApp.filters',
  'rcApp.services',
  'rcApp.directives',
  'rcApp.controllers',
  'ngResource',
  'ui.bootstrap',
  'angular-md5',
  'loadingOnAJAX',
  'ui.bootstrap.modal.dialog',
  'angularFileUpload'
]);

rcMod.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
  $routeProvider.
    when('/login', {templateUrl: 'modules/login.html', controller: 'LoginCtrl'}).
    when('/profile', {templateUrl: 'modules/profile.html', controller: 'ProfileCtrl'}).
    when('/profile/:accountSid', {templateUrl: 'modules/profile.html', controller: 'ProfileCtrl'}).
    when('/dashboard', {templateUrl: 'modules/dashboard.html', controller: 'DashboardCtrl'}).
    when('/numbers', {redirectTo: '/numbers/incoming'}).
    when('/numbers/incoming', {templateUrl: 'modules/numbers-incoming.html', controller: 'NumbersCtrl'}).
    when('/numbers/incoming/:phoneSid', {templateUrl: 'modules/numbers-incoming-details.html', controller: 'NumberDetailsCtrl', resolve: { $modalInstance : function() {return undefined;} }}).
    when('/numbers/clients', {templateUrl: 'modules/numbers-clients.html', controller: 'ClientsCtrl'}).
    when('/numbers/clients/:clientSid', {templateUrl: 'modules/numbers-clients-details.html', controller: 'ClientDetailsCtrl', resolve: { $modalInstance : function() {return undefined;} }}).
    when('/numbers/outgoing', {templateUrl: 'modules/numbers-outgoing.html', controller: 'OutgoingCtrl'}).
    when('/numbers/shortcodes', {templateUrl: 'modules/numbers-shortcodes.html', controller: 'MainCtrl'}).
    when('/numbers/porting', {templateUrl: 'modules/numbers-porting.html', controller: 'MainCtrl'}).
    when('/logs', {redirectTo: '/logs/calls'}).
    when('/logs/calls', {templateUrl: 'modules/logs-calls.html', controller: 'LogsCallsCtrl'}).
    when('/logs/calls/:callSid', {templateUrl: 'modules/logs-calls-details.html', controller: 'LogsCallsDetailsCtrl', resolve: { $modalInstance : function() {return undefined;}, callSid: function() {} }}).
    when('/logs/messages', {templateUrl: 'modules/logs-messages.html', controller: 'LogsMessagesCtrl'}).
    when('/logs/recordings', {templateUrl: 'modules/logs-recordings.html', controller: 'LogsRecordingsCtrl'}).
    when('/logs/transcriptions', {templateUrl: 'modules/logs-transcriptions.html', controller: 'LogsTranscriptionsCtrl'}).
    when('/logs/notifications', {templateUrl: 'modules/logs-notifications.html', controller: 'LogsNotificationsCtrl'}).
    when('/usage', {templateUrl: 'modules/usage.html', controller: 'MainCtrl'}).
    when('/providers', {templateUrl: 'modules/providers.html', controller: 'MainCtrl'}).
    when('/ras', {templateUrl: 'modules/rappmanager.html', controller: 'RappManagerCtrl', resolve: {products: rappManagerCtrl.getProducts, localApps: rappManagerCtrl.getLocalApps} }).
    when('/ras/config/:projectName', {templateUrl: 'modules/rappmanager-config.html', controller: 'RappManagerConfigCtrl', resolve: { rappConfig : rappManagerConfigCtrl.loadRappConfig }}).
    otherwise({redirectTo: '/dashboard'});

  // $locationProvider.html5Mode(true);
}]);

rcMod.directive('equals', function() {
  return {
    restrict: 'A', // only activate on element attribute
    require: '?ngModel', // get a hold of NgModelController
    link: function(scope, elem, attrs, ngModel) {
      if(!ngModel) return; // do nothing if no ng-model

      // watch own value and re-validate on change
      scope.$watch(attrs.ngModel, function() {
        validate();
      });

      // observe the other value and re-validate on change
      attrs.$observe('equals', function (val) {
        validate();
      });

      var validate = function() {
        // values
        var val1 = ngModel.$viewValue;
        var val2 = attrs.equals;

        // set validity
        ngModel.$setValidity('equals', val1 === val2);
      };
    }
  }
});

rcMod.run(function($rootScope, $location, $anchorScroll, AuthService) {
  $rootScope.$on("$routeChangeStart", function(event, next, current) {
    $anchorScroll(); // scroll to top
    if(!AuthService.isLoggedIn()) {
      $location.path("/login");
    }
  })
});

// AJAX LOADER

angular
  .module('loadingOnAJAX', [])
  .config(function($httpProvider) {
    $httpProvider.responseInterceptors.push(interceptor);
    var numLoadings = 0;
    // var loadingScreen = $('<div style="position:fixed;top:0;left:0;right:0;bottom:0;z-index:10000;background-color:gray;background-color:rgba(70,70,70,0.2);"><img style="position:absolute;top:50%;left:50%;" alt="" src="data:image/gif;base64,R0lGODlhQgBCAPMAAP///wAAAExMTHp6etzc3KCgoPj4+BwcHMLCwgAAAAAAAAAAAAAAAAAAAAAAAAAAACH/C05FVFNDQVBFMi4wAwEAAAAh/hpDcmVhdGVkIHdpdGggYWpheGxvYWQuaW5mbwAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9VBzMu/8VcRTWsVXFYYBsS4knZZYH4d6gYdpyLMErnBAwGFg0pF5lcBBYCMEhR3dAoJqVWWZUMRB4Uk5KEAUAlRMqGOCFhjsGjbFnnWgliLukXX5b8jUUTEkSWBNMc3tffVIEA4xyFAgCdRiTlWxfFl6MH0xkITthfF1fayxxTaeDo5oUbW44qaBpCJ0tBrmvprc5GgKnfqWLb7O9xQQIscUamMJpxC4pBYxezxi6w8ESKU3O1y5eyts/Gqrg4cnKx3jmj+gebevsaQXN8HDJyy3J9OCc+AKycCVQWLZfAwqQK5hPXR17v5oMWMhQEYKLFwmaQTDgl5OKHP8cQjlGQCHIKftOqlzJsqVLPwJiNokZ86UkjDg5emxyIJHNnDhtCh1KtGjFkt9WAgxZoGNMny0RFMC4DyJNASZtips6VZkEp1P9qZQ3VZFROGLPfiiZ1mDKHBApwisZFtWkmNSUIlXITifWtv+kTl0IcUBSlgYEk2tqa9PhZ2/Fyd3UcfIQAwXy+jHQ8R0+zHVHdQZ8A7RmIZwFeN7TWMpS1plJsxmNwnAYqc4Sx8Zhb/WPyqMynwL9eMrpQwlfTOxQco1gx7IvOPLNmEJmSbbrZf3c0VmRNUVeJZe0Gx9H35x9h6+HXjj35dgJfYXK8RTd6B7K1vZO/3qFi2MV0cccemkkhJ8w01lA4ARNHegHUgpCBYBUDgbkHzwRAAAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9VAjMu/8VIRTWcVjFYYBsSxFmeVYm4d6gYa5U/O64oGQwsAwOpN5skipWiEKPQXBAVJq0pYTqnCB8UU5KwJPAVEqK7mCbrLvhyxRZobYlYMD5CYxzvmwUR0lbGxNHcGtWfnoDZYd0EyKLGAgClABHhi8DmCxjj3o1YYB3Em84UxqmACmEQYghJmipVGRqCKE3BgWPa7RBqreMGGfAQnPDxGomymGqnsuAuh4FI7oG0csAuRYGBgTUrQca2ts5BAQIrC8aBwPs5xzg6eEf1lzi8qf06foVvMrtm7fO3g11/+R9SziwoZ54DoPx0CBgQAGIEefRWyehwACKGv/gZeywcV3BFwg+hhzJIV3Bbx0IXGSJARxDmjhz6tzJs4NKkBV7SkJAtOi6nyDh8FRnlChGoVCjSp0aRqY5ljZjplSpNKdRfxQ8Jp3ZE1xTjpkqFuhGteQicFQ1xmWEEGfWXWKfymPK9kO2jxZvLstW1GBLwI54EiaqzxoRvSPVrYWYsq8byFWxqcOs5vFApoKlEEm8L9va0DVHo06F4HQUA6pxrQZoGIBpyy1gEwlVuepagK1xg/BIWpLn1wV6ASfrgpcuj5hkPpVOIbi32lV3V+8U9pVVNck5ByPiyeMjiy+Sh3C9L6VyN9qZJEruq7X45seNe0Jfnfkp+u1F4xEjKx6tF006NPFS3BCv2AZgTwTwF1ZX4QnFSzQSSvLeXOrtEwEAIfkECQoAAAAsAAAAAEIAQgAABP8QyEmrvVQIzLv/FSEU1nFYhWCAbEsRx1aZ5UG4OGgI9ny+plVuCBiQKoORr1I4DCyDJ7GzEyCYziVlcDhOELRpJ6WiGGJCSVhy7k3aXvGlGgfwbpM1ACabNMtyHGCAEk1xSRRNUmwmV4F7BXhbAot7ApIXCJdbMRYGA44uZGkSIptTMG5vJpUsVQOYAIZiihVtpzhVhAAGCKQ5vaQiQVOfGr+PZiYHyLlJu8mMaI/GodESg7EfKQXIBtrXvp61F2Sg10RgrBwEz7DoLcONH5oa3fBUXKzNc2TW+Fic8OtAQBzAfv8OKgwBbmEOBHiSRIHo0AWBFMuwPdNgpGFFAJr/li3D1KuAu48YRBIgMHAPRZSeDLSESbOmzZs4oVDaKTFnqZVAgUbhSamVzYJIIb70ybSp06eBkOb81rJklCg5k7IkheBq0UhTgSpdKeFqAYNOZa58+Q0qBpluAwWDSRWYyXcoe0Gc+abrRL7XviGAyNLDxSj3bArey+EuWJ+LG3ZF+8YjNW9Ac5m0LEYv4A8GTCaGp5fykNBGPhNZrHpcajOFi8VmM9i0K9G/EJwVI9VM7dYaR7Pp2Fn3L8GcLxREZtJaaMvLXwz2NFvOReG6Mel+sbvvUtKbmQgvECf0v4K2k+kWHnp8eeO+v0f79PhLdz91sts6C5yFfJD3FVIHHnoWkPVRe7+Qt196eSkongXw4fQcCnW41F9F0+ETAQAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9dAjMu/8VISCWcFiFYIBsS4lbJcSUSbg4aMxrfb68nFBSKFg0xhpNgjgMUM9hZye4URCC6MRUGRxI18NSesEOehIqGjCjUK1pU5KMMSBlVd9LXCmI13QWMGspcwADWgApiTtfgRIEBYCHAoYEA2AYWHCHThZ2nCyLgG9kIgehp4ksdlmAKZlCfoYAjSpCrWduCJMuBrxAf1K5vY9xwmTExp8mt4GtoctNzi0FmJMG0csAwBUGs5pZmNtDWAeeGJdZBdrk6SZisZoaA5LuU17n9jpm7feK53Th+FXs3zd//xJOyKbQGAIriOp1a9giErwYCCJGZEexQ8ZzIP8PGPplDRGtjj7OVUJI4CHKeQhfypxJs6bNDyU11rs5IaTPnBpP0oTncwzPo0iTKjXWMmbDjPK8IShikmfIlVeslSwwseZHn1G0sitY0yLINGSVEnC6lFVXigbi5iDJ8WW2tWkXTpWYd9tdvGkjFXlrdy1eDlOLsG34t9hUwgwTyvV2d6Big4efDe6LqylnDt+KfO6cGddmNwRGf5qcxrNp0SHqDmnqzbBqblxJwR7WklTvuYQf7yJL8IXL2rfT5c7KCUEs2gt/G5waauoa57vk/Ur9L1LXb12x6/0OnVxoQC3lcQ1xXC93d2stOK8ur3x0u9YriB+ffBl4+Sc5158LMdvJF1Vpbe1HTgQAIfkECQoAAAAsAAAAAEIAQgAABP8QyEmrvXQMzLv/lTEUliBYxWCAbEsRwlaZpUC4OCgKK0W/pl5uWCBVCgLE7ERBxFDGYUc0UDYFUclvMkhWnExpB6ERAgwx8/Zsuk3Qh6z4srNybb4wAKYHIHlzHjAqFEh2ABqFWBRoXoESBAVmEkhZBANuGJeHXTKMmDkphC8amUN8pmxPOAaik4ZzSJ4ScIA5VKO0BJOsCGaNtkOtZY9TAgfBUri8xarJYsOpzQAIyMxjVbwG0tN72gVxGGSl3VJOB+GaogXc5ZoD6I7YGpLuU/DI9Trj7fbUyLlaGPDlD0OrfgUTnkGosAUCNymKEGzYIhI+JghE0dNH8QKZY+j/8jEikJFeRwwgD4xAOJChwowuT8qcSbOmzQ5FRugscnNCypD5IkYc0VML0JB9iipdyrQptIc9yRyysC1jETkzU2IxZfVqgYk2yRxNdxUB2KWRUtK65nSX02Lb2NoTETOE1brNwFljse2q25MiQnLUZPWsTBghp76QiLegXpXi2GlrnANqCHCz9g3uVu0AZYMZDU8zEFKuZtHdSKP7/Cb0r7/KDPwCaRr010kkWb8hkEq15xyRDA/czIr3JNWZdcCeYNbUQLlxX/CmCgquWTO5XxzKvnt5ueGprjc5tC0Vb+/TSJ4deNbsyPXG54rXHn4qyeMPa5+Sxp351JZU6SbMGXz+2YWeTOxZ4F4F9/UE4BeKRffWHgJ6EAEAIfkECQoAAAAsAAAAAEIAQgAABP8QyEmrvXQMzLv/lTEglmYhgwGuLEWYlbBVg0C0OCim9DwZMlVuCECQKoVRzCdBCAqWApTY2d0oqOkENkkeJ04m9fIqCCW7M0BGEQnUbu34YvD2rhIugMDGBucdLzxgSltMWW0CAl9zBAhqEnYTBAV4ZAOWBU8WdZYrWZBWY3w2IYpyK3VSkCiMOU6uboM4dQNmbQSQtI+Jf0Sqt4Acsp45tcHCpr5zqsXJfLOfBbwhzsl7unWbFwhSlddUTqcclN664IE1iq5k3tTow5qn53Td3/AcCAdP9FXv+JwQWANIEFfBZAIjSRHY7yAGSuoESHDkbWFDhy8U7dsnxwBFbw7/O2iUgYxOrpDk7qFcybKly5cIK7qDSUHjgY37uumcNo3mBAE3gQaV6LOo0aNI4XkcGFJnFUc62bEUesCWJYpR/7nMeDPoFCNGTiatBZSogYtHCTBN2sIjWnAi1po08vaavqpy0UBlyFJE15L1wNaF9yKo1ImCjTq5KWYS3xCDh2gFUOcAqg8G6AK8G3lY2M4sgOzL+/QxQANBSQf+dxZ0m5KiD7jObBqx6gsDqlbgMzqHI7E/avu+6Yp3Y8zAHVty20ETo7IWXtz2l1zt1Uz72ty8fM2jVrVq1GK5ieSmaxC/4TgKv/zmcqDHAXmHZH23J6CoOONLPpG/eAoFZIdEHHz4LEWfJwSY55N30RVD3IL87VFMDdOh9B88EQAAIfkECQoAAAAsAAAAAEIAQgAABP8QyEmrvbQUzLv/lVEg1jBYyGCAbEsRw1aZ5UC4OCiq80kZplVuCECQKprjhEZJyZpPIkZUuL1iPeRAKSEIfFIOQiOUAAtlANMc/Jm4YQsVXuAtwQAYvtiOcwhkTVsZUU5uAlZ+BghpEkkvaB2AiQB1UWZVOWORP3WNOAZflABAApc6m41jcDiGh3agqT8Eny4GtK+1LHO6fmxfvbsanL4hJrBhi5nFFV7IIJOfBsF+uCEIphiAI6PMLikC2VObjN62A+E2H9sj1OYi6cQetxrd5hXYpu5y1vfj9v4CXpgmkBkBK6sQ9CvYYke6LqtGGNknEEa4i+LMHBwxgqEHdOn/ynG4RTHgJI8oU6pcyXKlkZcwW5Y4gPGiEY4JZc6gyVPAgT06gwodStQjSaFjAGokEDOoz3iUmMJUWNKfxZ7iXh6sarTOUzNcZS4sqmgsQxFKRzI1WxDBgZ8Ub0llK7DUW3kD54YtBuOtAFYT9BLFdlfbVjl7W4jslHEX08Qf3AqAPItqwFA00+o4SLcYZkRSblmeMI2yiDSf98ode1hKgZ8hnmq+wLmRXMoE3o7CDPTD0WYHmxwAPAEblwE05ajzdZsCcjzJJ7zGY+AtceaPK+im8Fb4ASQ0KXdoHvhtmu6kt5P22VvR6CXRJ6Cf4POS2wPip3yqr/17hvjSnVKXGnry+VcefkjNV6AF1gmV2ykKOgIaWRT4FFAEACH5BAkKAAAALAAAAABCAEIAAAT/EMhJq720FMy7/5VREJZmIYUBriwlbpUZD2prf289FUM4pLeghIA4jWKwCWFQrCCaQo4BpRsWoBLZBDEgUZa9aIdwreYoPxfPzMOKLdNjBrhLAgxpCpf+xpy3cll2S1giXX0SU1UST4UIXhhkVXtwgSxECIt/Qng0IW03cZkVZJBBXG6dnqGNZgaLNgYEbD+wLKK2iIkDvLm3rbqVtYhxvm9gxhdEs3DJx7BTTJHAwUJgeRdT1NUrZLyHHpiPztWGvKMgsk/kwVzDsczcHVOm8vY47PfdXo0E8fo2iBQQwGuIuCf/AHLwRpAgtjvqGin0wItgmXkJJ1oopbGjx48g/0MCPNhPZIUBAlKqJLjskct6IlE2VBnGpM2bOHN6lJXPHgqYLmQtA+pRJsFHX1r6ywgSzEoBMJbO6jmRiMwwr3SGo6p1Xtadlla88sdVDIKUq/BJLRsFj0o+ftaaXKLSTVKyOc+mtONiaiWA6NRAjXXggF1detmSKnxAsQcDAg4IcHyHMeXHKhUTsKzGsQgzKok+5ozmQM0gA0/fyXxjQOFFmw2LiV0P8gG+ILjAKnz67OEtArDIrCTaBoLCplyfTpnBtIvIv4kV5oucQuEvkmNIvoyhwGvsja0fcFF9AuTB8gwUduNd9fXSfI9PtvdQQmTq45urBqBlovoD9bxn3hd3NsVmgYATRFZcVeiJV4IAC5rEnD0RAAAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9FCHMu/+VgRBWUVhEYYBsS4lbhZyy6t6gaFNFPBmmFW4IIJAqhFEN2bNoiB6YcJL0SUy1IxUL7VSnAGmGJgHuyiZt9wJTA2bg5k++Pa/ZGnBS/dxazW5QBgRgEnsvCIUhShMzVmWMLnuFYoJBISaPOV9IkUOOmJc4gyNgBqddg6YFA3Y3pIl3HWauo5OybCa1Q6SKuCm7s4mKqLgXhBY6moa3xkQpAwPLZVXIzi1A0QWByXvW1xwi2rGbSb7gVNHkLqfn6GHf7/Lh7vM31kZGxfbYM9ED1EaM0MfPi4l/rf6cGsit4JV/PeqpcojhEMWLGDNq3Agln0cjHP8nIBz50WPIhwIGpFRJ5qTLlzBjrkEgLaSGhoYKCDjA80DIaCl7qBnQs+cAnAWhpVwZo6eAbTJ1qARYBCnMeDI7DqgHDohVNkQPtOSHICjXH2EPbL0IRIDbdRjK8hTw9V3blNMApM1LkYDKpxiI1hIxDy6kVq948u1CIOVZEI0PCHjM6y/lcHMvV3bccSfdF8FYiDBlmVfmCoK76Bzrl/MNop8pEOBZl0Pj2GgB31tbYSdVCWX5lh2aEgVUWQh4gkk9wS2P4j/eyjOwc+xONTszOH8++V0ByXrAU+D5Yidp3dcMKK7w/beE7BRYynCruQWX+GIrSGYPncfYedQd4AYZeS+Ix9FsAliwX2+4adTYfwQ+VxtG/V0TAQAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9FCHMu/+VgRCWZhGIAa4sJW6VGRdqa39vPSFFWKS3oIRAqqCKO9gEpdwhhRgDSjccxZoAzRNAKPSgHRGBmqP8XDwybwsOHa9UmcRwpnSBbU55aU3aC090gHlzYyd9c3hRillyEyJUK0SGLlNggpGCWCBSI5GWUF1bmpErUkRkBqUtUmpeq6ZHsIQAgjRtp5S0Ll6MUJ2zuD/BF6ilqrvFxzybhZ7JQl29epO60DheXmwWudbX3Dy9xI+T48kEA8M3qua7rd/wks3x0TUH9wKD9DYiXukSBe4JPCBg3j4+BdINSNekiwCBAg52SJgOUDAEAwxKBCWxo8ePIP9DwhtIUmQFigtTFnhIkqBJMyljfnlJs6bNm/Qwajz4hoNDiDRlMgpIMiPNLjEXwoCoD2e/lEO24VzSbuqHLlUJiVk34N5MiRjztaMjcEDWPHRS+irBUoBUnisXvu1KcOfGhQUxdL0Vwi6YtSL+tSDw0G8QwmYJESZ4loWBAQISg1ksoDEryJIPP6zMy/IjRo8jW6YcaS+YlV9rYW7clbMdgm9BEHYbAnJq2QPYPBxgJy8HjE/icmvaBgFjCrYpCIg4Qfij5bFxPUz98Mny3sx3iIYX0PWQ4xMeulhOJvk1A9VPRq7gEnk+I+S/ebFgWnl2CQjWz/CI/kCk9kvE9xIUAQCGd4AF0NGE3m3XnZSZVfpdEwEAIfkECQoAAAAsAAAAAEIAQgAABP8QyEmrvZQQzLv/laFZCGIRiAGuLCVuFXqmbQ2KNFWGpWr/ANGJ4JvIMghYRgnEvIoSQ7KyQzKD1Sbn6dJAj9Geq3TVhryxnCSLNSHV5gt3Iv0yUUwpXIsYlDV5RB0iX2xRgjUDBwJXc0B6UFgFZR8GB5eRL1p4PAV7K5aXeQaRNaRQep8soQelcWOeri2ssnGptbMCB26vIbGJBwOlYL0hpSKTGIqXBcVNKAXJGAiXi5TOWwjRqhUF1QK42EEE24gfBMu84hfkk+EX2u/OhOv1K8T2Zojf0vmz0NEkFNBVLZg6f3K0RVt4Z+A3hB0WejLHbsBBiF3kYdzIsaPHjyz/CBZcBJKCxJMiCwooOSHagAIvXzZjSbOmzZvitF3kyIkDuWUkS8JkCGVASgF+WEKL+dINwZcaMeoZegjnlqhWO5DDamuKqXQ8B1jUaMDhgQJczUgRO9YDgqfXEJYV28+Ct0U7O/60iMHbJyn5KIbhm0tA3jjohL0yoAtcPQN008YQQFnyKraWgzRGxQ0UnLmKbRCg7JiC0ZlA+qCOgtmG0dJGKMcFgQ52FKo10JWiPCADYQzomMDs7SszlcomBawWm3w15KSPKa8GIJsCZRdIj4cWN9D2aNvX6RhFJfawFsaMtFcI39Lw5O3OAlYwepD9GuUkzGNDf8W+ZvgefWeBEn8AGDUbQuhcRGAfxtnD3DoRAAAh+QQJCgAAACwAAAAAQgBCAAAE/xDISau9lBDMu/8VcRSWZhmEAa4shRxHuVVI2t6gAc+TSaE2nBAwGFgEoxBPApQNPbokpXAQKEMI1a/29FAPWokInFkCwwDgsnuCkSgwREY+QdF7NTTb8joskUY9SxpmBFl7EggDawCAGQd3FyhohoyTOANVen2MLXZ6BghcNwZIZBSZgUOGoJV6KwSmaAYFr54Gs6KHQ6VVnYhMrmxRAraIoaLGpEiRwEx5N5m1J83OTK92v1+Q1ry6vwAIpgLg3dS6yhPbA+nmdqJBHwaZ3OYchtA3BNP2GJf9AD0YCggMlwRTAwqUIygJXwE6BUzBEDCgGsMtoh4+NFOAXpWLHP8y1oh3YZ9FkGlIolzJsqXLlzgkwpgIcwKCAjhzPhSApCcMVTBvCtV4sqbRo0iTshFak1WHfQN6WgmaM5+EiFWqUFxIMJROnDN4UuSX1E5OMVyPGlSKaF+7bqHenogqoKi9fQ/lponIk+zFUAkVthPHc9FLwGA58K17FO9DDBH9PguoMuXjFgSi2u2SWTKvwnpx0MIZ2h/ogLQSlq5QauuW1axJpvac4/QUAW+GKGo2G3ZEwxl4ws5QZE3qzSU9R80NIHO5fUsUMX82/II4drcjFXGR8EdxgPMYoyKHCmhmoM1V9/s9iyIait6x1+mIXEjrNeKmw59SMUSR6l5UE1EjM9txN1049RUUlR771fFfUw1OEJUF38E0TzURJkLbUR31EwEAOwAAAAAAAAAAAA==" /></div>')
    var loadingScreen = $('<div style="position: fixed; top: 0px; left: 0px; right: 0px; bottom: 0px; z-index: 10000; background-color: rgba(70, 70, 70, 0.2);"><div style="position: absolute; top: 50%; left: 50%;"><i class="icon-spinner icon-4x icon-spin"></i></div>')
      .appendTo($('body')).hide();
    $httpProvider.responseInterceptors.push(function() {
      return function(promise) {
        numLoadings++;
        loadingScreen.show();
        var hide = function(r) { if (!(--numLoadings)) loadingScreen.hide(); return r; };
        return promise.then(hide, hide);
      };
    });
  });



var interceptor = ['$rootScope', '$q', '$location', function (scope, $q, $location) {

  function success(response) {
    return response;
  }

  function error(response) {
    var status = response.status;

    if (status == 401) {
      console.log("Redirecting to login due to 401 ERROR CODE (@" + $location.url() + ")");
      $location.path("/login");
      return $q.reject(response);
    }
    // otherwise
    return $q.reject(response);

  }

  return function (promise) {
    return promise.then(success, error);
  }

}];


// MD5
angular.module('angular-md5', []).factory('md5', [function() {
    var md5 = {
      createHash: function(str) {
        var xl;

        var rotateLeft = function (lValue, iShiftBits) {
          return (lValue << iShiftBits) | (lValue >>> (32 - iShiftBits));
        };

        var addUnsigned = function (lX, lY) {
          var lX4, lY4, lX8, lY8, lResult;
          lX8 = (lX & 0x80000000);
          lY8 = (lY & 0x80000000);
          lX4 = (lX & 0x40000000);
          lY4 = (lY & 0x40000000);
          lResult = (lX & 0x3FFFFFFF) + (lY & 0x3FFFFFFF);
          if (lX4 & lY4) {
            return (lResult ^ 0x80000000 ^ lX8 ^ lY8);
          }
          if (lX4 | lY4) {
            if (lResult & 0x40000000) {
              return (lResult ^ 0xC0000000 ^ lX8 ^ lY8);
            } else {
              return (lResult ^ 0x40000000 ^ lX8 ^ lY8);
            }
          } else {
            return (lResult ^ lX8 ^ lY8);
          }
        };

        var _F = function (x, y, z) {
          return (x & y) | ((~x) & z);
        };
        var _G = function (x, y, z) {
          return (x & z) | (y & (~z));
        };
        var _H = function (x, y, z) {
          return (x ^ y ^ z);
        };
        var _I = function (x, y, z) {
          return (y ^ (x | (~z)));
        };

        var _FF = function (a, b, c, d, x, s, ac) {
          a = addUnsigned(a, addUnsigned(addUnsigned(_F(b, c, d), x), ac));
          return addUnsigned(rotateLeft(a, s), b);
        };

        var _GG = function (a, b, c, d, x, s, ac) {
          a = addUnsigned(a, addUnsigned(addUnsigned(_G(b, c, d), x), ac));
          return addUnsigned(rotateLeft(a, s), b);
        };

        var _HH = function (a, b, c, d, x, s, ac) {
          a = addUnsigned(a, addUnsigned(addUnsigned(_H(b, c, d), x), ac));
          return addUnsigned(rotateLeft(a, s), b);
        };

        var _II = function (a, b, c, d, x, s, ac) {
          a = addUnsigned(a, addUnsigned(addUnsigned(_I(b, c, d), x), ac));
          return addUnsigned(rotateLeft(a, s), b);
        };

        var convertToWordArray = function (str) {
          var lWordCount;
          var lMessageLength = str.length;
          var lNumberOfWords_temp1 = lMessageLength + 8;
          var lNumberOfWords_temp2 = (lNumberOfWords_temp1 - (lNumberOfWords_temp1 % 64)) / 64;
          var lNumberOfWords = (lNumberOfWords_temp2 + 1) * 16;
          var lWordArray = new Array(lNumberOfWords - 1);
          var lBytePosition = 0;
          var lByteCount = 0;
          while (lByteCount < lMessageLength) {
            lWordCount = (lByteCount - (lByteCount % 4)) / 4;
            lBytePosition = (lByteCount % 4) * 8;
            lWordArray[lWordCount] = (lWordArray[lWordCount] | (str.charCodeAt(lByteCount) << lBytePosition));
            lByteCount++;
          }
          lWordCount = (lByteCount - (lByteCount % 4)) / 4;
          lBytePosition = (lByteCount % 4) * 8;
          lWordArray[lWordCount] = lWordArray[lWordCount] | (0x80 << lBytePosition);
          lWordArray[lNumberOfWords - 2] = lMessageLength << 3;
          lWordArray[lNumberOfWords - 1] = lMessageLength >>> 29;
          return lWordArray;
        };

        var wordToHex = function (lValue) {
          var wordToHexValue = "",
            wordToHexValue_temp = "",
            lByte, lCount;
          for (lCount = 0; lCount <= 3; lCount++) {
            lByte = (lValue >>> (lCount * 8)) & 255;
            wordToHexValue_temp = "0" + lByte.toString(16);
            wordToHexValue = wordToHexValue + wordToHexValue_temp.substr(wordToHexValue_temp.length - 2, 2);
          }
          return wordToHexValue;
        };

        var x = [],
          k, AA, BB, CC, DD, a, b, c, d, S11 = 7,
          S12 = 12,
          S13 = 17,
          S14 = 22,
          S21 = 5,
          S22 = 9,
          S23 = 14,
          S24 = 20,
          S31 = 4,
          S32 = 11,
          S33 = 16,
          S34 = 23,
          S41 = 6,
          S42 = 10,
          S43 = 15,
          S44 = 21;

        //str = this.utf8_encode(str);
        x = convertToWordArray(str);
        a = 0x67452301;
        b = 0xEFCDAB89;
        c = 0x98BADCFE;
        d = 0x10325476;

        xl = x.length;
        for (k = 0; k < xl; k += 16) {
          AA = a;
          BB = b;
          CC = c;
          DD = d;
          a = _FF(a, b, c, d, x[k + 0], S11, 0xD76AA478);
          d = _FF(d, a, b, c, x[k + 1], S12, 0xE8C7B756);
          c = _FF(c, d, a, b, x[k + 2], S13, 0x242070DB);
          b = _FF(b, c, d, a, x[k + 3], S14, 0xC1BDCEEE);
          a = _FF(a, b, c, d, x[k + 4], S11, 0xF57C0FAF);
          d = _FF(d, a, b, c, x[k + 5], S12, 0x4787C62A);
          c = _FF(c, d, a, b, x[k + 6], S13, 0xA8304613);
          b = _FF(b, c, d, a, x[k + 7], S14, 0xFD469501);
          a = _FF(a, b, c, d, x[k + 8], S11, 0x698098D8);
          d = _FF(d, a, b, c, x[k + 9], S12, 0x8B44F7AF);
          c = _FF(c, d, a, b, x[k + 10], S13, 0xFFFF5BB1);
          b = _FF(b, c, d, a, x[k + 11], S14, 0x895CD7BE);
          a = _FF(a, b, c, d, x[k + 12], S11, 0x6B901122);
          d = _FF(d, a, b, c, x[k + 13], S12, 0xFD987193);
          c = _FF(c, d, a, b, x[k + 14], S13, 0xA679438E);
          b = _FF(b, c, d, a, x[k + 15], S14, 0x49B40821);
          a = _GG(a, b, c, d, x[k + 1], S21, 0xF61E2562);
          d = _GG(d, a, b, c, x[k + 6], S22, 0xC040B340);
          c = _GG(c, d, a, b, x[k + 11], S23, 0x265E5A51);
          b = _GG(b, c, d, a, x[k + 0], S24, 0xE9B6C7AA);
          a = _GG(a, b, c, d, x[k + 5], S21, 0xD62F105D);
          d = _GG(d, a, b, c, x[k + 10], S22, 0x2441453);
          c = _GG(c, d, a, b, x[k + 15], S23, 0xD8A1E681);
          b = _GG(b, c, d, a, x[k + 4], S24, 0xE7D3FBC8);
          a = _GG(a, b, c, d, x[k + 9], S21, 0x21E1CDE6);
          d = _GG(d, a, b, c, x[k + 14], S22, 0xC33707D6);
          c = _GG(c, d, a, b, x[k + 3], S23, 0xF4D50D87);
          b = _GG(b, c, d, a, x[k + 8], S24, 0x455A14ED);
          a = _GG(a, b, c, d, x[k + 13], S21, 0xA9E3E905);
          d = _GG(d, a, b, c, x[k + 2], S22, 0xFCEFA3F8);
          c = _GG(c, d, a, b, x[k + 7], S23, 0x676F02D9);
          b = _GG(b, c, d, a, x[k + 12], S24, 0x8D2A4C8A);
          a = _HH(a, b, c, d, x[k + 5], S31, 0xFFFA3942);
          d = _HH(d, a, b, c, x[k + 8], S32, 0x8771F681);
          c = _HH(c, d, a, b, x[k + 11], S33, 0x6D9D6122);
          b = _HH(b, c, d, a, x[k + 14], S34, 0xFDE5380C);
          a = _HH(a, b, c, d, x[k + 1], S31, 0xA4BEEA44);
          d = _HH(d, a, b, c, x[k + 4], S32, 0x4BDECFA9);
          c = _HH(c, d, a, b, x[k + 7], S33, 0xF6BB4B60);
          b = _HH(b, c, d, a, x[k + 10], S34, 0xBEBFBC70);
          a = _HH(a, b, c, d, x[k + 13], S31, 0x289B7EC6);
          d = _HH(d, a, b, c, x[k + 0], S32, 0xEAA127FA);
          c = _HH(c, d, a, b, x[k + 3], S33, 0xD4EF3085);
          b = _HH(b, c, d, a, x[k + 6], S34, 0x4881D05);
          a = _HH(a, b, c, d, x[k + 9], S31, 0xD9D4D039);
          d = _HH(d, a, b, c, x[k + 12], S32, 0xE6DB99E5);
          c = _HH(c, d, a, b, x[k + 15], S33, 0x1FA27CF8);
          b = _HH(b, c, d, a, x[k + 2], S34, 0xC4AC5665);
          a = _II(a, b, c, d, x[k + 0], S41, 0xF4292244);
          d = _II(d, a, b, c, x[k + 7], S42, 0x432AFF97);
          c = _II(c, d, a, b, x[k + 14], S43, 0xAB9423A7);
          b = _II(b, c, d, a, x[k + 5], S44, 0xFC93A039);
          a = _II(a, b, c, d, x[k + 12], S41, 0x655B59C3);
          d = _II(d, a, b, c, x[k + 3], S42, 0x8F0CCC92);
          c = _II(c, d, a, b, x[k + 10], S43, 0xFFEFF47D);
          b = _II(b, c, d, a, x[k + 1], S44, 0x85845DD1);
          a = _II(a, b, c, d, x[k + 8], S41, 0x6FA87E4F);
          d = _II(d, a, b, c, x[k + 15], S42, 0xFE2CE6E0);
          c = _II(c, d, a, b, x[k + 6], S43, 0xA3014314);
          b = _II(b, c, d, a, x[k + 13], S44, 0x4E0811A1);
          a = _II(a, b, c, d, x[k + 4], S41, 0xF7537E82);
          d = _II(d, a, b, c, x[k + 11], S42, 0xBD3AF235);
          c = _II(c, d, a, b, x[k + 2], S43, 0x2AD7D2BB);
          b = _II(b, c, d, a, x[k + 9], S44, 0xEB86D391);
          a = addUnsigned(a, AA);
          b = addUnsigned(b, BB);
          c = addUnsigned(c, CC);
          d = addUnsigned(d, DD);
        }

        var temp = wordToHex(a) + wordToHex(b) + wordToHex(c) + wordToHex(d);

        return temp.toLowerCase();
      }

    };

    return md5;

  }])
  .filter('gravatar', ['md5', function(md5) {
    return function(text) {
      return (text) ? md5.createHash(text.toLowerCase()) : '';
    };
  }]);