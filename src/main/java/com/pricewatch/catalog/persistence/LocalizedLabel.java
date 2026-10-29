package com.pricewatch.catalog.persistence;

import jakarta.persistence.Embeddable;

@Embeddable
public class LocalizedLabel {
    private String en;
    private String zhHant;
    private String zhHans;

    protected LocalizedLabel() {
    }

    public LocalizedLabel(String en, String zhHant, String zhHans) {
        this.en = en;
        this.zhHant = zhHant;
        this.zhHans = zhHans;
    }

    public String getEn() { return en; }
    public void setEn(String en) { this.en = en; }
    public String getZhHant() { return zhHant; }
    public void setZhHant(String zhHant) { this.zhHant = zhHant; }
    public String getZhHans() { return zhHans; }
    public void setZhHans(String zhHans) { this.zhHans = zhHans; }
}
