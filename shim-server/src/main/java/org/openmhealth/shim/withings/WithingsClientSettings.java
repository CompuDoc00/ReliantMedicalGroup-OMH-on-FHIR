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

import org.openmhealth.shim.OAuth2ClientSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * @author Emerson Farrugia
 * @author Larry Garber, MD
 */
@Component
@ConfigurationProperties("openmhealth.shim.withings")
public class WithingsClientSettings extends OAuth2ClientSettings {

    private boolean intradayDataAvailable = false;


    public boolean isIntradayDataAvailable() {

        return intradayDataAvailable;
    }

    public void setIntradayDataAvailable(boolean intradayDataAvailable) {

        this.intradayDataAvailable = intradayDataAvailable;
    }
    private List<String> scopes = Arrays.asList(
            "user.info",
            "user.metrics",
            "user.activity",
            "user.sleepevents"
    );

    @Override
    public List<String> getScopes() {

        return scopes;
    }

    public void setScopes(List<String> scopes) {

        this.scopes = scopes;
    }
}
