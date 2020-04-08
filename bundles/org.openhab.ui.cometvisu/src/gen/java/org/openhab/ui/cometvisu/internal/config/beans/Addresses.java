//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.2 
// See <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2020.04.08 at 06:07:32 PM CEST 
//


package org.openhab.ui.cometvisu.internal.config.beans;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for addresses complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="addresses"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence maxOccurs="unbounded"&gt;
 *         &lt;element name="address" type="{}address"/&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="value-mapping" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="address-mapping" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "addresses", propOrder = {
    "address"
})
public class Addresses {

    @XmlElement(required = true)
    protected List<Address> address;
    @XmlAttribute(name = "value-mapping")
    protected String valueMapping;
    @XmlAttribute(name = "address-mapping")
    protected String addressMapping;

    /**
     * Gets the value of the address property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the address property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAddress().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Address }
     * 
     * 
     */
    public List<Address> getAddress() {
        if (address == null) {
            address = new ArrayList<Address>();
        }
        return this.address;
    }

    /**
     * Gets the value of the valueMapping property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getValueMapping() {
        return valueMapping;
    }

    /**
     * Sets the value of the valueMapping property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setValueMapping(String value) {
        this.valueMapping = value;
    }

    /**
     * Gets the value of the addressMapping property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAddressMapping() {
        return addressMapping;
    }

    /**
     * Sets the value of the addressMapping property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAddressMapping(String value) {
        this.addressMapping = value;
    }

}