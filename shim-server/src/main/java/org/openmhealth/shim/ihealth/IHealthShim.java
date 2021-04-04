/*
 * Copyright 2017 Open mHealth
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.openmhealth.shim.ihealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.openmhealth.shim.*;
import org.openmhealth.shim.ihealth.mapper.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.DefaultOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static org.openmhealth.shim.ihealth.IHealthShim.IHealthDataTypes.*;
import static org.slf4j.LoggerFactory.getLogger;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.TreeMap;
import java.util.Iterator;
import org.springframework.security.oauth2.client.filter.state.DefaultStateKeyGenerator;
import org.springframework.security.oauth2.client.filter.state.StateKeyGenerator;

/**
 * Encapsulates parameters specific to the iHealth REST API and processes requests made of shimmer for iHealth data.
 *
 * @author Chris Schaefbauer
 * @author Emerson Farrugia
 */
@Component
public class IHealthShim extends OAuth2Shim {

    private static final Logger logger = getLogger(IHealthShim.class);

    private StateKeyGenerator stateKeyGenerator = new DefaultStateKeyGenerator();     //*LDG Added this from the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken

    public static final String SHIM_KEY = "ihealth";
    private static final String USER_AUTHORIZATION_URL_SUFFIX = "/OAuthv2/userauthorization/";
    private static final String ACCESS_TOKEN_URL_SUFFIX = USER_AUTHORIZATION_URL_SUFFIX;

    @Autowired
    private IHealthClientSettings clientSettings;

    @Override
    public String getLabel() {

        return "iHealth";
    }

    @Override
    public String getShimKey() {

        return SHIM_KEY;
    }

    @Override
    public String getUserAuthorizationUrl() {

        return clientSettings.getApiBaseUrl() + USER_AUTHORIZATION_URL_SUFFIX;
    }

    @Override
    public String getAccessTokenUrl() {

        return clientSettings.getApiBaseUrl() + ACCESS_TOKEN_URL_SUFFIX;
    }

    @Override
    protected OAuth2ClientSettings getClientSettings() {

        return clientSettings;
    }

    @Override                                   //*LDG Withings doesn't have this override line!
    public AuthorizationCodeAccessTokenProvider getAuthorizationCodeAccessTokenProvider() {

        return new IHealthAuthorizationCodeAccessTokenProvider();
    }

    @Override
    public ShimDataType[] getShimDataTypes() {

        return new ShimDataType[] {
                OXYGEN_SATURATION, // TODO the order matters here since the first is used as a trigger request
                BLOOD_GLUCOSE,
                PHYSICAL_ACTIVITY,
                BODY_WEIGHT,
                BODY_MASS_INDEX,
                HEART_RATE,
                STEP_COUNT,
                SLEEP_DURATION,
                BLOOD_PRESSURE
        };
    }

    public enum IHealthDataTypes implements ShimDataType {

        PHYSICAL_ACTIVITY("sport.json"),
        BLOOD_GLUCOSE("glucose.json"),
        BLOOD_PRESSURE("bp.json"),
        BODY_WEIGHT("weight.json"),
        BODY_MASS_INDEX("weight.json"),
        HEART_RATE("bp.json", "spo2.json"),
        STEP_COUNT("activity.json"),
        SLEEP_DURATION("sleep.json"),
        OXYGEN_SATURATION("spo2.json");

        private List<String> endPoint;

        IHealthDataTypes(String endpoint) {

            this.endPoint = singletonList(endpoint);
        }

        IHealthDataTypes(String... endpoints) {

            this.endPoint = Lists.newArrayList(endpoints);
        }

        public List<String> getEndpoints() {

            return endPoint;
        }

    }

    @Override            //*LDG This override was commented out in Withings.
    protected ResponseEntity<ShimDataResponse> getData(OAuth2RestOperations restTemplate,
            ShimDataRequest shimDataRequest) throws ShimException {

        final IHealthDataTypes dataType;
        try {
            dataType = valueOf(
                    shimDataRequest.getDataTypeKey().trim().toUpperCase());
        }
        catch (NullPointerException | IllegalArgumentException e) {
            throw new ShimException("Null or Invalid data type parameter: "
                    + shimDataRequest.getDataTypeKey()
                    + " in shimDataRequest, cannot retrieve data.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDate = shimDataRequest.getStartDateTime() == null ?
                now.minusDays(1) : shimDataRequest.getStartDateTime();
        OffsetDateTime endDate = shimDataRequest.getEndDateTime() == null ?
                now.plusDays(1) : shimDataRequest.getEndDateTime();

        /*
            The physical activity point handles start and end datetimes differently than the other endpoints. It
            requires use to include the range until the beginning of the next day.
         */
        //if (dataType == PHYSICAL_ACTIVITY) {     //*LDG Actually, this was also a problem with Heart Rate, so all may be like this!

        endDate = endDate.plusDays(1);
        //}

        // SC and SV values are client-based keys that are unique to each endpoint within a project
        String scValue = clientSettings.getClientSerialNumber();
        List<String> svValues = getEndpointSecrets(dataType);

        List<JsonNode> responseEntities = newArrayList();

        int i = 0;

        // We iterate because one of the measures (Heart rate) comes from multiple endpoints, so we submit
        // requests to each of these endpoints, map the responses separately and then combine them
        for (String endpoint : dataType.getEndpoints()) {

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(clientSettings.getApiBaseUrl() + "/");

            // Need to use a dummy userId if we haven't authenticated yet. This is the case where we are using
            // getData to trigger Spring to conduct the OAuth exchange
            String userId = "uk";

            logger.info("iHealthShim is processing a getData request");
            if (shimDataRequest.getAccessParameters() != null) {

                OAuth2AccessToken token =
                        SerializationUtils.deserialize(shimDataRequest.getAccessParameters().getSerializedToken());

                userId = Preconditions.checkNotNull((String) token.getAdditionalInformation().get("UserID"));
                uriBuilder.queryParam("access_token", token.getValue());
                logger.info("iHealthShim getData request for userId="+userId);
            }

            uriBuilder.path("/user/")
                    .path(userId + "/")
                    .path(endpoint)
                    .queryParam("client_id", restTemplate.getResource().getClientId())
                    .queryParam("client_secret", restTemplate.getResource().getClientSecret())
                    .queryParam("start_time", startDate.toEpochSecond())
                    .queryParam("end_time", endDate.toEpochSecond())
                    .queryParam("locale", "default")
                    .queryParam("sc", scValue)
                    // TODO this is way too brittle, retrieve endpoint secret by endpoint instead of by measure type
                    .queryParam("sv", svValues.get(i));

            ResponseEntity<JsonNode> responseEntity;

            try {
                URI url = uriBuilder.build().encode().toUri();
                logger.info("iHealthShim getData request url="+url);
                responseEntity = restTemplate.getForEntity(url, JsonNode.class);     //*LDG This starts oauth2.client.OAuth2RestTemplate.createRequest and oauth2.client.token.AccessTokenProviderChain.obtainAccessToken will refeshAccessToken if necessary
                                                                                     //*LDG If it does need to refresh the access token, it invokes IHealthAuthorizationCodeAccessTokenProvider.refreshAccessToken below,  then oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
                                                                                     //*LDG                        and then oauth2.client.token.OAuth2AccessTokenSupport.retrieveToken
            }
            catch (HttpClientErrorException | HttpServerErrorException e) {
                // TODO figure out how to handle this
                logger.error("A request for iHealth data failed.", e);
                throw e;
            }

            logger.info("iHealthShim getData request was successful!");

            if (shimDataRequest.getNormalize()) {

                IHealthDataPointMapper mapper;

                switch (dataType) {

                    case PHYSICAL_ACTIVITY:
                        mapper = new IHealthPhysicalActivityDataPointMapper();
                        break;
                    case BLOOD_GLUCOSE:
                        mapper = new IHealthBloodGlucoseDataPointMapper();
                        break;
                    case BLOOD_PRESSURE:
                        mapper = new IHealthBloodPressureDataPointMapper();
                        break;
                    case BODY_WEIGHT:
                        mapper = new IHealthBodyWeightDataPointMapper();
                        break;
                    case BODY_MASS_INDEX:
                        mapper = new IHealthBodyMassIndexDataPointMapper();
                        break;
                    case STEP_COUNT:
                        mapper = new IHealthStepCountDataPointMapper();
                        break;
                    case SLEEP_DURATION:
                        mapper = new IHealthSleepDurationDataPointMapper();
                        break;
                    case HEART_RATE:
                        // there are two different mappers for heart rate because the data can come from two endpoints
                        if (endpoint.equals("bp.json")) {
                            mapper = new IHealthBloodPressureEndpointHeartRateDataPointMapper();
                            break;
                        }
                        else if (endpoint.equals("spo2.json")) {
                            mapper = new IHealthBloodOxygenEndpointHeartRateDataPointMapper();
                            break;
                        }
                    case OXYGEN_SATURATION:
                        mapper = new IHealthOxygenSaturationDataPointMapper();
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

                responseEntities.addAll(mapper.asDataPoints(singletonList(responseEntity.getBody())));
            }
            else {
                responseEntities.add(responseEntity.getBody());
            }

            i++;
        }

        return ResponseEntity.ok().body(
                ShimDataResponse.result(SHIM_KEY, responseEntities));
    }

    private List<String> getEndpointSecrets(IHealthDataTypes dataType) {

        switch (dataType) {
            case PHYSICAL_ACTIVITY:
                return singletonList(clientSettings.getSportEndpointSecret());
            case BODY_WEIGHT:
                return singletonList(clientSettings.getWeightEndpointSecret());
            case BODY_MASS_INDEX:
                return singletonList(
                        clientSettings.getWeightEndpointSecret()); // body mass index comes from the weight endpoint
            case BLOOD_PRESSURE:
                return singletonList(clientSettings.getBloodPressureEndpointSecret());
            case BLOOD_GLUCOSE:
                return singletonList(clientSettings.getBloodGlucoseEndpointSecret());
            case STEP_COUNT:
                return singletonList(clientSettings.getActivityEndpointSecret());
            case SLEEP_DURATION:
                return singletonList(clientSettings.getSleepEndpointSecret());
            case HEART_RATE:
                return newArrayList(clientSettings.getBloodPressureEndpointSecret(),
                        clientSettings.getSpO2EndpointSecret());
            case OXYGEN_SATURATION:
                return singletonList(clientSettings.getSpO2EndpointSecret());
            default:
                throw new UnsupportedOperationException();
        }
    }
    @Override
    public OAuth2ProtectedResourceDetails getResource() {

        AuthorizationCodeResourceDetails resource = (AuthorizationCodeResourceDetails) super.getResource();
        resource.setAuthenticationScheme(AuthenticationScheme.none);
        return resource;
    }

    @Override
    protected String getAuthorizationUrl(UserRedirectRequiredException exception, Map<String, String> addlParameters) {

        final OAuth2ProtectedResourceDetails resource = getResource();

        UriComponentsBuilder callBackUriBuilder = UriComponentsBuilder.fromUriString(getDefaultRedirectUrl())
                .queryParam("state", exception.getStateKey());

        UriComponentsBuilder authorizationUriBuilder = UriComponentsBuilder.fromUriString(exception.getRedirectUri())
                .queryParam("client_id", resource.getClientId())
                .queryParam("response_type", "code")
                .queryParam("APIName", Joiner.on(' ').join(resource.getScope()))
                //.queryParam("RequiredAPIName", Joiner.on(' ').join(resource.getScope()))    //*LDG This is optional
                .queryParam("redirect_uri", callBackUriBuilder.build().toString());

        return authorizationUriBuilder.build().encode().toString();
    }

    public class IHealthAuthorizationCodeAccessTokenProvider extends AuthorizationCodeAccessTokenProvider {

        public IHealthAuthorizationCodeAccessTokenProvider() {

            this.setTokenRequestEnhancer(new RequestEnhancer() {

                @Override
                public void enhance(AccessTokenRequest request,
                        OAuth2ProtectedResourceDetails resource,
                        MultiValueMap<String, String> form, HttpHeaders headers) {

                    form.set("client_id", resource.getClientId());
                    form.set("client_secret", resource.getClientSecret());
                    form.set("redirect_uri", getDefaultRedirectUrl());
                    form.set("state", request.getStateKey());
                }
            });
        }

        @Override
        protected HttpMethod getHttpMethod() {

            //return HttpMethod.GET;                //*LDG Changing this to a POST because that's what Google and Withings use although it did not seem to matter...
            return HttpMethod.POST;
        }

                                  //*LDG I am trying to make this like Withings since this is where the access and refesh tokens get updated when they expire
        @Override
        public OAuth2AccessToken refreshAccessToken(
                OAuth2ProtectedResourceDetails resource,
                OAuth2RefreshToken refreshToken, AccessTokenRequest request)
                throws UserRedirectRequiredException,
                OAuth2AccessDeniedException {

            logger.info("iHealth resource grant_type we are about to use="+resource.getGrantType());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            //resource.setGrantType("refresh_token");                                                         //*LDG Since this is a request to refresh the access token, we must change "authorization_code" to "refresh_token"
            logger.info("iHealth resource authentication scheme we are about to use="+resource.getAuthenticationScheme());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth resource client authentication scheme we are about to use="+resource.getClientAuthenticationScheme());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth resource AccessTokenUri we are about to use="+resource.getAccessTokenUri());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth resource Scope we are about to use="+resource.getScope());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            //logger.info("iHealth resource response_type we are about to use="+resource.getResponseType());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth request headers we are about to use="+request.getHeaders());                         //*LDG This is the request we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth request CurrentUri we are about to use="+request.getCurrentUri());                         //*LDG This is the request we are about to use to get a new access token (and new refresh token)
            logger.info("iHealth refreshToken we are about to use="+refreshToken);                         //*LDG This is the refresh token we are about to use to get a new access token (and new refresh token)

            MultiValueMap form = new LinkedMultiValueMap();                                                 //*LDG Added this back from the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
            form.add("response_type", "refresh_token");                                                     //*LDG iHealth uses "response_type" instead of "grant_type" for refresh!!! Added this based on the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
            form.add("refresh_token", refreshToken.getValue());                                             //*LDG Added the next few lines back from the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
            OAuth2AccessToken accessToken;
            try {
                accessToken = retrieveToken(request, resource, form, getHeadersForTokenRequest(request));     //*LDG We're going to hold this like withings does (see below)
            }
            catch (OAuth2AccessDeniedException e) {
                throw getRedirectForAuthorization((AuthorizationCodeResourceDetails) resource, request);
            }

            logger.info("iHealth accessToken.getRefreshToken()="+accessToken.getRefreshToken());                         //*LDG This is the new refresh token
            // Withings supposedly replaces the refresh token with each new Access Token (so perhaps iHealth is the same?), but just in case we'll check to see if we need to hold on to the existing refresh token...
            if (accessToken.getRefreshToken() == null) {
                ((DefaultOAuth2AccessToken) accessToken).setRefreshToken(refreshToken);
            }
            return accessToken;
        }

        private HttpHeaders getHeadersForTokenRequest(AccessTokenRequest request) {                           //*LDG Added this from the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
            HttpHeaders headers = new HttpHeaders();
            // No cookie for token request
            return headers;
        }

        private UserRedirectRequiredException getRedirectForAuthorization(AuthorizationCodeResourceDetails resource,        //*LDG Added this from the source code for security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider.refreshAccessToken
                                                                          AccessTokenRequest request) {

            // we don't have an authorization code yet. So first get that.
            TreeMap requestParameters = new TreeMap();
            requestParameters.put("response_type", "code"); // oauth2 spec, section 3
            requestParameters.put("client_id", resource.getClientId());
            // Client secret is not required in the initial authorization request

            String redirectUri = resource.getRedirectUri(request);
            if (redirectUri != null) {
                requestParameters.put("redirect_uri", redirectUri);
            }

            if (resource.isScoped()) {

                StringBuilder builder = new StringBuilder();
                List scope = resource.getScope();

                if (scope != null) {
                    Iterator scopeIt = scope.iterator();
                    while (scopeIt.hasNext()) {
                        builder.append(scopeIt.next());
                        if (scopeIt.hasNext()) {
                            builder.append(' ');
                        }
                    }
                }

                requestParameters.put("scope", builder.toString());
            }

            UserRedirectRequiredException redirectException = new UserRedirectRequiredException(
                    resource.getUserAuthorizationUri(), requestParameters);

            String stateKey = stateKeyGenerator.generateKey(resource);
            redirectException.setStateKey(stateKey);
            request.setStateKey(stateKey);
            redirectException.setStateToPreserve(redirectUri);
            request.setPreservedState(redirectUri);

            return redirectException;

        }

        @Override
        protected ResponseExtractor<OAuth2AccessToken> getResponseExtractor() {

            return new ResponseExtractor<OAuth2AccessToken>() {

                @Override
                public OAuth2AccessToken extractData(ClientHttpResponse response) throws IOException {

                    JsonNode node = new ObjectMapper().readTree(response.getBody());
                    String token = Preconditions
                            .checkNotNull(node.path("AccessToken").textValue(), "Missing access token: %s", node);
                    logger.info("iHealth ResponseExtractor AccessToken="+token);                         //*LDG Just so we can see for testing purposes
                    String refreshToken = Preconditions
                            .checkNotNull(node.path("RefreshToken").textValue(), "Missing refresh token: %s" + node);
                    logger.info("iHealth ResponseExtractor RefreshToken="+refreshToken);                 //*LDG Just so we can see for testing purposes
                    String userId =
                            Preconditions.checkNotNull(node.path("UserID").textValue(), "Missing UserID: %s", node);
                    logger.info("iHealth ResponseExtractor UserID="+userId);                             //*LDG Just so we can see for testing purposes
                    long expiresIn = node.path("Expires").longValue() * 1000;
                    logger.info("iHealth ResponseExtractor Milliseconds that it expiresIn="+expiresIn);  //*LDG Just so we can see for testing purposes
                    Preconditions.checkArgument(expiresIn > 0, "Missing Expires: %s", node);

                    //expiresIn = 180000;                                                 //*LDG We're setting it to exprire in 3 minutes for testing purposes
                    //logger.info("iHealth ResponseExtractor Milliseconds that it NOW expiresIn="+expiresIn);  //*LDG Just so we can see for testing purposes

                    DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken(token);
                    accessToken.setExpiration(new Date(System.currentTimeMillis() + expiresIn));
                    accessToken.setRefreshToken(new DefaultOAuth2RefreshToken(refreshToken));
                    accessToken.setAdditionalInformation(ImmutableMap.<String, Object>of("UserID", userId));
                    return accessToken;

                }
            };
        }



    }

}
