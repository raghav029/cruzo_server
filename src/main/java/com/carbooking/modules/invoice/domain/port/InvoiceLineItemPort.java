package com.carbooking.modules.invoice.domain.port;

import com.carbooking.entity.Invoice;
import com.carbooking.entity.InvoiceLineItem;

import java.util.List;

public interface InvoiceLineItemPort {
    List<InvoiceLineItem> findByInvoice(Invoice invoice);
    InvoiceLineItem save(InvoiceLineItem item);
}
