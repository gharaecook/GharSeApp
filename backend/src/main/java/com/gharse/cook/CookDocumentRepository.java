package com.gharse.cook;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CookDocumentRepository extends JpaRepository<CookDocument, UUID> {

    List<CookDocument> findByCookIdOrderByCreatedAtDesc(UUID cookId);

    boolean existsByCookIdAndDocumentType(UUID cookId, DocumentType type);

    List<CookDocument> findByCookIdInAndDocumentType(Collection<UUID> cookIds, DocumentType type);
}
