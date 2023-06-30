angular.module('omhOnFhirService', [])
.factory('OmhOnFhirApi', [ '$rootScope', '$http', '$window', '__env', function($rootScope, $http, $window, env){
    var factory = {};

    console.log("Looking at environments");
    console.log(env);

    //===================================================================================
    // Variables in the factory
    //===================================================================================\
    factory.patientResourceObj;
    factory.patientId;
    factory.loginWindow;

    //===================================================================================
    // Getters and setters for the objects in the factory
    //===================================================================================
    factory.getPatientResourceObj = function getPatientResourceObj(){
        return this.patientResourceObj;
    };
    factory.setPatientResourceObj = function setPatientResourceObj (patientResourceObj){
        console.log("OMH on FHIR Service setting Patient");
        console.log(patientResourceObj);
        this.patientResourceObj = patientResourceObj;

        this.patientId = this.patientResourceObj.id;
        console.log("PatientId: " + this.getPatientId());

        $rootScope.patientName = this.getPatientNameFromObject(this.patientResourceObj);
        $rootScope.$apply();
        console.log("PatientName: " + this.patientName);
    };

    factory.getPatientId = function getPatientId(){
        return this.patientId;
    };
    factory.setPatientId = function setPatientId(patientId){
        this.patientId = patientId;
    };

    factory.getPatientName = function getPatientName(){
        return $rootScope.patientName;
    };
    factory.setPatientName = function setPatientName(patientName){
        $rootScope.patientName = patientName;
    };

    factory.getDisplayName = function getDisplayName(){
        return $rootScope.DisplayName;
    };
    factory.setDisplayName = function setDisplayName(DisplayName){
        $rootScope.DisplayName = DisplayName;
        $window.localStorage.setItem("DisplayName", DisplayName);   //*LDG Also store in in persistent storage
    };

    factory.getEhrID = function getEhrID(){
        return $rootScope.EhrID;
    };
    factory.setEhrID = function setEhrID(EhrID){
        $rootScope.EhrID = EhrID;
        $window.localStorage.setItem("EhrID", EhrID);   //*LDG Also store in in persistent storage
    };

    factory.getORDcode = function getORDcode(){
        return $rootScope.ORDcode;
    };
    factory.setORDcode = function setORDcode(ORDcode){
        $rootScope.ORDcode = ORDcode;
        $window.localStorage.setItem("ORDcode", ORDcode);   //*LDG Also store in in persistent storage
    };

    factory.getShimKey = function getShimKey(){
        return $rootScope.ShimKey;
    };
    factory.setShimKey = function setShimKey(ShimKey){
        $rootScope.ShimKey = ShimKey;
        $window.localStorage.setItem("ShimKey", ShimKey);   //*LDG Also store in in persistent storage
    };

    factory.getEpicOK = function getEpicOK(){
        $rootScope.EpicOK = $window.localStorage.getItem("EpicOK");
        return $rootScope.EpicOK;
    };

    factory.getMeasure = function getMeasure(){
        return $rootScope.Measure;
    };
    factory.setMeasure = function setMeasure(Measure){
        $rootScope.Measure = Measure;
        $window.localStorage.setItem("Measure", Measure);   //*LDG Also store in in persistent storage
    };

    factory.getMeasureDisplay = function getMeasureDisplay(){
        return $rootScope.MeasureDisplay;
    };

    factory.getMeasureDisplayForHeading = function getMeasureDisplayForHeading(){
        return $rootScope.MeasureDisplayForHeading;
    };

    factory.getShimmerId = function getShimmerId(){
        return $rootScope.shimmerId;
    };
    factory.setShimmerId = function setShimmerId(shimmerId){
        console.log("Setting shimmer ID " + shimmerId);
        $rootScope.shimmerId = shimmerId;
        $window.localStorage.setItem("ShimmerId", shimmerId);   //*LDG Also store in in persistent storage
    };

    factory.getLoginSuccessful = function getLoginSuccessful(){
        return $rootScope.loginSuccess;
    };
    factory.setLoginSuccessful = function setLoginSuccessful(loginSuccessful){
        console.log("Was login successful " + loginSuccessful);
        $rootScope.loginSuccess = loginSuccessful;
    };

    //===================================================================================
    // Additional functions for objects in the factory
    //===================================================================================

    factory.getPatientNameFromObject = function getPatientNameFromObject(pt){
        if (pt.name) {
            var names = pt.name.map(function(name) {
                return name.given.join(" ") + " " + name.family;
            });
            return names.join(" / ");
        } else {
            return "anonymous";
        }
    };

    factory.lookup = function lookup(ORDcode, Last4SSN){
        console.log("Sending request to Epic Interconnect to lookup and validate patient");
        var lookupUrl =
            env.CheckORDforOMHURL +
            "?ORDcode=" + ORDcode +
            "&Last4SSN=" + Last4SSN;
        console.log("Checking with URL: " + lookupUrl);
        return $http.get(lookupUrl);
    };

    factory.login = function login( shimKey, ehrID ){
        console.log("Logging in to Shimmer");
        this.loginSuccessful = false;
        if(ehrID == ''){
            ehrID=this.getPatientId();
        }
        var shimmerAuthUrl =
            env.omhOnFhirAPIBase +
            env.omhOnFhirAPIShimmerAuth +
            "?ehrId=" + ehrID +
            "&shimkey=" + shimKey;
        console.log("Authorizing with Shimmer " + shimmerAuthUrl);
        this.loginWindow = $window.open(shimmerAuthUrl, 'Sign In', 'left=100,top=100,width=600,height=900');
    };

    factory.requestDocumentReference = function requestDocumentReference(shimmerId, startDate, endDate, dataType){
        var shimmerDocRefUrl = env.omhOnFhirAPIBase + "/DocumentReference?subject=" + shimmerId;

        if(startDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        if(endDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }

        shimmerDocRefUrl = shimmerDocRefUrl + "&dataType=" + dataType;

        console.log("Requesting Document Reference with header  " + shimmerDocRefUrl);

        //returns a promise that contains headers, status, and data
        var headers =
            {
                "Connection": "close"
            };
        console.log("Here are the headers: " + headers);
        //*LDG to force close so we don't get the "Commit" error and then the chunked error, although we fixed this instead by ensuring that a session is open from the start
        //return $http.get(shimmerDocRefUrl, {"headers": headers});
        return $http.get(shimmerDocRefUrl);
    };

    factory.requestObservation = function requestObservation(shimmerId, startDate, endDate, dataType){
        return this.requestOmhResource("Observation", shimmerId, startDate, endDate, dataType);
    };

    factory.requestOmhResource = function requestOmhResource(resource, shimmerId, startDate, endDate, dataType){
        var shimmerDocRefUrl = env.omhOnFhirAPIBase + "/" + resource + "?subject=" + shimmerId;

        if(startDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + startDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }
        if(endDate){
            shimmerDocRefUrl = shimmerDocRefUrl + "&date=" + endDate.toISOString().substring(0,10);//to make format 'yyyy-MM-dd'
        }

        shimmerDocRefUrl = shimmerDocRefUrl + "&dataType=" + dataType;

        console.log("Requesting with header  " + resource + " "  + shimmerDocRefUrl);

        //returns a promise that contains headers, status, and data
        var headers =
            {
                "Connection": "close"
            };
        console.log("Here are the headers: " + headers);
        //*LDG to force close so we don't get the "Commit" error and then the chunked error
        //return $http.get(shimmerDocRefUrl, {"headers": headers});
        return $http.get(shimmerDocRefUrl);
    };

    factory.requestBinaryAsJson = function requestBinaryAsJson(binaryUrl){
        var shimmerBinaryUrl = env.omhOnFhirAPIBase + "/" + binaryUrl;
        console.log("Requesting Binary with header " + shimmerBinaryUrl);
        var requestOptions =
            {
                "headers":  {
                    "Accept": "application/json"
                }
            };
        //returns a promise that contains headers, status, and data
        return $http.get(shimmerBinaryUrl, requestOptions);
    };

    factory.requestBinaryAsBase64 = function requestBinaryAsBase64(binaryUrl){
        var shimmerBinaryUrl = env.omhOnFhirAPIBase + "/" + binaryUrl;
        console.log("Requesting Binary " + shimmerBinaryUrl);
        //returns a promise that contains headers, status, and data
        return $http.get(shimmerBinaryUrl);
    };

    //Return the factory object
    return factory;
}]);