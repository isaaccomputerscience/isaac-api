/**
 * Copyright 2015 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto.content;

/**
 * @author sac92
 *
 */
public class AnvilAppDTO extends ContentDTO {
    private String appId;
    private String appAccessKey;

    /**
     * AnvilAppDTO.
     */
    public AnvilAppDTO() {

    }

    /**
     * Gets the appId.
     * 
     * @return the appId
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Sets the appId.
     * 
     * @param appId
     *            the appId to set
     */
    public void setAppId(final String appId) {
        this.appId = appId;
    }

    /**
     * Gets the appAccessKey.
     * 
     * @return the appAccessKey
     */
    public String getAppAccessKey() {
        return appAccessKey;
    }

    /**
     * Sets the appAccessKey.
     * 
     * @param appAccessKey
     *            the appAccessKey to set
     */
    public void setAppAccessKey(final String appAccessKey) {
        this.appAccessKey = appAccessKey;
    }
}
