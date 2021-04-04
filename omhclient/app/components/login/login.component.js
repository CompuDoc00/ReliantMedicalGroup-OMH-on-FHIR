'use strict';

angular.
module('login').
component('login', {

    templateUrl: 'components/login/login.template.html',
    controller: ['$scope', '$http', '$window', '$location', '$routeParams', '$cookies', 'OmhOnFhirApi', '__env', function LoginController($scope, $http, $window, $location, $routeParams, $cookies, OmhOnFhirApi, env){
        var self = this;
        self.env = env;
        self.omhOnFhirApi = OmhOnFhirApi;
        self.shimmerId;
        self.loginSuccess;
        self.alertMsg = "";
        self.pageMsg = 'Link patient to an existing account.';
        self.googleOauthUrl;
        self.patientName;
        self.DisplayName;
        self.EhrID;
        self.Measure;
        self.ORDaccessCode = "";
        self.SSN4 = "";
        self.waitingForLookup = false;
        self.data;

        console.log("Params passed to login");
        console.log($routeParams);

        //===================================================================================
        // Initialization
        //===================================================================================
        console.log("Checking for oauth ready state");
        FHIR.oauth2.ready(function(smart) {
            console.log("Looking at patient");
            console.log(smart);
            smart.patient.read().then(function (pt) {
                self.omhOnFhirApi.setPatientResourceObj(pt);
                self.patientName = self.omhOnFhirApi.getPatientName();
                self.pageMsg = "Link patient " + self.patientName + " to an existing account.";
                $scope.patientName = self.omhOnFhirApi.getPatientName();
                //update the scope so the variables are updated in the view
                $scope.$apply();
            });
        },
        function(error){
            console.log("Error");
            console.log(error);
        });

        //When the window gets focus check to see if authentication through OAuth was successful.
        //If so, forward to activity page, otherwise remain on login page.
        $window.onfocus = function (){
            console.log("Login window has focus");
            console.log("Login in successful " + this.loginSuccess);
            console.log("Shimmer Id: " + this.shimmerId);
            this.patientName = $scope.patientName;
            console.log("Patient Name: " + this.patientName);
            if( this.loginSuccess == true ){
                //forward to the activity page
                console.log("Authentication successful redirecting to " + self.env.baseUrl + "activity?shimmerId=" + this.shimmerId + "&patientName=" + this.patientName);
                $window.location.href = self.env.baseUrl + "activity?shimmerId=" + this.shimmerId + "&patientName=" + this.patientName;
            }
            else{
                console.log("Login not successful");
                self.alertMsg = "Authentication and authorization not yet completed";
            }
        };

        //===================================================================================
        // Functions
        //===================================================================================
        self.setShimmerId = function setShimmerId(shimmerId){
            console.log("LOGIN SETTING SHIMMER ID " + shimmerId);
            self.shimmerId = shimmerId;
        };
        self.setLoginSuccess = function setLoginSuccess(loginSuccess){
            console.log("LOGIN SETTING SUCCESS " + loginSuccess);
            self.loginSuccess = loginSuccess;
        };
        self.loginWithFitbit = function loginWithFitbit() {
            console.log("Logging in with Fitbit");
            self.omhOnFhirApi.login(self.env.fitbitShim);
        };

        self.loginWithWithings = function loginWithWithings(DisplayName, EhrID, Measure){
            console.log("Storing Epic Name and MRN for use by other components");
            self.omhOnFhirApi.setDisplayName(DisplayName); //*LDG This sets the display name in the root scope
            //$window.localStorage.setItem("DisplayName", DisplayName);   //*LDG Also store in in persistent storage
            self.omhOnFhirApi.setEhrID(EhrID); //*LDG This sets the Medical Record Number in the root scope
            self.omhOnFhirApi.setMeasure(Measure); //*LDG This sets the Measure in the root scope
            self.omhOnFhirApi.setShimKey(self.env.withingsShim); //*LDG This sets the ShimKey in the root scope
            console.log("Logging in with Withings for " + DisplayName+ " with MRN " + EhrID);
            self.omhOnFhirApi.login(self.env.withingsShim, EhrID);     //*LDG Lets explicitly pass the patient's MRN
        };

        self.loginWithiHealth = function loginWithiHealth(DisplayName, EhrID, Measure){
            console.log("Storing Epic Name and MRN for use by other components");
            self.omhOnFhirApi.setDisplayName(DisplayName); //*LDG This sets the display name in the root scope
            //$window.localStorage.setItem("DisplayName", DisplayName);   //*LDG Also store in in persistent storage
            self.omhOnFhirApi.setEhrID(EhrID); //*LDG This sets the Medical Record Number in the root scope
            self.omhOnFhirApi.setMeasure(Measure); //*LDG This sets the Measure in the root scope
            self.omhOnFhirApi.setShimKey(self.env.iHealthShim); //*LDG This sets the ShimKey in the root scope
            console.log("Logging in with iHealth for " + DisplayName+ " with MRN " + EhrID);
            self.omhOnFhirApi.login(self.env.iHealthShim, EhrID);     //*LDG Lets explicitly pass the patient's MRN
        };

        self.loginWithGoogleFit = function loginWithGoogleFit(DisplayName, EhrID, Measure){
            console.log("Storing Epic Name and MRN for use by other components");
            self.omhOnFhirApi.setDisplayName(DisplayName); //*LDG This sets the display name in the root scope
            //$window.localStorage.setItem("DisplayName", DisplayName);   //*LDG Also store in in persistent storage
            self.omhOnFhirApi.setEhrID(EhrID); //*LDG This sets the Medical Record Number in the root scope
            self.omhOnFhirApi.setMeasure(Measure); //*LDG This sets the Measure in the root scope
            self.omhOnFhirApi.setShimKey(self.env.googleFitShim); //*LDG This sets the ShimKey in the root scope
            console.log("Logging in with Google fit for " + DisplayName+ " with MRN " + EhrID);
            self.omhOnFhirApi.login(self.env.googleFitShim, EhrID);     //*LDG Lets explicitly pass the patient's MRN
        };

        self.toggleShowLogin = function toggleShowLogin(){
            self.showLogin = !self.showLogin;
            console.log("Toggled showLogin to: " + self.showLogin);
        };
        self.toggleVisibleSSN = function toggleVisibleSSN() {
            self.typePassword = !self.typePassword;
        };
        self.orderLookup = function orderLookup(){
            self.waitingForLookup = true;
            $scope.lookupResponse = "";                       //*LDG This and setting waitingForLookup to true sets the waiting spinner going
            console.log("Now waiting for Epic to confirm ORD Access Code " + self.ORDaccessCode + " and first 4 digits of SSN " + self.SSN4);
            self.omhOnFhirApi.setORDcode(self.ORDaccessCode);              //*LDG Save this so that I can send it back to Epic later
            //self.omhOnFhirApi.lookup(self.ORDaccessCode = "", self.SSN4 = "");
            console.log("Sending request to Epic Interconnect to lookup and validate patient");
            //returns a promise that contains headers, status, and data
            //var headers =
            //    {
            //        "Connection": "close"
            //    };
            //console.log("Here are the headers: " + headers);
            //*LDG to force close so we don't get the "Commit" error and then the chunked error, although we fixed this instead by ensuring that a session is open from the start
            //return $http.get(shimmerDocRefUrl, {"headers": headers});
            var lookupUrl =
                env.CheckORDforOMHURL +
                "?ORDcode=" + self.ORDaccessCode +
                "&Last4SSN=" + self.SSN4;
            console.log("Checking with URL: " + lookupUrl);
            //$http.get(lookupUrl, {"method": 'JSONP'}).then(
            $http.get(lookupUrl).then(
                function (data) {
                    $scope.lookupResponse = data.data;     //*LDG This will then be accessed by the login.template.html when on of the data aggregator buttons is pressed.
                }
            );
            console.log("Here's what I immediately got for the response " + $scope.lookupResponse);
            //self.waitingForLookup = false;     //*LDG I removed this because it reaches here before we actually have a response since it's actually asynchronous
 /*
            $scope.lookupResponse = {};        //*LDG If you comment out all of the about code and just activate this, you can test without connecting to Epic's Interconnect server
            $scope.lookupResponse.OK = true;
            $scope.lookupResponse.MRN = 123456;
            $scope.lookupResponse.Name = 'Lorenzo';
            $scope.lookupResponse.Measure = 'OXYGEN_SATURATION';
*/
        };

    }]
});
