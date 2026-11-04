package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors front end {@code TicketSecurityDTO} (gisorch); not shared by design - keep in sync.
 * Favorites extensions: unknown JSON input properties are ignored; null fields
 * are omitted on output (sparse wire shape, so a legacy body round-trips unchanged).
 *
 * <p>Grep anchor: favorites
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketSecurityDto {
    /**
     * The input SureLock document token encoded as a hexadecimal string.
     */
    private String docToken;
    /**
     * The input SureLock secure document token encoded as a hexadecimal string.
     */
    private String secureDocToken;
    /**
     * The output SureLock key modifier encoded as a hexadecimal string.
     */
    private String keyModifier;
    /**
     * Ticket security type.  For example SureLock or NASPL ECN
     */
    private String type;

    public String getDocToken() {
        return docToken;
    }
    public void setDocToken(String docToken) {
        this.docToken = docToken;
    }
    public String getSecureDocToken() {
        return secureDocToken;
    }
    public void setSecureDocToken(String secureDocToken) {
        this.secureDocToken = secureDocToken;
    }
    public String getKeyModifier() {
        return keyModifier;
    }
    public void setKeyModifier(String keyModifier) {
        this.keyModifier = keyModifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
