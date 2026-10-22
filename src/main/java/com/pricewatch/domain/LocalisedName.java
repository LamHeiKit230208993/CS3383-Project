package com.pricewatch.domain;

import jakarta.persistence.Embeddable;

import java.util.List;

@Embeddable
public class LocalisedName {

    private String en;
    private String zhHant;
    private String zhHans;

    protected LocalisedName() {
    }

    public LocalisedName(String en, String zhHant, String zhHans) {
        this.en = en == null ? "" : en;
        this.zhHant = zhHant == null ? "" : zhHant;
        this.zhHans = zhHans == null ? "" : zhHans;
    }

    public String getEn() {
        return en;
    }

    public String getZhHant() {
        return zhHant;
    }

    public String getZhHans() {
        return zhHans;
    }

    public String text(String lang) {
        return "zh-Hant".equals(lang) ? zhHant : "zh-Hans".equals(lang) ? zhHans : en;
    }

    public List<String> all() {
        return List.of(en, zhHant, zhHans);
    }
}
