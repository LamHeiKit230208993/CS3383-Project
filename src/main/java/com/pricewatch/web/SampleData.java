package com.pricewatch.web;

import java.util.List;

/**
 * Static sample data for the UI prototype. No database, import, or live data.
 * Every value here is deliberately fake and labelled as sample data in the UI.
 */
public final class SampleData {

    private SampleData() {
    }

    public record Listing(String retailer, String code, String price, String offer, String state) {
    }

    public record Product(String code, String category, String brand, String name, String pack,
                          String nameZhHant, String nameZhHans, String bestPrice, String bestRetailer,
                          List<Listing> listings) {
    }

    public record HistoryPoint(String date, String price, int height) {
    }

    public record AlertItem(String name, String target, String price, String state, String detail) {
    }

    public record ListItem(String name, String brand, String pack, int quantity, String price,
                           String retailer, boolean checked, String note) {
    }

    public record SavedItem(String name, String detail, String price, String state) {
    }

    public static final Product MILK = new Product(
            "P000000192", "Dairy & Eggs", "Garden", "Fresh Milk 946ml", "946ml carton",
            "鮮牛奶 946毫升", "鲜牛奶 946毫升", "40.00", "AEON",
            List.of(
                    new Listing("AEON", "AEON", "40.00", "Buy 2 to save $4.00", "best"),
                    new Listing("PARKnSHOP", "PARKNSHOP", "45.00", "—", "tie"),
                    new Listing("JASONS", "JASONS", "45.00", "—", "tie"),
                    new Listing("WELLCOME", "WELLCOME", "45.50", "2 for $14.00", "listed"),
                    new Listing("WATSONS", "WATSONS", "47.00", "—", "listed"),
                    new Listing("LUNGFUNG", "LUNGFUNG", "—", "No listing in this snapshot", "missing")
            )
    );

    public static final Product RICE = new Product(
            "P000000311", "Grocery staples", "Golden", "Rice 5kg", "5kg bag",
            "香米 5公斤", "香米 5公斤", "58.00", "WELLCOME",
            List.of(
                    new Listing("WELLCOME", "WELLCOME", "58.00", "—", "best"),
                    new Listing("PARKnSHOP", "PARKNSHOP", "62.00", "—", "listed"),
                    new Listing("AEON", "AEON", "64.50", "—", "listed")
            )
    );

    public static final Product EGGS = new Product(
            "P000000208", "Dairy & Eggs", "Farm Fresh", "Eggs 30s", "30 eggs",
            "鮮雞蛋 30隻", "鲜鸡蛋 30只", "72.00", "PARKnSHOP",
            List.of(
                    new Listing("PARKnSHOP", "PARKNSHOP", "72.00", "—", "best"),
                    new Listing("WELLCOME", "WELLCOME", "74.00", "—", "listed"),
                    new Listing("AEON", "AEON", "76.00", "—", "listed")
            )
    );

    public static final Product DETERGENT = new Product(
            "P000000455", "Household", "CleanHome", "Laundry Detergent 3L", "3L bottle",
            "洗衣液 3公升", "洗衣液 3公升", "95.00", "WATSONS",
            List.of(
                    new Listing("WATSONS", "WATSONS", "95.00", "—", "best"),
                    new Listing("MANNINGS", "MANNINGS", "98.00", "—", "listed"),
                    new Listing("PARKnSHOP", "PARKNSHOP", "101.00", "—", "listed")
            )
    );

    public static final List<Product> PRODUCTS = List.of(MILK, RICE, EGGS, DETERGENT);

    public static final List<ListItem> LIST_ITEMS = List.of(
            new ListItem("Fresh Milk 946ml", "Garden", "946ml carton", 2, "40.00", "AEON", true, "Cheapest at AEON"),
            new ListItem("Rice 5kg", "Golden", "5kg bag", 1, "58.00", "WELLCOME", false, "Cheapest at WELLCOME"),
            new ListItem("Eggs 30s", "Farm Fresh", "30 eggs", 1, "72.00", "PARKnSHOP", false, "Cheapest at PARKnSHOP"),
            new ListItem("Laundry Detergent 3L", "CleanHome", "3L bottle", 1, "95.00", "WATSONS", false, "Cheapest at WATSONS"),
            new ListItem("Bananas", "Fresh", "2 bunches", 2, "28.00", "AEON", false, "Cheapest at AEON")
    );

    public static final List<SavedItem> WISHLIST = List.of(
            new SavedItem("Olive Oil 1L", "Saved 3 days ago", "68.00", "Wishlist"),
            new SavedItem("Toilet Paper 10-pack", "Saved last week", "54.00", "Wishlist")
    );

    public static final List<SavedItem> TRACKING = List.of(
            new SavedItem("Fresh Milk 946ml", "Tracked for 30 days · 9 sample points", "40.00", "Tracking"),
            new SavedItem("Rice 5kg", "Tracked for 14 days · 5 sample points", "58.00", "Tracking")
    );

    public static final List<AlertItem> ALERTS = List.of(
            new AlertItem("Fresh Milk 946ml", "Target: below $45.00", "40.00", "fired",
                    "Target met · earlier sample price $45.00"),
            new AlertItem("Rice 5kg", "Drop: 5% or more", "58.00", "watching",
                    "Earlier sample price $58.00 · no drop yet"),
            new AlertItem("Eggs 30s", "Target: below $70.00", "72.00", "watching",
                    "Watching · not yet hit")
    );

    public static final List<HistoryPoint> HISTORY = List.of(
            new HistoryPoint("24 Sep", "42.00", 55),
            new HistoryPoint("27 Sep", "43.00", 62),
            new HistoryPoint("30 Sep", "41.00", 50),
            new HistoryPoint("3 Oct", "45.00", 70),
            new HistoryPoint("6 Oct", "44.00", 64),
            new HistoryPoint("9 Oct", "41.00", 48),
            new HistoryPoint("12 Oct", "40.00", 34),
            new HistoryPoint("15 Oct", "43.00", 58),
            new HistoryPoint("18 Oct", "41.00", 50),
            new HistoryPoint("Today", "40.00", 34)
    );
}
