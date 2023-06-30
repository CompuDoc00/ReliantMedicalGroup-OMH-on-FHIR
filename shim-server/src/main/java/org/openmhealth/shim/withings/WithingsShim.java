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
package org.openmhealth.shim.withings;

//import org.openmhealth.shimmer.configuration.TextJSONMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import com.fasterxml.jackson.databind.JsonNode;
import org.openmhealth.shim.*;
import org.openmhealth.shim.withings.domain.WithingsBodyMeasureType;
import org.openmhealth.shim.withings.mapper.*;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.RequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.nio.charset.Charset;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestTemplate;


import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.http.ResponseEntity.ok;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


/**
 * Encapsulates parameters specific to the Withings REST API and processes requests for Withings data from shimmer.
 *
 * @author Eric Jain
 * @author Chris Schaefbauer
 * @author Larry Garber, MD Updated to OAuth2
 */
@Component
public class WithingsShim extends OAuth2Shim {

    private static final Logger logger = getLogger(WithingsShim.class);

    public static final String SHIM_KEY = "withings";
    private static final String DATA_URL = "https://wbsapi.withings.net/";
    private static final String USER_AUTHORIZATION_URL = "https://account.withings.com/oauth2_user/authorize2";
    private static final String ACCESS_TOKEN_URL = "https://account.withings.com/oauth2/token";
    private static final String INTRADAY_ACTIVITY_ENDPOINT = "getintradayactivity";

    @Autowired
    private WithingsClientSettings clientSettings;

    @Override
    public String getLabel() {

        return "Withings";
    }

    @Override
    public String getShimKey() {

        return SHIM_KEY;
    }

    @Override
    public String getUserAuthorizationUrl() {

        return USER_AUTHORIZATION_URL;
    }

    @Override
    public String getAccessTokenUrl() {

        return ACCESS_TOKEN_URL;
    }

    @Override
    protected OAuth2ClientSettings getClientSettings() {

        return clientSettings;
    }

    public AuthorizationCodeAccessTokenProvider getAuthorizationCodeAccessTokenProvider() {

        return new WithingsAuthorizationCodeAccessTokenProvider();
    }

    @Override
    public ShimDataType[] getShimDataTypes() {

        return new WithingsDataTypes[] {
                WithingsDataTypes.BODY_WEIGHT, // order important because first is picked for trigger call
                WithingsDataTypes.BLOOD_PRESSURE,
                WithingsDataTypes.BODY_HEIGHT,
                WithingsDataTypes.BODY_TEMPERATURE,
                WithingsDataTypes.CALORIES_BURNED,
                WithingsDataTypes.HEART_RATE,
                WithingsDataTypes.SLEEP_DURATION,
                WithingsDataTypes.SLEEP_EPISODE,
                WithingsDataTypes.STEP_COUNT
        };
    }

    public enum WithingsDataTypes implements ShimDataType {

        BLOOD_PRESSURE("measure", "getmeas", true),
        BODY_HEIGHT("measure", "getmeas", true),
        BODY_TEMPERATURE("measure", "getmeas", true),
        BODY_WEIGHT("measure", "getmeas", true),
        CALORIES_BURNED("v2/measure", "getactivity", false),
        HEART_RATE("measure", "getmeas", true),
        SLEEP_DURATION("v2/sleep", "getsummary", false),
        SLEEP_EPISODE("v2/sleep", "getsummary", false),
        STEP_COUNT("v2/measure", "getactivity", false);

        private String endpoint;
        private String measureParameter;
        private boolean usesUnixEpochSecondsDate;

        WithingsDataTypes(String endpoint, String measureParameter, boolean usesUnixEpochSecondsDate) {

            this.endpoint = endpoint;
            this.measureParameter = measureParameter;
            this.usesUnixEpochSecondsDate = usesUnixEpochSecondsDate;
        }

        public String getEndpoint() {

            return endpoint;
        }

        public String getMeasureParameter() {

            return measureParameter;
        }

    }

    //@Override
    //public ShimDataResponse getData(OAuth2RestOperations restTemplate, ShimDataRequest shimDataRequest) throws ShimException {
    protected ResponseEntity<ShimDataResponse> getData(OAuth2RestOperations restTemplate,
               ShimDataRequest shimDataRequest) throws ShimException {

        //AccessParameters accessParameters = shimDataRequest.getAccessParameters();
        //String accessToken = accessParameters.getAccessToken();  //*LDG We get these elsewhere for OAuth2
        //String tokenSecret = accessParameters.getTokenSecret();

        // userid is a unique id associated with each user and returned by Withings in the authorization, this id is
        // used as a parameter in the request
        //final String userid = accessParameters.getAdditionalParameters().get("userid").toString();   //*LDG We also deal with this later

        final WithingsDataTypes withingsDataType;
        try {
            withingsDataType = WithingsDataTypes.valueOf(
                    shimDataRequest.getDataTypeKey().trim().toUpperCase());
        }
        catch (NullPointerException | IllegalArgumentException e) {
            throw new ShimException("Null or Invalid data type parameter: "
                    + shimDataRequest.getDataTypeKey()
                    + " in shimDataRequest, cannot retrieve data.");
        }

        MultiValueMap<String, String> dateTimeMap = new LinkedMultiValueMap<>();
        OffsetDateTime todayInUTC =
                LocalDate.now().atStartOfDay().atOffset(ZoneOffset.UTC);

        OffsetDateTime startDateInUTC = shimDataRequest.getStartDateTime() == null ?
                todayInUTC.minusDays(1) : shimDataRequest.getStartDateTime();

        OffsetDateTime endDateInUTC = shimDataRequest.getEndDateTime() == null ?
                todayInUTC.plusDays(1) :
                shimDataRequest.getEndDateTime().plusDays(1);   // We are inclusive of the last day, so add 1 day to get
                                                                // the end of day on the last day, which captures the entire last day

        if (withingsDataType.usesUnixEpochSecondsDate || isIntradayActivityMeasure(withingsDataType)) {
            //the intraday endpoints for activity also use epoch secs

            dateTimeMap.add("startdate", String.valueOf(startDateInUTC.toEpochSecond()));
            dateTimeMap.add("enddate", String.valueOf(endDateInUTC.toEpochSecond()));
        }
        else {
            dateTimeMap.add("startdateymd", startDateInUTC.toLocalDate().toString());
            dateTimeMap.add("enddateymd", endDateInUTC.toLocalDate().toString());
        }

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder
                .fromUriString(DATA_URL)
                .pathSegment(withingsDataType.getEndpoint());

        String measureParameter;
        if (isIntradayActivityMeasure(withingsDataType)) {
            // intraday data uses a different endpoint
            measureParameter = INTRADAY_ACTIVITY_ENDPOINT;
        }
        else {
            measureParameter = withingsDataType.getMeasureParameter();
        }
        uriComponentsBuilder
                .queryParam("action", measureParameter)
                //.queryParam("userid", userid)    //*LDG This isn't used with OAuth2
                .queryParams(dateTimeMap);

        // if it's a body measure we need to add the meastype number (AKA MagicNumber)
        if (Objects.equals(withingsDataType.getMeasureParameter(), "getmeas")) {

            /*
                The Withings API allows us to query for single body measures, which we take advantage of to reduce
                unnecessary data transfer. However, since blood pressure is represented as two separate measures,
                namely a diastolic and a systolic measure, when the measure type is blood pressure we ask for all
                measures and then filter out the ones we don't care about.
             */
            if (!"BLOOD_PRESSURE".equals(withingsDataType.name())) {

                WithingsBodyMeasureType measureType = WithingsBodyMeasureType.valueOf(withingsDataType.name());
                uriComponentsBuilder.queryParam("meastype", measureType.getMagicNumber());
            }

            uriComponentsBuilder.queryParam("category", 1); // filter out goal data points
        }

        URI uri = uriComponentsBuilder.build().encode().toUri();


        //ObjectMapper objectMapper = new ObjectMapper();    //*LDG Let's try to do it the same way we did GoogleFit
        //URI uri = createWithingsRequestUri(shimDataRequest, userid, withingsDataType);
        //URL url = signUrl(uri.toString(), accessToken, tokenSecret, null);  //*LDG Do it differently with OAuth2

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        //Add the Jackson Message converter that's going to handle response type [class com.fasterxml.jackson.databind.JsonNode]
        // and content type [text/json;charset=UTF-8] which Withings sends back
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();

        // Note: here we are making this converter to process any kind of response,
        // not only application/*json, which is the default behaviour
        converter.setSupportedMediaTypes(Arrays.asList(new MediaType("text", "json", Charset.forName("UTF-8"))));
        //restTemplate.setMessageConverters(converter);

        // TODO: Handle requests for a number of days greater than what Withings supports
        //HttpGet get = new HttpGet(url.toString());
        //HttpResponse response;
        ResponseEntity<JsonNode> responseEntity;
        try {
            //response = httpClient.execute(get);
            //HttpEntity responseEntity = response.getEntity();
            responseEntity = restTemplate.getForEntity(uri, JsonNode.class);
        }
        catch (HttpClientErrorException | HttpServerErrorException e) {
            // TODO figure out how to handle this
            logger.error("A request for Withings data failed.", e);
            throw e;
        }
        if (shimDataRequest.getNormalize()) {

            WithingsDataPointMapper<?> dataPointMapper = getDataPointMapper(withingsDataType);

            //InputStream content = responseEntity.getContent();
            //JsonNode jsonNode = objectMapper.readValue(content, JsonNode.class);
            //List<? extends DataPoint<?>> dataPoints = dataPointMapper.asDataPoints(jsonNode);
            //return ShimDataResponse.result(WithingsShim.SHIM_KEY, dataPoints);
            return ok().body(ShimDataResponse
                    .result(WithingsShim.SHIM_KEY, dataPointMapper.asDataPoints(responseEntity.getBody())));
        }
        else {
            //return ShimDataResponse
            //        .result(WithingsShim.SHIM_KEY, objectMapper.readTree(responseEntity.getContent()));
            return ok().body(ShimDataResponse
                    .result(WithingsShim.SHIM_KEY, responseEntity.getBody()));
        }
    }

    private WithingsDataPointMapper<?> getDataPointMapper(WithingsDataTypes withingsDataType) {

        switch (withingsDataType) {

            case BLOOD_PRESSURE:
                return new WithingsBloodPressureDataPointMapper();

            case BODY_HEIGHT:
                return new WithingsBodyHeightDataPointMapper();

            case BODY_TEMPERATURE:
                return new WithingsBodyTemperatureDataPointMapper();

            case BODY_WEIGHT:
                return new WithingsBodyWeightDataPointMapper();

            case CALORIES_BURNED:
                if (clientSettings.isIntradayDataAvailable()) {
                    return new WithingsIntradayCaloriesBurnedDataPointMapper();
                }

                return new WithingsDailyCaloriesBurnedDataPointMapper();

            case HEART_RATE:
                return new WithingsHeartRateDataPointMapper();

            case SLEEP_DURATION:
                return new WithingsSleepDurationDataPointMapper();

            case SLEEP_EPISODE:
                return new WithingsSleepEpisodeDataPointMapper();

            case STEP_COUNT:
                if (clientSettings.isIntradayDataAvailable()) {
                    return new WithingsIntradayStepCountDataPointMapper();
                }

                return new WithingsDailyStepCountDataPointMapper();

            default:
                throw new UnsupportedOperationException();
        }
    }


    /**
     * Determines whether the request is a Withings intraday request based on the configuration setup and the data type
     * from the Shim API request. This case requires a different endpoint and different time parameters than the
     * standard activity endpoint.
     *
     * @param withingsDataType the withings data type retrieved from the Shim API request
     */
    private boolean isIntradayActivityMeasure(WithingsDataTypes withingsDataType) {

        return clientSettings.isIntradayDataAvailable() && (withingsDataType == WithingsDataTypes.STEP_COUNT || withingsDataType ==
                WithingsDataTypes.CALORIES_BURNED);
    }

    @Override
    protected String getAuthorizationUrl(UserRedirectRequiredException exception, Map<String, String> addlParameters) {

        final OAuth2ProtectedResourceDetails resource = getResource();

        UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromUriString(exception.getRedirectUri())
                .queryParam("state", exception.getStateKey())
                .queryParam("client_id", resource.getClientId())
                .queryParam("response_type", "code")
                //.queryParam("access_type", "offline")
                //.queryParam("approval_prompt", "force")
                .queryParam("scope", StringUtils.collectionToDelimitedString(resource.getScope(), ","))
                .queryParam("redirect_uri", getDefaultRedirectUrl());

        return uriBuilder.build().encode().toUriString();
    }

    /**
     * Simple overrides to base spring class from oauth.
     */
    public class WithingsAuthorizationCodeAccessTokenProvider extends AuthorizationCodeAccessTokenProvider {

        public WithingsAuthorizationCodeAccessTokenProvider() {

            this.setTokenRequestEnhancer(new WithingsTokenRequestEnhancer());
        }

        @Override
        protected HttpMethod getHttpMethod() {

            return HttpMethod.POST;
        }

        @Override
        public OAuth2AccessToken refreshAccessToken(
                OAuth2ProtectedResourceDetails resource,
                OAuth2RefreshToken refreshToken, AccessTokenRequest request)
                throws UserRedirectRequiredException,
                OAuth2AccessDeniedException {

            logger.info("withings resource grant_type we are about to use="+resource.getGrantType());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("withings resource authentication scheme we are about to use="+resource.getAuthenticationScheme());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("withings resource client authentication scheme we are about to use="+resource.getClientAuthenticationScheme());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("withings resource AccessTokenUri we are about to use="+resource.getAccessTokenUri());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("withings resource Scope we are about to use="+resource.getScope());         //*LDG This is the resource we are about to use to get a new access token (and new refresh token)
            logger.info("withings request headers we are about to use="+request.getHeaders());                         //*LDG This is the request we are about to use to get a new access token (and new refresh token)
            logger.info("withings request CurrentUri we are about to use="+request.getCurrentUri());                         //*LDG This is the request we are about to use to get a new access token (and new refresh token)
            logger.info("withings refreshToken we are about to use="+refreshToken);                         //*LDG This is the refresh token we are about to use to get a new access token (and new refresh token)

            OAuth2AccessToken accessToken = super.refreshAccessToken(resource, refreshToken, request);
            logger.info("withings refreshToken="+refreshToken);                         //*LDG Just so we can see for testing purposes, this is the refresh token we just used to get a new access token (and new refresh token)
            logger.info("withings accessToken.getRefreshToken()="+accessToken.getRefreshToken());                         //*LDG Just so we can see for testing purposes, this is the new refresh token
            // Withings supposedly replaces the refresh token with each new Access Token, but just in case we'll check to see if we need to hold on to the existing refresh token...
            if (accessToken.getRefreshToken() == null) {
                ((DefaultOAuth2AccessToken) accessToken).setRefreshToken(refreshToken);
            }
            return accessToken;
        }
    }


    /**
     * Adds parameters required by Withings to authorization token requests.
     */
    private class WithingsTokenRequestEnhancer implements RequestEnhancer {

        @Override
        public void enhance(AccessTokenRequest request,
                OAuth2ProtectedResourceDetails resource,
                MultiValueMap<String, String> form, HttpHeaders headers) {
            //headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);     //*LDG considered adding this but that seems to be unnecessary
            headers.remove(HttpHeaders.AUTHORIZATION);                          //*LDG added this because Withings API doesn't use Basic Auth formatting because we use TLS
            form.set("client_id", resource.getClientId());
            form.set("client_secret", resource.getClientSecret());
            if (request.getStateKey() != null) {
                form.set("redirect_uri", getDefaultRedirectUrl());
            }
        }
    }
}
