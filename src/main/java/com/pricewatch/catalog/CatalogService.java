package com.pricewatch.catalog;

import com.pricewatch.catalog.importer.CatalogJsonParser;
import com.pricewatch.catalog.importer.CatalogReplacementService;
import com.pricewatch.catalog.importer.ConsumerCouncilSourceClient;
import com.pricewatch.catalog.importer.ImportedCatalog;
import com.pricewatch.catalog.persistence.AcceptedImportEntity;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class CatalogService {
    private static final String SAMPLE_RESOURCE = "/samples/pricewatch-products.json";
    private static final int MAX_SAMPLE_BYTES = 10 * 1024 * 1024;

    private final ProductRepository productRepository;
    private final AcceptedImportRepository importRepository;
    private final CatalogJsonParser parser;
    private final CatalogReplacementService replacementService;
    private final ConsumerCouncilSourceClient sourceClient;

    public CatalogService(ProductRepository productRepository,
                          AcceptedImportRepository importRepository,
                          CatalogJsonParser parser,
                          CatalogReplacementService replacementService,
                          ConsumerCouncilSourceClient sourceClient) {
        this.productRepository = productRepository;
        this.importRepository = importRepository;
        this.parser = parser;
        this.replacementService = replacementService;
        this.sourceClient = sourceClient;
    }

    @Transactional(readOnly = true)
    public List<ProductView> search(String query) {
        SearchResults results = searchResults(query, "");
        return Stream.concat(results.matches().stream(), results.related().stream()).toList();
    }

    @Transactional(readOnly = true)
    public SearchResults searchResults(String query, String category) {
        List<ProductEntity> products = productRepository.findAllByActiveTrue();
        List<String> categories = products.stream()
                .map(ProductEntity::getCategory2)
                .filter(Objects::nonNull)
                .map(LocalizedLabel::getEn)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(Comparator.comparing(CatalogService::normalize).thenComparing(Comparator.naturalOrder()))
                .toList();

        String normalizedQuery = normalize(query);
        List<String> terms = normalizedQuery.isEmpty() ? List.of() : List.of(normalizedQuery.split("\\s+"));
        String selectedCategory = normalize(category);
        List<SearchHit> matches = new ArrayList<>();
        List<SearchHit> related = new ArrayList<>();

        for (ProductEntity product : products) {
            if (!selectedCategory.isEmpty()
                    && !selectedCategory.equals(normalize(product.getCategory2() == null
                    ? "" : product.getCategory2().getEn()))) {
                continue;
            }
            SearchHit hit = rank(product, normalizedQuery, terms);
            if (hit == null) {
                continue;
            }
            (hit.related() ? related : matches).add(hit);
        }

        Comparator<SearchHit> ranking = Comparator.comparing(SearchHit::exactCode, Comparator.reverseOrder())
                .thenComparing(SearchHit::exactName, Comparator.reverseOrder())
                .thenComparing(SearchHit::phraseName, Comparator.reverseOrder())
                .thenComparing(SearchHit::nameTokenCount, Comparator.reverseOrder())
                .thenComparing(SearchHit::score, Comparator.reverseOrder())
                .thenComparing(SearchHit::code);
        matches.sort(ranking);
        related.sort(ranking);

        return new SearchResults(matches.stream().map(hit -> ProductView.from(hit.product())).toList(),
                related.stream().map(hit -> ProductView.from(hit.product())).toList(), categories);
    }

    @Transactional(readOnly = true)
    public ProductView find(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("A product code is required.");
        }
        return productRepository.findWithListingsByCode(code)
                .map(ProductView::from)
                .orElseThrow(() -> new IllegalArgumentException("Product " + code + " was not found."));
    }

    @Transactional(readOnly = true)
    public ImportStatus status() {
        return importRepository.findById(1L).map(CatalogService::toStatus).orElse(null);
    }

    public ImportStatus importSample() {
        try (InputStream input = new ClassPathResource(SAMPLE_RESOURCE).getInputStream()) {
            byte[] bytes = readLimited(input, MAX_SAMPLE_BYTES);
            return accept(bytes, "SAMPLE", "https://online-price-watch.consumer.org.hk/opw/opendata/pricewatch.json");
        } catch (IOException exception) {
            throw new CatalogImportException("Could not read the checked sample file.", exception);
        }
    }

    public ImportStatus refreshLive() {
        return accept(sourceClient.fetch(), "LIVE", sourceClient.sourceUrl());
    }

    ImportStatus accept(byte[] bytes, String origin, String sourceUrl) {
        ImportedCatalog parsed = parser.parse(bytes);
        Instant retrievedAt = Instant.now();
        String checksum = sha256(bytes);
        return replacementService.replace(parsed, origin, sourceUrl, retrievedAt, checksum);
    }

    private static SearchHit rank(ProductEntity product, String query, List<String> terms) {
        String name = labels(product.getName());
        String brand = labels(product.getBrand());
        String category1 = labels(product.getCategory1());
        String category2 = labels(product.getCategory2());
        String category3 = labels(product.getCategory3());
        String code = normalize(product.getCode());
        List<String> retailers = product.getListings().stream()
                .map(RetailerListingEntity::getRetailerCode).map(CatalogService::normalize).toList();

        int score = 0;
        int nameTokenCount = 0;
        boolean hasDirectMatch = false;
        for (String term : terms) {
            int best = 0;
            if (contains(name, term)) {
                best = 60;
                nameTokenCount++;
            }
            if (contains(brand, term)) best = Math.max(best, 50);
            if (contains(category3, term)) best = Math.max(best, 40);
            if (contains(category2, term)) best = Math.max(best, 30);
            if (code.contains(term)) best = Math.max(best, 20);
            if (retailers.stream().anyMatch(retailer -> retailer.contains(term))) best = Math.max(best, 15);
            boolean broadMatch = contains(category1, term);
            if (broadMatch) best = Math.max(best, 10);
            if (best == 0) {
                return null;
            }
            if (best > 10) {
                hasDirectMatch = true;
            }
            score += best;
        }

        boolean exactCode = !query.isEmpty() && code.equals(query);
        boolean exactName = !query.isEmpty() && containsExactLabel(product.getName(), query);
        boolean phraseName = !query.isEmpty() && contains(name, query);
        return new SearchHit(product, !terms.isEmpty() && !hasDirectMatch, exactCode, exactName, phraseName,
                nameTokenCount, score, product.getCode());
    }

    private static boolean contains(String localizedValues, String query) {
        return normalize(localizedValues).contains(query);
    }

    private static boolean containsExactLabel(LocalizedLabel label, String query) {
        if (label == null) return false;
        return Stream.of(label.getEn(), label.getZhHant(), label.getZhHans())
                .filter(Objects::nonNull)
                .map(CatalogService::normalize)
                .anyMatch(query::equals);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
    }

    private static String labels(LocalizedLabel label) {
        if (label == null) {
            return "";
        }
        return normalize(String.join(" ", label.getEn(), label.getZhHant(), label.getZhHans()));
    }

    private record SearchHit(ProductEntity product, boolean related, boolean exactCode, boolean exactName,
                            boolean phraseName, int nameTokenCount, int score, String code) {
    }

    private static ImportStatus toStatus(AcceptedImportEntity status) {
        return new ImportStatus(status.getOrigin(), status.getSourceUrl(), status.getRetrievedAt(),
                status.getSourceDate(), status.getChecksum(), status.getProductCount(),
                status.getListingCount(), status.getOfferCount());
    }

    private static byte[] readLimited(InputStream input, int maximum) throws IOException {
        byte[] bytes = input.readNBytes(maximum + 1);
        if (bytes.length > maximum) {
            throw new CatalogImportException("The sample file exceeds the 10 MB limit.");
        }
        return bytes;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available.", impossible);
        }
    }
}
