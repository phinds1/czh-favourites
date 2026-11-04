package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors front end {@code IdDocumentDTO} (gisorch); not shared by design - keep in sync.
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
public class IdDocumentDto {
    /**
     * Id number.
     */
    private String idNumber;
    /**
     * First name or names.
     */
    private String firstName;
    /**
     * Date of birth in YYYYMMDD format.
     */
    private String dateOfBirth;
    /**
     * City born in.
     */
    private String cityOfBirth;
    /**
     * Administrative region born in.
     */
    private String departmentOfBirth;
    /**
     * Phone number.
     */
    private String phoneNumber;
    /**
     * Device or location, such as terminal, where document was scanned.
     */
    private String origin;
    /**
     * The document type scanned.
     */
    private String documentType;
    /**
     * Date document was issued in YYYYMMDD format.
     */
    private String documentIssueDate;
    /**
     * Location or place document was issued.
     */
    private String documentIssueLocation;
    /**
     * Name of the document issuer;
     */
    private String documentIssuer;
    /**
     * The quality.
     */
    private String qualityOfIssuing;
    /**
     * If true, scanned information was modified by the retailer.
     */
    private Boolean modified;
    /**
     * Encrypted name.
     */
    private String encryptedName;
    /**
     * Encrypted address.
     */
    private String encryptedAddress;
    /**
     * Encrypted email address.
     */
    private String encryptedEmail;
    /**
     * Id of the decryption key to use with the encrypted fields.
     */
    private String decryptionKeyId;
    /**
     * Hash of names.
     */
    private String hash;

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCityOfBirth() {
        return cityOfBirth;
    }

    public void setCityOfBirth(String cityOfBirth) {
        this.cityOfBirth = cityOfBirth;
    }

    public String getDepartmentOfBirth() {
        return departmentOfBirth;
    }

    public void setDepartmentOfBirth(String departmentOfBirth) {
        this.departmentOfBirth = departmentOfBirth;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentIssueDate() {
        return documentIssueDate;
    }

    public void setDocumentIssueDate(String documentIssueDate) {
        this.documentIssueDate = documentIssueDate;
    }

    public String getDocumentIssueLocation() {
        return documentIssueLocation;
    }

    public void setDocumentIssueLocation(String documentIssueLocation) {
        this.documentIssueLocation = documentIssueLocation;
    }

    public String getDocumentIssuer() {
        return documentIssuer;
    }

    public void setDocumentIssuer(String documentIssuer) {
        this.documentIssuer = documentIssuer;
    }

    public String getQualityOfIssuing() {
        return qualityOfIssuing;
    }

    public void setQualityOfIssuing(String qualityOfIssuing) {
        this.qualityOfIssuing = qualityOfIssuing;
    }

    public Boolean getModified() {
        return modified;
    }

    public void setModified(Boolean modified) {
        this.modified = modified;
    }

    public String getEncryptedName() {
        return encryptedName;
    }

    public void setEncryptedName(String encryptedName) {
        this.encryptedName = encryptedName;
    }

    public String getEncryptedAddress() {
        return encryptedAddress;
    }

    public void setEncryptedAddress(String encryptedAddress) {
        this.encryptedAddress = encryptedAddress;
    }

    public String getEncryptedEmail() {
        return encryptedEmail;
    }

    public void setEncryptedEmail(String encryptedEmail) {
        this.encryptedEmail = encryptedEmail;
    }

    public String getDecryptionKeyId() {
        return decryptionKeyId;
    }

    public void setDecryptionKeyId(String decryptionKeyId) {
        this.decryptionKeyId = decryptionKeyId;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
}
