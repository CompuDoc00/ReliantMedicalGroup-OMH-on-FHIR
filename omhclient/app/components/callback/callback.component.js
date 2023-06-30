'use strict';

angular.
//module('callback').
module('callback').
component('callback', {

    templateUrl: 'components/callback/callback.template.html',
    //controller: ['$scope', '$rootScope', '$http', '$window', '$routeParams', 'OmhOnFhirApi', '__env', function LoginController($scope, $rootScope, $http, $window, $routeParams, OmhOnFhirApi, env){
    controller: ['$scope', '$rootScope', '$http', '$window', '$routeParams', '$cookies', 'OmhOnFhirApi', '__env', function LoginController($scope, $rootScope, $http, $window, $routeParams, $cookies, OmhOnFhirApi, env){
        var self = this;
        self.omhOnFhirApi = OmhOnFhirApi;

        console.log("Params passed to callback");
        console.log($routeParams);

        // Retrieving all cookie keys that login just set
        //var allCookies = $cookies.getAllKeys();
        //console.log("Cookie keys passed to callback" + allCookies);

        //===================================================================================
        // Initialization
        //===================================================================================
        if($routeParams.loginSuccess){
            console.log("Setting login success to: " + $routeParams.loginSuccess);
            //set the variable in the scope for this window
            $rootScope.loginSuccess = ($routeParams.loginSuccess == 'true');
            //set the variable in the scope of the parent window
            $window.opener.self.loginSuccess = ($routeParams.loginSuccess == 'true');
            $scope.linkResponse = {};
            var lookupUrl =
                env.FileShimmerIDforOMHURL +
                "?MRN=" + $window.localStorage.getItem("EhrID") +
                "&ShimKey=" + $window.localStorage.getItem("ShimKey") +
                "&ShimmerID=" + $routeParams.shimmerId +
                "&ORDcode=" + $window.localStorage.getItem("ORDcode");
            console.log("Sending to Epic with URL: " + lookupUrl);
            $http.get(lookupUrl).then(
                function (data) {
                    $scope.linkResponse = data.data;     //*LDG This will be displayed in the activity.template.html
                    $window.localStorage.setItem("EpicOK", $scope.linkResponse.OK);   //*LDG Also store in in persistent storage
                }
            );

        }
        if($routeParams.shimmerId){
            console.log("Setting shimmer id to: " + $routeParams.shimmerId);
            //set the variable in the scope for this window
            $rootScope.shimmerId = $routeParams.shimmerId;
            //set the variable in the scope of the parent window
            $window.opener.self.shimmerId = $routeParams.shimmerId;
        }
        $rootScope.DisplayName = $window.localStorage.getItem("DisplayName");

    }]
});