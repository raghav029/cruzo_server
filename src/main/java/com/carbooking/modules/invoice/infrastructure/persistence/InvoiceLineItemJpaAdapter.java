package com.carbooking.modules.invoice.infrastructure.persistence;

import com.carbooking.entity.Invoice;
import com.carbooking.entity.InvoiceLineItem;
import com.carbooking.modules.invoice.domain.port.InvoiceLineItemPort;
import com.carbooking.repository.InvoiceLineItemRepository;
import com.carbooking.modules.invoice.domain.port.InvoiceLineItemPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvoiceLineItemJpaAdapter implements InvoiceLineItemPort {

    private final InvoiceLineItemRepository repo;

    @Override public List<InvoiceLineItem> findByInvoice(Invoice invoice) { return repo.findByInvoice(invoice); }
    @Override public InvoiceLineItem save(InvoiceLineItem item) { return repo.save(item); }
}
