package com.carbooking.repository;

import com.carbooking.entity.Invoice;
import com.carbooking.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {
    List<InvoiceLineItem> findByInvoice(Invoice invoice);
}
