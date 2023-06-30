'use strict';

var env = {};
console.log('I made it to app.js!');
//Import variables if present  from env.js
if(window){
    Object.assign(env, window.__env);
}

// Declare app level module which depends on views, and components
var ngModule = angular.module('myApp', [
  'ngRoute',
  'launch',
  'activity',
  'login',
  'callback',
  'omhOnFhirService',
  'ngCookies'
]).
config(['$locationProvider', '$routeProvider', '$httpProvider', function($locationProvider, $routeProvider, $httpProvider) {
  $locationProvider.html5Mode(true);
  $locationProvider.hashPrefix('!');

  $routeProvider.
      when('/launch',{
        template: '<launch></launch>'
      }).
      when('/activity', {
        template: '<activity></activity>'
      }).
      when('/login', {
        template: '<login></login>'
      }).
      when('/callback', {
        template: '<callback></callback>'
      }).
      otherwise({redirectTo: '/login'});

  $httpProvider.interceptors.push(['$q',                    //*LDG Added this to prevent sending cookies to server in order to force login each time (hopefully)
        function($q) {
            return {
                request: function(config) {
                    config.withCredentials = false;
                    config.cache = false;
                    return config;
                }
            };
        }
  ]);
}])
.constant('__env', env);//Register environment in AngularJS as constant


