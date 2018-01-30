var clyKey = '';

if (clyKey) {
  var Countly = Countly || {};
  Countly.q = Countly.q || [];

  Countly.app_key = clyKey;
  Countly.url = 'https://restcomm.count.ly';

  Countly.q.push(['track_sessions']);
  Countly.q.push(['track_pageview', location.pathname + location.hash]);

  $(window).on('hashchange', function() {
    Countly.q.push(['track_pageview',location.pathname + location.hash]);
  });

  Countly.q.push(['track_clicks']);
  Countly.q.push(['track_links']);
  Countly.q.push(['track_forms']);
  Countly.q.push(['collect_from_forms']);
  Countly.q.push(['report_conversion']);

  // load countly script asynchronously
  (function() {
    var cly = document.createElement('script');
    cly.type = 'text/javascript';
    cly.async = true;
    cly.src = 'https://restcomm.count.ly/sdk/web/countly.min.js';
    cly.onload = function(){ Countly.init() };
    var s = document.getElementsByTagName('script')[0];
    s.parentNode.insertBefore(cly, s);
  })();


  var myEvents = [
    // 1. Login attempts, successful and failed ones
    { id: 'sign-in-button', cly: { key: 'login_attempt', values:[ {'id': 'rc-email'} ] } },

    // 2. Successful creation of an app / type of application / kinds of app
    { id: 'select-app-external', cly: { key: 'app_new_select', seg: { type: 'external' } } },
    { id: 'select-app-template', cly: { key: 'app_new_select', seg: { type: 'template' } } },
    { id: 'select-app-import',   cly: { key: 'app_new_select', seg: { type: 'import' } } },

    { id: 'create-app-external', cly: { key: 'app_new_create', seg: { type: 'external', kind: '$("#app-kind-select")[0].innerText.trim()' } } },
    { id: 'create-app-template', cly: { key: 'app_new_create', seg: { type: 'template', kind: '$("#app-kind-select")[0].innerText.trim()' } } },
    { id: 'create-app-import',   cly: { key: 'app_new_create', seg: { type: 'import', kind: '$("#app-kind-select")[0].innerText.trim()' } } },

    // 3. DID - country / capabilities / type
    {
      id: 'search-number-provider',
      cly: {
        key: 'number_provider_search',
        seg: {
          country: '$("#number-country").val()',
          capabilities: '$("#any-capability")[0].checked ? "any" : ( ($("#specific-capability-voice")[0].checked ? "voice" : "") + ($("#specific-capability-sms")[0].checked ? " sms" : "") )',
          type: '$("#any-type")[0].checked ? "any" : ( ($("#specific-type-landline")[0].checked ? "landline" : "") + ($("#specific-type-mobile")[0].checked ? " mobile" : "") + ($("#specific-type-tollfree")[0].checked ? " tollfree" : "") )'
        }
      }
    },

    {
      id: 'register-number-provider',
      cly: {
        key: 'number_provider_register',
        seg: {
          country: '$("#number-country").val()',
          capabilities: '$("#any-capability")[0].checked ? "any" : ( ($("#specific-capability-voice")[0].checked ? "voice" : "") + ($("#specific-capability-sms")[0].checked ? " sms" : "") )',
          type: '$("#any-type")[0].checked ? "any" : ( ($("#specific-type-landline")[0].checked ? "landline" : "") + ($("#specific-type-mobile")[0].checked ? " mobile" : "") + ($("#specific-type-tollfree")[0].checked ? " tollfree" : "") )',
          features: '$(this).closest("tr").children(".provider-numbers-list-features").text().replace(/\\s+/g, "")',
          cost: '$(this).closest("tr").children(".provider-numbers-list-cost").text()'
        }
      }
    },

    {
      id: 'register-number-provider-confirm',
      cly: {
        key: 'number_provider_register_confirm',
        seg: {
          country: '$("#number-country").val()',
          capabilities: '$("#any-capability")[0].checked ? "any" : ( ($("#specific-capability-voice")[0].checked ? "voice" : "") + ($("#specific-capability-sms")[0].checked ? " sms" : "") )',
          type: '$("#any-type")[0].checked ? "any" : ( ($("#specific-type-landline")[0].checked ? "landline" : "") + ($("#specific-type-mobile")[0].checked ? " mobile" : "") + ($("#specific-type-tollfree")[0].checked ? " tollfree" : "") )'
        }
      }
    },

    {
      id: 'register-number-provider-cancel',
      cly: {
        key: 'number_provider_register_cancel',
        seg: {
          country: '$("#number-country").val()',
          capabilities: '$("#any-capability")[0].checked ? "any" : ( ($("#specific-capability-voice")[0].checked ? "voice" : "") + ($("#specific-capability-sms")[0].checked ? " sms" : "") )',
          type: '$("#any-type")[0].checked ? "any" : ( ($("#specific-type-landline")[0].checked ? "landline" : "") + ($("#specific-type-mobile")[0].checked ? " mobile" : "") + ($("#specific-type-tollfree")[0].checked ? " tollfree" : "") )'
        }
      }
    },

    // 4. SIP numbers
    {
      id: 'register-number-sip',
      cly: {
        key: 'number_sip_register',
        seg: {
          hasFriendlyName: '$("#new-number-friendly-name").val().length > 0'
        }
      }
    },
    {
      id: 'register-number-sip-confirm',
      cly: {
        key: 'number_sip_register_confirm',
        seg: {
          hasFriendlyName: '$("#new-number-friendly-name").val().length > 0'
        }
      }
    },
    {
      id: 'register-number-sip-cancel',
      cly: {
        key: 'number_sip_register_cancel',
        seg: {
          hasFriendlyName: '$("#new-number-friendly-name").val().length > 0'
        }
      }
    },

    // 5. Clients
    {
      id: 'register-client',
      cly: {
        key: 'client_register',
        seg: {
          hasFriendlyName: '$("#new-client-friendly-name").val().length > 0'
        }
      }
    },
    {
      id: 'register-client-confirm',
      cly: {
        key: 'client_register_confirm',
        seg: {
          hasFriendlyName: '$("#new-client-friendly-name").val().length > 0'
        }
      }
    },
    {
      id: 'register-client-cancel',
      cly: {
        key: 'client_register_cancel',
        seg: {
          hasFriendlyName: '$("#new-client-friendly-name").val().length > 0'
        }
      }
    },

    // 7. Sub Account creation
    {
      id: 'register-sub-account',
      cly: {
        key: 'subaccount_register'
      }
    },
    {
      id: 'register-sub-account-confirm',
      cly: {
        key: 'subaccount_register_confirm',
        seg: {
          hasFriendlyName: '$("#new-account-friendly-name").val().length > 0',
          role: '$("#new-account-role option:selected").text()',
          status: '$("#new-account-status").val()'
        }
      }
    },
    {
      id: 'register-sub-account-cancel',
      cly: {
        key: 'subaccount_register_cancel',
        seg: {
          hasFriendlyName: '$("#new-account-friendly-name").val().length > 0',
          role: '$("#new-account-role option:selected").text()',
          status: '$("#new-account-status option:selected").text()'
        }
      }
    }
  ];

  var vnSent = false;

  evaluate = function(vars, elem) {
    return _.object(_.map(vars, function(value, key) {
      return [ key, value.startsWith('$(') ? eval(value) : value ];
    }, elem));
  };

  var clyReport = function (scope, elem) {
    elem.on('click', function() {
      angular.forEach(myEvents, function (event) {
        if (elem[0].id === event.id) {
          Countly.q.push(['add_event',{
            key: event.cly.key,
            count: 1,
            segmentation: evaluate(event.cly.seg, elem)
          }]);
        }
      });
      if (!vnSent && $('.user-menu a.dropdown-toggle').text()) {
        Countly.q.push(function() {
          Countly.user_details({
            name: $('.user-menu a.dropdown-toggle').text()
          });
          vnSent = true;
        });
      }
      return true;
    })
  };

  rcDirectives.directive('button', function () {
    return {
      restrict: 'E',
      link: clyReport
    }});

  rcDirectives.directive('a', function () {
    return {
      restrict: 'E',
      link: clyReport
    }
  });
}