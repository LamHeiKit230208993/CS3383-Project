package com.pricewatch.store;

import com.pricewatch.domain.ShoppingList;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShoppingListStore {

    private final ShoppingListRepository repository;

    public ShoppingListStore(ShoppingListRepository repository) {
        this.repository = repository;
    }

    public ShoppingList load() {
        return repository.findById(ShoppingList.SINGLETON_ID)
                .orElseGet(() -> repository.save(new ShoppingList(ShoppingList.DEFAULT_NAME, List.of())));
    }

    public ShoppingList save(ShoppingList list) {
        return repository.save(list);
    }
}
