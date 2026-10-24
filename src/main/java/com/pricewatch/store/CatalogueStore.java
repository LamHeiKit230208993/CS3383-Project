package com.pricewatch.store;

import com.pricewatch.domain.Catalogue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CatalogueStore {

    private final CatalogueRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    public CatalogueStore(CatalogueRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Catalogue get() {
        return repository.findFirstWithProducts().orElse(null);
    }

    public Optional<Catalogue> find() {
        return repository.findFirstWithProducts();
    }

    /**
     * J02: accept a fully validated catalogue in one step. The old row and its products are
     * deleted, then the new catalogue is written. On any failure the transaction rolls back and
     * the previous catalogue stays in place.
     */
    @Transactional
    public Catalogue replace(Catalogue incoming) {
        for (Catalogue existing : repository.findAll()) {
            repository.delete(existing);
        }
        repository.flush();
        entityManager.clear();
        Catalogue saved = repository.save(incoming);
        entityManager.flush();
        return saved;
    }
}
