package cz.bsl.czh.favorites.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Mirrors front end {@code TaxDTO} (gisorch); not shared by design - keep in sync.
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
public class TaxDto {
    /**
     * The type of tax this amount and rate is.
     */
    private String taxId;
    /**
     * Amount of tax charged.
     */
    private Long taxAmount;
    /**
     * The tax percentage multiplied by 1000.
     */
    private Long taxRate;
    /**
     * Gross amount in cents.
     */
    private Long grossAmount;
    /**
     * Net amount in cents.
     */
    private Long netAmount;


    public String getTaxId() {
        return taxId;
    }
    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }
    public Long getTaxAmount() {
        return taxAmount;
    }
    public void setTaxAmount(Long taxAmount) {
        this.taxAmount = taxAmount;
    }
    public Long getTaxRate() {
        return taxRate;
    }
    public void setTaxRate(Long taxRate) {
        this.taxRate = taxRate;
    }

    public Long getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Long grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Long netAmount) {
        this.netAmount = netAmount;
    }
}
