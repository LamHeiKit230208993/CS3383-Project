package com.pricewatch.web;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.preferences.DisplayPreferencesService;
import com.pricewatch.reminder.ReminderService;
import com.pricewatch.tracking.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:web;DB_CLOSE_DELAY=-1", "spring.main.web-application-type=servlet"})
@AutoConfigureMockMvc
@Transactional
class SecondaryDisplayTest {
    private static final String MILK = "P000000124";
    @Autowired private MockMvc mvc;
    @Autowired private CatalogService catalog;
    @Autowired private DisplayPreferencesService preferences;
    @Autowired private ReminderService reminders;
    @Autowired private TrackingService tracking;

    @Test
    void secondaryPagesUseTraditionalChineseLabelsAndValidation() throws Exception {
        setup("zh-Hant");
        expectPage("/data", "已接受的商品目錄");
        expectPage("/planned/ask", "價格助手");
        expectPage("/budget", "儲存預算");
        expectPage("/saved", "鮮牛奶 236毫升");
        expectPage("/alerts", "我的提示規則");
        expectPage("/reminders", "鮮牛奶 236毫升");
        mvc.perform(post("/reminders/create").param("code", MILK)
                        .param("quantity", "invalid").param("intervalDays", "7"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("請檢查表格欄位")));
    }

    @Test
    void secondaryPagesUseSimplifiedChineseLabelsAndValidation() throws Exception {
        setup("zh-Hans");
        expectPage("/data", "已接受的商品目录");
        expectPage("/planned/ask", "价格助手");
        expectPage("/budget", "保存预算");
        expectPage("/saved", "鲜牛奶 236毫升");
        expectPage("/alerts", "我的提醒规则");
        expectPage("/reminders", "鲜牛奶 236毫升");
        mvc.perform(post("/budget").param("period", "INVALID").param("limit", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("请选择每周或每月预算。")));
    }

    private void setup(String language) {
        catalog.importSample();
        preferences.update(language, true);
        tracking.save(MILK);
        tracking.startAll(MILK);
        reminders.create(MILK, 2, 7, "AEON");
    }

    private void expectPage(String route, String label) throws Exception {
        mvc.perform(get(route))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(label)));
    }
}
