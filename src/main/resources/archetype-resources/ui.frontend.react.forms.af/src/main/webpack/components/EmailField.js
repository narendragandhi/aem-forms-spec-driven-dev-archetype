import React from 'react';
import { Field } from '@aem-forms/af-react-components';

const EmailField = ({ label, description, name, value, id, visible, enabled }) => {
  if (!visible) return null;
  return (
    <div className="email-field">
      <label htmlFor={id}>{label}</label>
      {description && <p>{description}</p>}
      <input type="email" id={id} name={name} value={value || ''} disabled={!enabled} placeholder="Enter your email" />
    </div>
  );
};

export default function (props) {
  return <Field {...props} render={EmailField} />;
}
