package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.preferences.DisplayPreferencesService;
import com.pricewatch.notifications.WatchRuleService;
import com.pricewatch.notifications.repository.ProductTargetEventRepository;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class SecondaryDisplayTest {
    private static final String MILK = "P000000124";
    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private DisplayPreferencesService preferences;
    @Autowired private WatchRuleService rules;
    @Autowired private TrackingService tracking;
    @Autowired private ProductTargetEventRepository occurrenceRows;

    @Test
    void secondaryPagesUseTraditionalChineseLabelsAndValidation() throws Exception {
        setup("zh-Hant");
        expectPage("/data", "已接受的商品目錄");
        expectPage("/planned/ask", "價格助手");
        expectPage("/personal-data", "儲存預算");
        expectPage("/saved", "鮮牛奶 236毫升");
        expectPage("/alerts", "記錄");
        expectPage("/alerts?tab=rules", "目前規則");
        expectPage("/alerts/rules?code=" + MILK, "鮮牛奶 236毫升");
        mvc.perform(post("/alerts/watch-rules/save").param("code", MILK)
                        .param("id", "invalid").param("condition", "time"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("請檢查表格欄位")));
    }

    @Test
    void secondaryPagesUseSimplifiedChineseLabelsAndValidation() throws Exception {
        setup("zh-Hans");
        expectPage("/data", "已接受的商品目录");
        expectPage("/planned/ask", "价格助手");
        expectPage("/personal-data", "保存预算");
        expectPage("/saved", "鲜牛奶 236毫升");
        expectPage("/alerts", "记录");
        expectPage("/alerts?tab=rules", "当前规则");
        expectPage("/alerts/rules?code=" + MILK, "鲜牛奶 236毫升");
        mvc.perform(post("/budget").param("period", "INVALID").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("请选择每周或每月预算。")));
    }

    @Test
    void aDueReminderRemainsInAlertHistoryAfterDismissal() throws Exception {
        setup("en");
        rules.save(MILK, null, null, LocalDateTime.now(ZoneId.of("Asia/Hong_Kong")).minusHours(1), "ONCE");
        expectPage("/alerts", "Time reminder");
        expectPage("/alerts", "Time reminder");
        assertEquals(1, occurrenceRows.count());
        long occurrence = occurrenceRows.findAll().getFirst().getId();
        mvc.perform(post("/alerts/product-events/" + occurrence + "/read"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/alerts"));
        expectPage("/alerts", "Read");
        assertEquals(1, occurrenceRows.count());
    }

    private void setup(String language) {
        catalog.importSample();
        preferences.update(language, true);
        tracking.save(MILK);
        tracking.startAll(MILK);
    }

    private void expectPage(String route, String label) throws Exception {
        mvc.perform(get(route))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(label)));
    }
}
