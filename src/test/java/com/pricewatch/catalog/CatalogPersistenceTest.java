package com.pricewatch.catalog;

import com.pricewatch.PricewatchApplication;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.shopping.persistence.ShoppingListEntity;
import com.pricewatch.shopping.persistence.ShoppingListItemEntity;
import com.pricewatch.shopping.repository.ShoppingListItemRepository;
import com.pricewatch.shopping.repository.ShoppingListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogPersistenceTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void productsAndShoppingListSurviveDatabaseReopen() {
        String jdbcUrl = "jdbc:h2:file:" + temporaryDirectory.resolve("reopen-db");

        try (ConfigurableApplicationContext firstRun = start(jdbcUrl)) {
            ProductEntity product = new ProductEntity("P000000124");
            product.setBrand(new LocalizedLabel("Kowloon Dairy", "維記", "维记"));
            product.setName(new LocalizedLabel("Fresh Milk 236mL", "鮮牛奶 236毫升", "鲜牛奶 236毫升"));
            product.setCategory1(new LocalizedLabel("Dairy", "奶類", "奶类"));
            product.setCategory2(new LocalizedLabel("Milk", "牛奶", "牛奶"));
            product.setCategory3(new LocalizedLabel("Fresh milk", "鮮奶", "鲜奶"));
            product.setActive(true);
            firstRun.getBean(ProductRepository.class).saveAndFlush(product);

            ShoppingListEntity list = firstRun.getBean(ShoppingListRepository.class).saveAndFlush(
                    new ShoppingListEntity("My list"));
            firstRun.getBean(ShoppingListItemRepository.class).saveAndFlush(
                    new ShoppingListItemEntity(list, product, 3, true, "JASONS"));
        }

        try (ConfigurableApplicationContext reopened = start(jdbcUrl)) {
            ProductEntity savedProduct = reopened.getBean(ProductRepository.class)
                    .findById("P000000124").orElseThrow();
            List<ShoppingListItemEntity> items = reopened.getBean(ShoppingListItemRepository.class)
                    .findAllByShoppingList_NameOrderByIdAsc("My list");

            assertEquals("鮮牛奶 236毫升", savedProduct.getName().getZhHant());
            assertEquals(1, items.size());
            assertEquals(3, items.getFirst().getQuantity());
            assertEquals(true, items.getFirst().isChecked());
            assertEquals("P000000124", items.getFirst().getProductCode());
            assertEquals("JASONS", items.getFirst().getSelectedRetailerCode());
        }
    }

    private ConfigurableApplicationContext start(String jdbcUrl) {
        return new SpringApplicationBuilder(PricewatchApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=" + jdbcUrl,
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=validate",
                        "--spring.main.banner-mode=off",
                        "--logging.level.root=ERROR");
    }
}
