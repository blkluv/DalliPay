//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.3.1 generiert 
// Siehe <a href="https://javaee.github.io/jaxb-v2/">https://javaee.github.io/jaxb-v2/</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2023.05.04 um 04:37:51 PM CEST 
//


package com.radynamics.dallipay.iso20022.camt054.camt05300108.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java-Klasse für DateAndPlaceOfBirth1 complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType name="DateAndPlaceOfBirth1"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="BirthDt" type="{urn:iso:std:iso:20022:tech:xsd:camt.053.001.08}ISODate"/&gt;
 *         &lt;element name="PrvcOfBirth" type="{urn:iso:std:iso:20022:tech:xsd:camt.053.001.08}Max35Text" minOccurs="0"/&gt;
 *         &lt;element name="CityOfBirth" type="{urn:iso:std:iso:20022:tech:xsd:camt.053.001.08}Max35Text"/&gt;
 *         &lt;element name="CtryOfBirth" type="{urn:iso:std:iso:20022:tech:xsd:camt.053.001.08}CountryCode"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DateAndPlaceOfBirth1", propOrder = {
    "birthDt",
    "prvcOfBirth",
    "cityOfBirth",
    "ctryOfBirth"
})
public class DateAndPlaceOfBirth1 {

    @XmlElement(name = "BirthDt", required = true)
    @XmlSchemaType(name = "date")
    protected XMLGregorianCalendar birthDt;
    @XmlElement(name = "PrvcOfBirth")
    protected String prvcOfBirth;
    @XmlElement(name = "CityOfBirth", required = true)
    protected String cityOfBirth;
    @XmlElement(name = "CtryOfBirth", required = true)
    protected String ctryOfBirth;

    /**
     * Ruft den Wert der birthDt-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getBirthDt() {
        return birthDt;
    }

    /**
     * Legt den Wert der birthDt-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setBirthDt(XMLGregorianCalendar value) {
        this.birthDt = value;
    }

    /**
     * Ruft den Wert der prvcOfBirth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPrvcOfBirth() {
        return prvcOfBirth;
    }

    /**
     * Legt den Wert der prvcOfBirth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPrvcOfBirth(String value) {
        this.prvcOfBirth = value;
    }

    /**
     * Ruft den Wert der cityOfBirth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCityOfBirth() {
        return cityOfBirth;
    }

    /**
     * Legt den Wert der cityOfBirth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCityOfBirth(String value) {
        this.cityOfBirth = value;
    }

    /**
     * Ruft den Wert der ctryOfBirth-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCtryOfBirth() {
        return ctryOfBirth;
    }

    /**
     * Legt den Wert der ctryOfBirth-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCtryOfBirth(String value) {
        this.ctryOfBirth = value;
    }

}
