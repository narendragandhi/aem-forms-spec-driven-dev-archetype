/**
 * E2E Tests for AEM Forms functionality
 */
describe('AEM Forms E2E Tests', () => {

  beforeEach(() => {
    // Visit the forms page
    cy.visit('/content/${appName}/us/en/forms.html');
  });

  describe('Adaptive Form Rendering', () => {
    it('should render the adaptive form container', () => {
      cy.get('[data-cmp-is="adaptiveForm"]').should('exist');
    });

    it('should display form fields', () => {
      cy.get('input[type="text"]').should('have.length.at.least', 1);
    });

    it('should show validation errors for required fields', () => {
      cy.get('form').submit();
      cy.get('.field-error, .guideFieldError').should('exist');
    });
  });

  describe('Form Field Interactions', () => {
    it('should accept text input', () => {
      cy.get('input[name="firstName"]').first()
        .type('John')
        .should('have.value', 'John');
    });

    it('should validate email format', () => {
      cy.get('input[type="email"]').first()
        .type('invalid-email')
        .blur();
      cy.get('.field-error').should('contain', 'email');
    });

    it('should accept valid email', () => {
      cy.get('input[type="email"]').first()
        .clear()
        .type('john@example.com')
        .blur();
      cy.get('.field-error').should('not.exist');
    });
  });

  describe('Form Submission', () => {
    it('should submit form with valid data', () => {
      cy.get('input[name="firstName"]').type('John');
      cy.get('input[name="lastName"]').type('Doe');
      cy.get('input[type="email"]').type('john.doe@example.com');

      cy.intercept('POST', '**/af/submit/**').as('formSubmit');
      cy.get('button[type="submit"]').click();

      cy.wait('@formSubmit').its('response.statusCode').should('eq', 200);
    });

    it('should show success message after submission', () => {
      // Fill required fields
      cy.get('input[name="firstName"]').type('Jane');
      cy.get('input[name="lastName"]').type('Smith');
      cy.get('input[type="email"]').type('jane@example.com');

      cy.get('button[type="submit"]').click();
      cy.get('.success-message, .thank-you').should('be.visible');
    });
  });
});
