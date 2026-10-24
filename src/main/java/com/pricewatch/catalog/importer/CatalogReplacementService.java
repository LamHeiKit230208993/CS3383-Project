package com.pricewatch.catalog.importer;

import com.pricewatch.catalog.ImportStatus;
import com.pricewatch.catalog.persistence.AcceptedImportEntity;
import com.pricewatch.catalog.persistence.LocalizedLabel;
import com.pricewatch.catalog.persistence.OfferEntity;
import com.pricewatch.catalog.persistence.ProductEntity;
import com.pricewatch.catalog.persistence.RetailerListingEntity;
import com.pricewatch.catalog.repository.AcceptedImportRepository;
import com.pricewatch.catalog.repository.OfferRepository;
import com.pricewatch.catalog.repository.ProductRepository;
import com.pricewatch.catalog.repository.RetailerListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CatalogReplacementService {
    private final ProductRepository productRepository;
    private final RetailerListingRepository listingRepository;
    private final OfferRepository offerRepository;
    private final AcceptedImportRepository importRepository;

    public CatalogReplacementService(ProductRepository productRepository,
                                     RetailerListingRepository listingRepository,
                                     OfferRepository offerRepository,
                                     AcceptedImportRepository importRepository) {
        this.productRepository = productRepository;
        this.listingRepository = listingRepository;
        this.offerRepository = offerRepository;
        this.importRepository = importRepository;
    }

    @Transactional
    public ImportStatus replace(ImportedCatalog catalog, String origin, String sourceUrl,
                                Instant retrievedAt, String checksum) {
        offerRepository.deleteAllInBatch();
        listingRepository.deleteAllInBatch();
        productRepository.deactivateAll();

        List<String> codes = catalog.products().stream().map(ImportedCatalog.ImportedProduct::code).toList();
        Map<String, ProductEntity> productsByCode = new HashMap<>();
        productRepository.findAllById(codes).forEach(product -> productsByCode.put(product.getCode(), product));

        List<ProductEntity> importedProducts = new ArrayList<>(catalog.products().size());
        for (ImportedCatalog.ImportedProduct source : catalog.products()) {
            ProductEntity product = productsByCode.getOrDefault(source.code(), new ProductEntity(source.code()));
            product.setBrand(toLabel(source.brand()));
            product.setName(toLabel(source.name()));
            product.setCategory1(toLabel(source.category1()));
            product.setCategory2(toLabel(source.category2()));
            product.setCategory3(toLabel(source.category3()));
            product.setActive(true);
            importedProducts.add(product);
        }
        List<ProductEntity> savedProducts = productRepository.saveAllAndFlush(importedProducts);
        Map<String, ProductEntity> savedByCode = new HashMap<>();
        savedProducts.forEach(product -> savedByCode.put(product.getCode(), product));

        List<RetailerListingEntity> listings = new ArrayList<>(catalog.listingCount());
        for (ImportedCatalog.ImportedProduct sourceProduct : catalog.products()) {
            ProductEntity product = savedByCode.get(sourceProduct.code());
            for (ImportedCatalog.ImportedListing sourceListing : sourceProduct.listings()) {
                RetailerListingEntity listing = new RetailerListingEntity(
                        product, sourceListing.retailerCode(), sourceListing.price());
                product.addListing(listing);
                sourceListing.offers().forEach(text -> listing.addOffer(new OfferEntity(toLabel(text))));
                listings.add(listing);
            }
        }
        listingRepository.saveAllAndFlush(listings);

        AcceptedImportEntity accepted = new AcceptedImportEntity(origin, sourceUrl, retrievedAt,
                null, checksum, catalog.products().size(), catalog.listingCount(), catalog.offerCount());
        importRepository.save(accepted);
        return new ImportStatus(origin, sourceUrl, retrievedAt, null, checksum,
                catalog.products().size(), catalog.listingCount(), catalog.offerCount());
    }

    private LocalizedLabel toLabel(com.pricewatch.catalog.LocalizedText text) {
        return new LocalizedLabel(text.en(), text.zhHant(), text.zhHans());
    }
}
