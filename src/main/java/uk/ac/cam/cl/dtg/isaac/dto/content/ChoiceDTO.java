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
package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;

/**
 * Choice object The choice object is a specialized form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
public class ChoiceDTO extends ContentDTO {

    private boolean correct;
    private ContentBaseDTO explanation;

    /**
     * Default Constructor required for mappers.
     */
    public ChoiceDTO() {

    }

    public boolean isCorrect() {return correct;}

    public void setCorrect(final boolean correct) {
        this.correct = correct;
    }

    public ContentBaseDTO getExplanation() {return explanation;}

    public void setExplanation(final ContentBaseDTO explanation) {
        this.explanation = explanation;
    }
}
