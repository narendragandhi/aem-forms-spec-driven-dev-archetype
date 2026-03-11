# Interactive Communications Guide

This guide covers how to use AEM Forms Interactive Communications (IC) with this archetype.

## Overview

Interactive Communications enable you to create personalized, multi-channel customer documents such as:
- Account statements
- Policy documents
- Welcome kits
- Correspondence letters
- Bills and invoices

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Interactive Communication                 │
├─────────────────────┬───────────────────────────────────────┤
│    Print Channel    │           Web Channel                 │
│    (PDF Output)     │      (Responsive HTML)                │
├─────────────────────┴───────────────────────────────────────┤
│                    Document Fragments                        │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌──────────────────┐  │
│  │ Header  │ │ Footer  │ │ T&Cs    │ │ Customer Details │  │
│  └─────────┘ └─────────┘ └─────────┘ └──────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                    Form Data Model (FDM)                     │
│  ┌─────────────────┐    ┌─────────────────────────────┐    │
│  │  REST API       │    │  Database (JDBC)            │    │
│  │  Customer Data  │    │  Transaction History        │    │
│  └─────────────────┘    └─────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

## Included Assets

### Document Fragments
Located in `/content/dam/formsanddocuments/fragments/${appName}/`:

| Fragment | Description |
|----------|-------------|
| `header` | Branded document header with logo |
| `footer` | Contact info and legal disclaimers |
| `terms-and-conditions` | Standard T&Cs text |
| `customer-details` | Customer name/address block |

### Sample Interactive Communications
Located in `/content/dam/formsanddocuments/ic/${appName}/`:

| IC | Description |
|----|-------------|
| `account-statement` | Monthly financial statement with transactions |
| `welcome-kit` | New customer onboarding document |

### Form Data Models
Located in `/content/dam/formsanddocuments-fdm/${appName}/`:

| FDM | Type | Description |
|-----|------|-------------|
| `rest-customer-api` | REST | Customer and transaction data via REST API |

## Creating a New Interactive Communication

### 1. Define the Data Model

Create or configure a Form Data Model that connects to your data source:

```json
// Example REST API response structure
{
  "customer": {
    "customerId": "CUST-001",
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "accountBalance": 5420.50
  },
  "transactions": [
    {
      "transactionId": "TXN-001",
      "date": "2024-01-15",
      "description": "Direct Deposit",
      "amount": 3500.00,
      "type": "credit"
    }
  ]
}
```

### 2. Create Document Fragments

Fragments are reusable content blocks. Create them in AEM Forms UI:

1. Navigate to **Forms > Forms & Documents**
2. Create folder under `fragments/${appName}/`
3. Create **Document Fragment**
4. Bind data elements to FDM entities

### 3. Design the Print Channel

The print channel generates PDF output:

1. Create **IC Template** > Print Channel
2. Add layout with **Target Areas**
3. Drag fragments into target areas
4. Configure data bindings
5. Set PDF options (watermarks, fonts, etc.)

### 4. Design the Web Channel

The web channel generates responsive HTML:

1. Edit IC > Web Channel
2. Enable **Auto-sync from Print** (optional)
3. Add responsive components
4. Configure interactive elements (charts, tables)
5. Preview across devices

## Integration Patterns

### With Adaptive Forms

Trigger IC generation after form submission:

```java
@Component(service = WorkflowProcess.class)
public class GenerateStatementProcess implements WorkflowProcess {

    @Reference
    private InteractiveCommunicationService icService;

    @Override
    public void execute(WorkItem workItem, WorkflowSession session, MetaDataMap args) {
        String customerId = (String) workItem.getWorkflowData().getPayload();

        // Generate IC with customer data
        ICOptions options = new ICOptions();
        options.setChannel(Channel.PRINT);
        options.setFlattenPdf(true);

        Document statement = icService.generate(
            "/content/dam/formsanddocuments/ic/${appName}/account-statement",
            customerId,
            options
        );

        // Send via email or store
    }
}
```

### With AEM Workflows

Use the **Generate Interactive Communication** workflow step:

1. Create workflow model
2. Add **IC Generation** step
3. Configure:
   - IC path
   - Data source
   - Output channel
   - Delivery options

### Batch Generation

For bulk document generation:

```java
@Component(service = Runnable.class, property = {
    "scheduler.expression=0 0 2 * * ?",  // Daily at 2 AM
    "scheduler.concurrent=false"
})
public class MonthlyStatementBatch implements Runnable {

    @Reference
    private BatchService batchService;

    @Override
    public void run() {
        BatchConfig config = new BatchConfig();
        config.setIcPath("/content/dam/formsanddocuments/ic/${appName}/account-statement");
        config.setDataSource("/content/dam/formsanddocuments-fdm/${appName}/rest-customer-api");
        config.setOutputFolder("/content/dam/statements/" + LocalDate.now());

        batchService.generateBatch(config);
    }
}
```

## Output Service Configuration

The Output Service is pre-configured in `ui.config`:

```json
{
  "pdf.cacheEnabled": true,
  "pdf.cacheLocation": "/tmp/aemfd/output-cache",
  "pdf.maxCacheEntries": 1000,
  "pdf.fontDirectories": ["/apps/${appName}/fonts"]
}
```

### Custom Fonts

Add custom fonts to `/apps/${appName}/fonts/`:

1. Upload `.ttf` or `.otf` files
2. Restart AEM or clear font cache
3. Reference in IC templates

## Delivery Options

### Email Delivery

```java
EmailOptions email = new EmailOptions();
email.setTo("customer@example.com");
email.setSubject("Your Monthly Statement");
email.setAttachmentName("statement.pdf");

icService.generateAndSend(icPath, customerId, email);
```

### Store in DAM

```java
StorageOptions storage = new StorageOptions();
storage.setPath("/content/dam/customer-documents/" + customerId);
storage.setFilename("statement-" + LocalDate.now() + ".pdf");

icService.generateAndStore(icPath, customerId, storage);
```

### Download via Servlet

```java
@Component(service = Servlet.class, property = {
    "sling.servlet.paths=/services/ic/download"
})
public class ICDownloadServlet extends SlingAllMethodsServlet {

    @Reference
    private InteractiveCommunicationService icService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        String customerId = request.getParameter("customerId");

        Document pdf = icService.generate(
            "/content/dam/formsanddocuments/ic/${appName}/account-statement",
            customerId,
            new ICOptions().setChannel(Channel.PRINT)
        );

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=statement.pdf");
        pdf.copyToStream(response.getOutputStream());
    }
}
```

## Best Practices

1. **Use fragments** - Reuse content across multiple ICs
2. **Design mobile-first** - Web channel should be responsive
3. **Cache data** - Pre-fetch FDM data for batch processing
4. **Monitor performance** - Use Output Service caching
5. **Version templates** - Keep template history in DAM
6. **Test both channels** - Ensure print/web parity

## Troubleshooting

| Issue | Solution |
|-------|----------|
| PDF fonts missing | Add fonts to `/apps/${appName}/fonts/` |
| FDM connection fails | Check Cloud Services configuration |
| Web channel not rendering | Verify responsive grid settings |
| Batch job timeout | Increase scheduler timeout, reduce batch size |

## Related Documentation

- [Adobe IC Documentation](https://experienceleague.adobe.com/docs/experience-manager-65/forms/interactive-communications/introduction-interactive-communication.html)
- [Form Data Model Guide](./external-services-integration.md)
- [Output Service API](https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/aemfd/output/api/package-summary.html)
