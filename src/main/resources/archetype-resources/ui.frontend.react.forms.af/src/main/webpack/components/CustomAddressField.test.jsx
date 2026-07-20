import React from 'react';
import { vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';

// Mock the AEM Forms wrapper: its CJS build require()s the ESM-only
// @adobe/json-formula, which node cannot load in the vitest environment.
// The unit under test is the render component, not the framework wrapper.
vi.mock('@aemforms/af-react-components', () => ({
  Field: (props) => {
    const Render = props.render;
    return <Render {...props} />;
  },
}));

import CustomAddressField from './CustomAddressField';

describe('CustomAddressField', () => {
  const defaultProps = {
    label: 'Mailing Address',
    description: 'Enter your full street address',
    name: 'address',
    id: 'address-123',
    visible: true,
    enabled: true,
    value: ''
  };

  test('renders correctly with label and placeholder', () => {
    render(<CustomAddressField {...defaultProps} />);
    
    expect(screen.getByText('Mailing Address')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Start typing your address...')).toBeInTheDocument();
  });

  test('updates input value on change', () => {
    render(<CustomAddressField {...defaultProps} />);
    const input = screen.getByPlaceholderText('Start typing your address...');
    
    fireEvent.change(input, { target: { value: '123 Main St' } });
    expect(input.value).toBe('123 Main St');
  });

  test('is disabled when enabled prop is false', () => {
    render(<CustomAddressField {...defaultProps} enabled={false} />);
    const input = screen.getByPlaceholderText('Start typing your address...');
    
    expect(input).toBeDisabled();
  });
});
