package com.shipsense.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * A sorting facility — a node in the route graph.
 * Maps to the {@code hubs} table.
 */
@Entity
@Table(name = "hubs")
public class Hub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Short human code used by the API, e.g. "CHN", "DEL". */
    @Column(nullable = false, unique = true, length = 3)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    protected Hub() { // required by JPA
    }

    public Hub(String code, String name, String city, BigDecimal lat, BigDecimal lng) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.lat = lat;
        this.lng = lng;
    }

    public Integer getId()      { return id; }
    public String getCode()     { return code; }
    public String getName()     { return name; }
    public String getCity()     { return city; }
    public BigDecimal getLat()  { return lat; }
    public BigDecimal getLng()  { return lng; }

    public void setCode(String code)   { this.code = code; }
    public void setName(String name)   { this.name = name; }
    public void setCity(String city)   { this.city = city; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
}
