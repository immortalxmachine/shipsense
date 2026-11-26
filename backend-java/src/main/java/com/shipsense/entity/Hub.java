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

    /** 2-letter Brazilian UF code used by the API, e.g. "SP", "RJ". */
    @Column(nullable = false, unique = true, length = 2)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    /** IBGE macro-region: N, NE, CO, SE or S. */
    @Column(nullable = false, length = 2)
    private String region;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal lng;

    protected Hub() { // required by JPA
    }

    public Hub(String code, String name, String city, String region,
               BigDecimal lat, BigDecimal lng) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.region = region;
        this.lat = lat;
        this.lng = lng;
    }

    public Integer getId()      { return id; }
    public String getCode()     { return code; }
    public String getName()     { return name; }
    public String getCity()     { return city; }
    public String getRegion()   { return region; }
    public BigDecimal getLat()  { return lat; }
    public BigDecimal getLng()  { return lng; }

    public void setCode(String code)       { this.code = code; }
    public void setName(String name)       { this.name = name; }
    public void setCity(String city)       { this.city = city; }
    public void setRegion(String region)   { this.region = region; }
    public void setLat(BigDecimal lat)     { this.lat = lat; }
    public void setLng(BigDecimal lng)     { this.lng = lng; }
}
