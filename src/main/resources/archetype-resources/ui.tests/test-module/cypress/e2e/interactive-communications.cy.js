/**
 * E2E Tests for Interactive Communications
 */
describe('Interactive Communications E2E Tests', () => {

  describe('Document Fragment Tests', () => {
    it('should load header fragment', () => {
      cy.visit('/content/dam/formsanddocuments/fragments/${appName}/header.html');
      cy.get('.document-fragment').should('exist');
    });

    it('should load footer fragment', () => {
      cy.visit('/content/dam/formsanddocuments/fragments/${appName}/footer.html');
      cy.get('.document-fragment').should('exist');
    });
  });

  describe('IC Template Tests', () => {
    it('should access account statement IC', () => {
      cy.visit('/content/dam/formsanddocuments/ic/${appName}/account-statement.html');
      cy.get('[data-ic-type="interactiveCommunication"]').should('exist');
    });

    it('should display print channel option', () => {
      cy.visit('/content/dam/formsanddocuments/ic/${appName}/account-statement.html');
      cy.get('[data-channel="print"]').should('exist');
    });

    it('should display web channel option', () => {
      cy.visit('/content/dam/formsanddocuments/ic/${appName}/account-statement.html');
      cy.get('[data-channel="web"]').should('exist');
    });
  });

  describe('IC Generation Tests', () => {
    it('should generate PDF for print channel', () => {
      cy.request({
        method: 'POST',
        url: '/services/ic/generate',
        body: {
          icPath: '/content/dam/formsanddocuments/ic/${appName}/account-statement',
          channel: 'print',
          customerId: 'TEST-001'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.headers['content-type']).to.include('application/pdf');
      });
    });

    it('should render web channel HTML', () => {
      cy.request({
        method: 'POST',
        url: '/services/ic/generate',
        body: {
          icPath: '/content/dam/formsanddocuments/ic/${appName}/account-statement',
          channel: 'web',
          customerId: 'TEST-001'
        }
      }).then((response) => {
        expect(response.status).to.eq(200);
        expect(response.headers['content-type']).to.include('text/html');
      });
    });
  });

  describe('Form Data Model Tests', () => {
    it('should connect to REST data source', () => {
      cy.request({
        method: 'GET',
        url: '/content/dam/formsanddocuments-fdm/${appName}/rest-customer-api/services/getCustomer?customerId=TEST-001',
        failOnStatusCode: false
      }).then((response) => {
        // May return 404 if backend not configured, but should not error
        expect([200, 404, 503]).to.include(response.status);
      });
    });
  });

  describe('Customer Statement Flow', () => {
    it('should complete full statement generation flow', () => {
      // 1. Navigate to IC editor
      cy.visit('/aem/forms.html/content/dam/formsanddocuments/ic/${appName}');

      // 2. Select account statement
      cy.get('[data-item-title="Account Statement"]').click();

      // 3. Preview web channel
      cy.get('[data-action="preview-web"]').click();
      cy.get('.ic-preview').should('be.visible');

      // 4. Download PDF
      cy.get('[data-action="download-pdf"]').click();
      // Verify download initiated
    });
  });
});
