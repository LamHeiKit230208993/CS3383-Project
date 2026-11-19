package com.pricewatch.ask;

import com.pricewatch.catalog.CatalogService;
import com.pricewatch.catalog.ImportStatus;
import com.pricewatch.catalog.ListingView;
import com.pricewatch.catalog.LocalizedText;
import com.pricewatch.catalog.ProductView;
import com.pricewatch.i18n.AppMessages;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieval and price facts are deterministic. The model is only asked after a
 * single accepted product has been selected, and its currency amounts are
 * checked against the facts before its answer is shown.
 */
@Service
public class AskService {
    private static final int MAX_QUESTION_CHARACTERS = 500;
    private static final int MAX_CHOICES = 20;
    private static final Pattern CURRENCY_AMOUNT =
            Pattern.compile("(?i)(?:HK\\s*\\$|\\$)\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)");

    private final CatalogService catalog;
    private final AiAnswerClient ai;

    public AskService(CatalogService catalog, AiAnswerClient ai) {
        this.catalog = catalog;
        this.ai = ai;
    }

    @Transactional(readOnly = true)
    public AskResult ask(String rawQuestion, String productCode) {
        String question = rawQuestion == null ? "" : rawQuestion.strip();
        if (question.isEmpty()) {
            throw new IllegalArgumentException(AppMessages.text("ask.blank"));
        }
        if (question.length() > MAX_QUESTION_CHARACTERS) {
            throw new IllegalArgumentException(AppMessages.text("ask.tooLong", MAX_QUESTION_CHARACTERS));
        }

        List<ProductView> products = catalog.searchResults("", "").matches();
        ProductView selected;
        if (productCode != null && !productCode.isBlank()) {
            selected = products.stream()
                    .filter(product -> product.code().equals(productCode.strip()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(AppMessages.text("ask.selection")));
        } else {
            List<ProductView> matches = strongestMatches(products, question);
            if (matches.isEmpty()) {
                return AskResult.noMatch(question);
            }
            if (matches.size() > 1) {
                return AskResult.choose(question, matches.stream().limit(MAX_CHOICES)
                        .map(AskService::candidate).toList());
            }
            selected = matches.getFirst();
        }

        AskFacts facts = factsFor(selected, question);
        try {
            String answer = ai.answer(facts.asMap(), question);
            if (amountsMatch(answer, facts)) {
                return AskResult.answered(question, facts, answer, false);
            }
        } catch (RuntimeException ignored) {
            // A model failure must leave the checked facts and normal comparison usable.
        }
        return AskResult.answered(question, facts, fallbackAnswer(facts), true);
    }

    private static List<ProductView> strongestMatches(List<ProductView> products, String question) {
        String normalizedQuestion = normalize(question);
        int bestScore = 0;
        List<ProductView> best = new ArrayList<>();
        for (ProductView product : products) {
            int score = score(product, normalizedQuestion);
            if (score == 0) {
                continue;
            }
            if (score > bestScore) {
                bestScore = score;
                best.clear();
                best.add(product);
            } else if (score == bestScore) {
                best.add(product);
            }
        }
        return best;
    }

    private static int score(ProductView product, String question) {
        String code = normalize(product.code());
        if (!code.isEmpty() && question.contains(code)) {
            return 1000;
        }
        int score = 0;
        Set<String> tokens = new HashSet<>();
        for (LocalizedText text : List.of(product.name(), product.brand(),
                product.category2(), product.category3())) {
            for (String value : new String[]{text.en(), text.zhHant(), text.zhHans()}) {
                String label = normalize(value);
                if (label.isEmpty()) {
                    continue;
                }
                if (question.contains(label)) {
                    score += 60;
                }
                for (String token : label.split("[^\\p{L}\\p{N}]+")) {
                    if (isUsefulToken(token)) {
                        tokens.add(token);
                    }
                }
            }
        }
        for (String token : tokens) {
            if (question.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private static boolean isUsefulToken(String token) {
        return token.length() >= 2 || token.codePoints().anyMatch(AskService::isCjk);
    }

    private static boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    private static AskResult.Candidate candidate(ProductView product) {
        Locale locale = LocaleContextHolder.getLocale();
        return new AskResult.Candidate(product.code(), product.name().textFor(locale),
                product.brand().textFor(locale), product.category2().textFor(locale));
    }

    private AskFacts factsFor(ProductView product, String question) {
        Locale locale = LocaleContextHolder.getLocale();
        List<AskFacts.PriceFact> prices = product.listings().stream()
                .map(listing -> new AskFacts.PriceFact(listing.retailerCode(), listing.price()))
                .toList();
        List<String> lowestRetailers = product.lowestListings().stream().map(ListingView::retailerCode).toList();
        ImportStatus status = catalog.status();
        return new AskFacts(product.code(), product.name().textFor(locale),
                product.brand().textFor(locale), product.category2().textFor(locale),
                prices, product.lowestPrice(), lowestRetailers,
                status == null ? null : status.retrievedAt(), question);
    }

    private static String fallbackAnswer(AskFacts facts) {
        if (facts.lowestPrice() == null) {
            return AppMessages.text("ask.fallback.unpriced", facts.productName());
        }
        String retailers = String.join(", ", new LinkedHashSet<>(facts.lowestRetailers()));
        return AppMessages.text("ask.fallback.priced", facts.productName(),
                money(facts.lowestPrice()), retailers);
    }

    private static boolean amountsMatch(String answer, AskFacts facts) {
        if (answer == null || answer.isBlank()) {
            return false;
        }
        List<BigDecimal> allowed = facts.knownPrices();
        Matcher matcher = CURRENCY_AMOUNT.matcher(answer);
        while (matcher.find()) {
            BigDecimal amount;
            try {
                amount = new BigDecimal(matcher.group(1).replace(",", ""));
            } catch (NumberFormatException exception) {
                return false;
            }
            if (allowed.stream().noneMatch(price -> price.compareTo(amount) == 0)) {
                return false;
            }
        }
        return true;
    }

    private static String money(BigDecimal value) {
        var format = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-HK"));
        format.setCurrency(Currency.getInstance("HKD"));
        format.setMaximumFractionDigits(Math.max(2, value.stripTrailingZeros().scale()));
        return format.format(value);
    }
}
