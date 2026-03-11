# UI.frontend Module

This module contains the frontend source code for the AEM Forms BMAD project. It is a React-based Single Page Application (SPA) that is built and embedded into the `ui.apps` module.

## Main Dependencies

*   **React:** The core library for building the user interface.
*   **AEM SPA Editor:** Libraries for enabling the AEM SPA Editor experience.
*   **AEM Core Components (React):** A set of reusable UI components for AEM.
*   **React Router:** For handling routing within the SPA.

## Building

This module is built as part of the main project build. The `frontend-maven-plugin` is used to execute the `npm run build` command, which compiles the frontend assets and generates a client library.

To build the frontend individually, you can run the following command from the root of this module:

```bash
npm install
npm run build
```

## Available Scripts

*   `npm start`: Starts the development server.
*   `npm run build`: Builds the application for production.
*   `npm test`: Runs the test suite.
*   `npm run eject`: Ejects from the Create React App configuration.
*   `npm run sync`: Syncs the compiled assets with the `ui.apps` module.
