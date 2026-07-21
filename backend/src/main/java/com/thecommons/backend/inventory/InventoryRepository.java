package com.thecommons.backend.inventory;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByQrCode(String qrCode);

    boolean existsByQrCode(String qrCode);
}


