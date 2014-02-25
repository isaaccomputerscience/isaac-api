package uk.ac.cam.cl.dtg.segue.dto;

import java.util.List;

import org.bson.types.ObjectId;

import uk.ac.cam.cl.dtg.rspp.models.JsonType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Choice object
 * The choice object is a specialized form of content and allows the storage of data relating to possible answers to questions. 
 *
 */
@JsonType("question")
public class Question extends Content {

	protected ContentBase answer;
	protected List<ContentBase> hints;
	
	@JsonCreator
	public Question(@JsonProperty("_id") String _id,
			       @JsonProperty("id") String id, 
				   @JsonProperty("title") String title, 
				   @JsonProperty("type") String type, 
				   @JsonProperty("author") String author,
				   @JsonProperty("encoding") String encoding,
				   @JsonProperty("src") String src,
				   @JsonProperty("layout") String layout,
				   @JsonProperty("contentReferenced") List<String> children,
				   @JsonProperty("contentLiteral") String value,
				   @JsonProperty("attribution") String attribution,
				   @JsonProperty("relatedContent") List<String> relatedContent,
				   @JsonProperty("version") int version,
				   @JsonProperty("answer") ContentBase answer,
				   @JsonProperty("hints") List<ContentBase> hints) {
		super(_id, 
		      id, 
		      title, 
		      type, 
		      author, 
		      encoding, 
		      src, 
		      layout, 
		      children, 
		      value, 
		      attribution, 
		      relatedContent, 
		      version);
		
		this.answer = answer;
		this.hints = hints;
	}

	public Question(){
		super();
	}
	
	public ContentBase getAnswer() {
		return answer;
	}

	public List<ContentBase> getHints() {
		return hints;
	}

}
