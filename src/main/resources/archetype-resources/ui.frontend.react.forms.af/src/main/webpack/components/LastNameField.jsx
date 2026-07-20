import React from 'react';
import { Field } from '@aem-forms/af-react-components';

const LastNameField = ({ label, description, name, value, id, visible, enabled }) => {
  if (!visible) return null;
  return (
    <div className="lastname-field">
      <label htmlFor={id}>{label}</label>
      {description && <p>{description}</p>}
      <input type="text" id={id} name={name} value={value || ''} disabled={!enabled} placeholder="Enter your last name" />
    </div>
  );
};

export default function (props) {
  return <Field {...props} render={LastNameField} />;
}
