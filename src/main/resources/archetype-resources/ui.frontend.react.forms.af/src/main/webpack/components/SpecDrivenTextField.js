import React from 'react';
import { Field } from '@aem-forms/af-react-components';

/**
 * A generic, "spec-aware" text field component.
 * This component is designed to be driven by a JSON schema specification.
 * An AI agent could use this as a template to generate new components
 * or use it directly by passing different parts of a spec to it.
 */
const SpecDrivenTextField = ({
  label,
  description,
  name,
  value,
  id,
  visible,
  enabled,
  // These would be passed in from the JSON schema
  schemaProps = {}
}) => {
  if (!visible) {
    return null;
  }

  return (
    <div className="spec-driven-field">
      <label htmlFor={id} className="spec-driven-label">
        {label || schemaProps.title}
      </label>
      { (description || schemaProps.description) && (
        <p className="spec-driven-description">{description || schemaProps.description}</p>
      )}
      <input
        id={id}
        name={name}
        type={schemaProps.format || 'text'}
        value={value || ''}
        disabled={!enabled}
        placeholder={schemaProps.placeholder || ''}
        required={schemaProps.required || false}
        className="spec-driven-input"
      />
    </div>
  );
};

export default function (props) {
  // The 'Field' component from @aem-forms/af-react-components handles the
  // integration with the AEM Forms framework and data model.
  return <Field {...props} render={SpecDrivenTextField} />;
}
