package com.umdc.directory.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

/// Parameters of a search of businesses, shared by the saved searches and the last search.
@MappedSuperclass
public class SearchParamsBase {

    @Column(name = "q", length = 100)
    private String q;
    @Column(name = "category_id")
    private UUID categoryId;
    @Column(name = "city_id")
    private UUID cityId;
    @Column(name = "city", length = 100)
    private String city;
    @Column(name = "lat")
    private Double lat;
    @Column(name = "lng")
    private Double lng;
    @Column(name = "radius_m")
    private Integer radiusM;
    @Column(name = "bbox", length = 100)
    private String bbox;
    @Column(name = "sort", length = 10)
    private String sort;

    public SearchParamsBase() {
        // JPA
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getCityId() {
        return cityId;
    }

    public void setCityId(UUID cityId) {
        this.cityId = cityId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLng() {
        return lng;
    }

    public void setLng(Double lng) {
        this.lng = lng;
    }

    public Integer getRadiusM() {
        return radiusM;
    }

    public void setRadiusM(Integer radiusM) {
        this.radiusM = radiusM;
    }

    public String getBbox() {
        return bbox;
    }

    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
