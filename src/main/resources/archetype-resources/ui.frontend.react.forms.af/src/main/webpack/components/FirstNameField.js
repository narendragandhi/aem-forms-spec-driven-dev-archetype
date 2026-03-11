import React from 'react';
import { Field } from '@aem-forms/af-react-components';

/**
 * A concrete implementation for the 'firstName' field from the sample spec.
 * This component demonstrates a direct mapping from a spec property to a
 * working React component.
 */
const FirstNameField = ({
  label,
  description,
  name,
  value,
  id,
  visible,
  enabled,
}) => {
  if (!visible) {
    return null;
  }

  return (
    <div className="firstname-field">
      <label htmlFor={id} className="firstname-label">
        {label}
      </label>
      {description && (
        <p className="firstname-description">{description}</p>
      )}
      <input
        id={id}
        name={name}
        type="text"
        value={value || ''}
        disabled={!enabled}
        placeholder="Enter your first name"
        className="firstname-input"
      />
    </div>
  );
};

export default function (props) {
  // The 'Field' component handles the integration with the AEM Forms framework.
  return <Field {...props} render={FirstNameField} />;
}
