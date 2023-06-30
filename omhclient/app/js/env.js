(function (window) {
    //following environment variable pattern described here: https://www.jvandemo.com/how-to-configure-your-angularjs-application-using-environment-variables/
    window.__env = window.__env || {};
    window.__env.baseUrl = '/omhonfhir/';
    window.__env.fitbitShim = 'fitbit';
    window.__env.googleFitShim = 'googlefit';
    window.__env.iHealthShim = 'ihealth';
    window.__env.withingsShim = 'withings';
    window.__env.omhOnFhirClientId = '93651a15-4664-486e-8661-eca7ebc21bda';
    window.__env.omhOnFhirScope = 'patient/*.read launch'; //to force provider login use the following scopes 'openid profile'
    window.__env.omhOnFhirRedirectUri = 'http://SOMHFHIR01.fhs.com:8989/omhonfhir/activity';  //'http://rmgomhclient:8989/omhonfhir/login';  //'https://apps.hdap.gatech.edu/omhonfhir/login';
    window.__env.omhOnFhirAPIBase = 'http://SOMHFHIR01.fhs.com:8888/mdata'; 'http://STOMHFHIR01.reliantmedicalgroup.org:8888/mdata' //shimmerAuthentication Redirect URL Base  //'https://apps.hdap.gatech.edu/mdata';
    window.__env.omhOnFhirAPIShimmerAuth = '/shimmerAuthentication';
    window.__env.CheckORDforOMHURL = 'http://Sesscriptprd01.fhs.com/Interconnect-WebServiceHostPRD/api/Garber/2020/Informatics/Utility/CheckORDforOMH';
    window.__env.FileShimmerIDforOMHURL = 'http://Sesscriptprd01.fhs.com/Interconnect-WebServiceHostPRD/api/Garber/2020/Informatics/Utility/FileShimmerIDforOMH';
}(this));