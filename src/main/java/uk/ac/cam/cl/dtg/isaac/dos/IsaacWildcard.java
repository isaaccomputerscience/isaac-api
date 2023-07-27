/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacWildcardDTO;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;

import java.util.List;
import java.util.Set;

/**
 * IsaacWildcard Represents gameboard advertising space.
 */
@JsonContentType("isaacWildcard")
@DTOMapping(IsaacWildcardDTO.class)
public class IsaacWildcard extends Content {
    protected String description;
    protected String url;

    @JsonCreator
    public IsaacWildcard(
            @JsonProperty("id") final String id,
            @JsonProperty("title") final String title,
            @JsonProperty("subtitle") final String subtitle,
            @JsonProperty("type") final String type,
            @JsonProperty("author") final String author,
            @JsonProperty("encoding") final String encoding,
            @JsonProperty("canonicalSourceFile") final String canonicalSourceFile,
            @JsonProperty("layout") final String layout,
            @JsonProperty("children") final List<ContentBase> children,
            @JsonProperty("value") final String value,
            @JsonProperty("attribution") final String attribution,
            @JsonProperty("relatedContent") final List<String> relatedContent,
            @JsonProperty("published") final boolean published,
            @JsonProperty("deprecated") final Boolean deprecated,
            @JsonProperty("tags") final Set<String> tags,
            @JsonProperty("level") final Integer level,
            @JsonProperty("description") final String description,
            @JsonProperty("url") final String url) {
        super(id, title, subtitle, type, author, encoding, canonicalSourceFile, layout, children, value, attribution,
                relatedContent, published, deprecated, tags, level);

        this.description = description;
        this.url = url;
    }

    /**
     * Default constructor required for Jackson.
     */
    public IsaacWildcard() {

    }

    /**
     * getDescription.
     * 
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     *            of the wildcard
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * getUrl.
     * 
     * @return url
     */
    public String getUrl() {
        // It appears as though sometimes urls are provided with trailing spaces in git...
        // I do not know why...
        if (url != null) {
            return url.trim();
        } else {
            return null;
        }
    }

    /**
     * @param url
     *            - navigation url
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IsaacWildcard [description=");
        builder.append(description);
        builder.append(", url=");
        builder.append(url);
        builder.append(", title=");
        builder.append(title);
        builder.append(", subtitle=");
        builder.append(subtitle);
        builder.append(", author=");
        builder.append(author);
        builder.append(", encoding=");
        builder.append(encoding);
        builder.append(", layout=");
        builder.append(layout);
        builder.append(", children=");
        builder.append(children);
        builder.append(", value=");
        builder.append(value);
        builder.append(", attribution=");
        builder.append(attribution);
        builder.append(", relatedContent=");
        builder.append(relatedContent);
        builder.append(", published=");
        builder.append(published);
        builder.append(", level=");
        builder.append(level);
        builder.append(", id=");
        builder.append(id);
        builder.append(", type=");
        builder.append(type);
        builder.append(", tags=");
        builder.append(tags);
        builder.append(", canonicalSourceFile=");
        builder.append(canonicalSourceFile);
        builder.append(", version=");
        builder.append(version);
        builder.append("]");
        return builder.toString();
    }
}
