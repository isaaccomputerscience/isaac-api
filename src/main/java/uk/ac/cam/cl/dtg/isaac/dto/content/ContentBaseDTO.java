/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dto.content;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.api.client.util.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.segue.dao.TrimWhitespaceDeserializer;

/**
 * Represents any content related data that can be stored by the api.
 * <p>
 * This class is required mainly due to the relatively complex polymorphic type hierarchy that gets serialized and
 * deserialized using a custom serializer (ContentBaseDeserializer).
 */
public abstract class ContentBaseDTO {

  private String id;
  private String type;
  private Set<String> tags;
  private String canonicalSourceFile;
  private String version;
  private List<AudienceContext> audience;
  private Map<String, List<String>> display;

  /**
   * Default constructor.
   */
  public ContentBaseDTO() {
    this.tags = Sets.newHashSet();
  }


  /**
   * Gets the id.
   *
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id to set
   */
  @JsonDeserialize(using = TrimWhitespaceDeserializer.class)
  public void setId(final String id) {
    this.id = id;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the type to set
   */
  public void setType(final String type) {
    this.type = type;
  }

  /**
   * Gets the tags.
   *
   * @return the tags
   */
  public Set<String> getTags() {
    return tags;
  }

  /**
   * Sets the tags.
   *
   * @param tags the tags to set
   */
  public void setTags(final Set<String> tags) {
    this.tags = tags;
  }

  /**
   * Gets the canonicalSourceFile.
   *
   * @return the canonicalSourceFile
   */
  @JsonIgnore
  public String getCanonicalSourceFile() {
    return canonicalSourceFile;
  }

  /**
   * Sets the canonicalSourceFile.
   *
   * @param canonicalSourceFile the canonicalSourceFile to set
   */
  public void setCanonicalSourceFile(final String canonicalSourceFile) {
    this.canonicalSourceFile = canonicalSourceFile;
  }

  /**
   * Gets the version.
   *
   * @return the version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version.
   *
   * @param version the version to set
   */
  public void setVersion(final String version) {
    this.version = version;
  }

  public List<AudienceContext> getAudience() {
    return audience;
  }

  public void setAudience(final List<AudienceContext> audience) {
    this.audience = audience;
  }

  public Map<String, List<String>> getDisplay() {
    return display;
  }

  public void setDisplay(final Map<String, List<String>> display) {
    this.display = display;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    sb.append("Content Object ID: " + this.id);
    sb.append(" Type: " + this.type);
    sb.append(" Source File: " + this.canonicalSourceFile);

    return sb.toString();
  }
}
