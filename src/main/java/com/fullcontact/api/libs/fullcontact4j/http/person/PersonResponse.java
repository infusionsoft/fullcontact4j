package com.fullcontact.api.libs.fullcontact4j.http.person;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.*;
import com.fullcontact.api.libs.fullcontact4j.FullContactException;
import com.fullcontact.api.libs.fullcontact4j.http.FCResponse;
import com.fullcontact.api.libs.fullcontact4j.http.person.model.*;
import lombok.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PersonResponse extends FCResponse {
    private static ObjectMapper mapper = new ObjectMapper();
    private ContactInfo contactInfo = new ContactInfo();
    private Demographics demographics = new Demographics();
    private DigitalFootPrints digitalFootprint = new DigitalFootPrints();
    private List<Organization> organizations = Collections.emptyList();
    private List<Photo> photos = Collections.emptyList();
    private double likelihood;
    private List<SocialProfile> socialProfiles = Collections.emptyList();
    private Macromeasures macromeasures = new Macromeasures(Collections.<Macromeasures.Interest>emptyList());
    private String requestId;
    private String message;

    /**
     * Gets the social profile of a certain type
     * @return the SocialProfile, or null if it doesn't exist.
     */
    public SocialProfile getSocialProfile(String typeId) {
        for(SocialProfile p : socialProfiles) {
            if(typeId.equals(p.getTypeId())) {
                return p;
            }
        }
        return null;
    }

    /**
     * Factory method to create a webhook response from json.
     * @param json
     * @return a new PersonResponse represented by the Json string
     * @throws com.fullcontact.api.libs.fullcontact4j.FullContactException if there is a parsing/mapping error.
     */
    public static PersonResponse fromJson(String json) throws FullContactException {
        //Properties not present in the POJO are ignored instead of throwing exceptions
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //An empty string ("") is interpreted as null
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        try {
            return mapper.readValue(json, PersonResponse.class);
        } catch(JsonMappingException e) {
            throw new FullContactException("Failed to convert person json to a response", e);
        } catch(JsonParseException e) {
            throw new FullContactException("Json is not valid format", e);
        } catch(IOException e) {
            throw new FullContactException("Unexpected exception when parsing json", e);
        }
    }

}


