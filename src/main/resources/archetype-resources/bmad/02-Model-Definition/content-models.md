# AEM Forms - Data Models and Templates

This document defines the data models, templates, and themes for the AEM Forms implementation. The focus is on creating reusable and structured assets for efficient form development and data capture.

## Form Data Models (FDM)

FDMs are the backbone of our data strategy, defining the entities, attributes, and services used to pre-fill forms and process submissions.

### User Profile FDM

- **Purpose**: Pre-fill common user information in authenticated forms and capture updates.
- **JCR Path**: `/conf/aem-forms-bmad-showcase/settings/fdm/user-profile-fdm`
- **JSON Schema Path**: `/conf/aem-forms-bmad-showcase/settings/fdm/user-profile-fdm/schemas/user-profile.schema.json`
- **Schema Fields**:
  - `firstName` (String)
  - `lastName` (String)
  - `email` (String, format: email)
  - `userId` (String)
  - `address`:
    - `street` (String)
    - `city` (String)
    - `state` (String)
    - `postalCode` (String)
    - `country` (String)
- **Services (Placeholder)**:
  - `getProfile(userId)`: Retrieves user data from an external system.
  - `updateProfile(userId, profileData)`: Updates user data in an external system.

### Loan Application FDM

- **Purpose**: Manage data for the entire loan application lifecycle.
- **Schema Fields**: (as previously defined)
- **Services (Placeholder)**: (as previously defined)

---

## Adaptive Form Templates

Templates provide a consistent structure and starting point for all forms.

### Standard Application Template

- **Template Path**: `/conf/aem-forms-bmad-showcase/settings/wcm/templates/standard-application`
- **Sample Form Using this Template**: `/content/forms/af/aem-forms-bmad-showcase/user-profile-form`
- **Structure**:
  - **Header**: Contains the form title and logo. Pre-configured.
  - **Form Panels (Wizard Layout)**: (as previously defined)
  - **Footer**: Contains links to privacy policy and terms of use.
- **Allowed Components**: All standard form components plus custom components like "Address Lookup".

---

## Adaptive Form Themes

Themes control the look and feel (styling) of the forms, ensuring brand consistency.

### Global Brand Theme

- **Clientlib Path**: `/etc/clientlibs/aem-forms-bmad-showcase/themes/global-brand-theme`
- **Styling**:
  - **Colors**: Primary Blue (`#005A9E`), Accent Orange (`#FF7A00`).
  - **Fonts**: `Roboto` for body text, `Montserrat` for headings.
  - **Field Style**: Underlined text fields with floating labels.
  - **Button Style**: Solid background for primary buttons, outlined for secondary.
  - **Wizard Navigation**: Progress bar at the top of the form.

---

## Custom Adaptive Form Components

These are custom React components developed to provide functionality beyond the out-of-the-box components. They are developed in the `ui.frontend.react.forms.af` module.

### Address Lookup Component

- **Resource Type**: `aem-forms-bmad-showcase/components/adaptiveForm/address-lookup`
- **Purpose**: Provides an autocomplete field for addresses, powered by a third-party API (e.g., Google Places). Reduces data entry errors.
- **Fields (Authoring Dialog)**:
  - `label` (Text): The field label.
  - `description` (Text): Help text for the user.
  - `apiKey` (Text): The API key for the address service (stored in OSGi config).
  - `dataBinding` (Path): The FDM path to bind the address data to (e.g., `/applicantInfo/address`).
- **Functionality**:
  - As the user types, the component calls the address API.
  - User selects an address from the suggestions.
  - The `street`, `city`, `state`, and `postalCode` fields in the FDM are automatically populated.

### E-Sign Component

- **Resource Type**: `aem-forms-bmad-showcase/components/adaptiveForm/e-sign`
- **Purpose**: Captures a user's signature using a signature pad or by typing their name.
- **Fields (Authoring Dialog)**:
  - `label` (Text): The field label.
  - `agreementText` (Rich Text): The legal text the user is agreeing to.
  - `captureMethod` (Select: "Draw", "Type", "Both"): The allowed signature methods.
- **Functionality**:
  - Renders a canvas for drawing or a text input for typing a signature.
  - On submission, the captured signature is saved as an image and attached to the form data.
