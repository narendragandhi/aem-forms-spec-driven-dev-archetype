import React from 'react';
import { vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';

// AdaptiveForm does real form-model creation internally (see
// @aemforms/af-core) - not the unit under test here. Capturing
// onSubmitSuccess lets the test drive it directly with a real event
// shape instead of exercising the whole framework.
let capturedOnSubmitSuccess;

vi.mock('@aemforms/af-react-renderer', () => ({
  AdaptiveForm: (props) => {
    capturedOnSubmitSuccess = props.onSubmitSuccess;
    return <div data-testid="adaptive-form-stub" />;
  },
}));

vi.mock('@aemforms/af-react-components', () => ({
  mappings: {},
}));

vi.mock('./main/webpack/components/CustomAddressField', () => ({
  default: () => <div />,
}));

import App from './App';

describe('App - headless submit forwarding', () => {
  beforeEach(() => {
    capturedOnSubmitSuccess = undefined;
    global.fetch = vi.fn();
    window.history.pushState({}, '', '/?formPath=/content/forms/af/AcmeApp/test-form');
  });

  test('forwards the real submitted form data to /bin/bmad/headless-submit and reads workflowId from its response', async () => {
    global.fetch
      // 1: /bin/bmad/headless-form-service metadata
      .mockResolvedValueOnce({ ok: true, json: async () => ({ endpoint: '/content/forms/af/AcmeApp/test-form.model.json' }) })
      // 2: the .model.json that endpoint points at
      .mockResolvedValueOnce({ json: async () => ({ id: 'test-form' }) })
      // 3: the headless-submit forward this test triggers
      .mockResolvedValueOnce({ json: async () => ({ status: 'success', workflowId: 'WF-12345' }) });

    render(<App />);

    await waitFor(() => expect(capturedOnSubmitSuccess).toBeInstanceOf(Function));

    // Real @aemforms/af-core 'submitSuccess' action shape: payload is the
    // framework's own native-submit response, submitted data comes from
    // target.getState().data (confirmed against the published af-core
    // source, not assumed) - not event.body, which doesn't exist.
    const realSubmitSuccessEvent = {
      type: 'submitSuccess',
      payload: { redirectUrl: '/content/forms/af/AcmeApp/thank-you' },
      target: { getState: () => ({ data: { fullName: 'Jane Doe' } }) },
    };
    capturedOnSubmitSuccess(realSubmitSuccessEvent);

    expect(await screen.findByText('WF-12345')).toBeInTheDocument();

    expect(global.fetch).toHaveBeenNthCalledWith(3, '/bin/bmad/headless-submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ fullName: 'Jane Doe' }),
    });
  });

  test('does not crash when the submitSuccess event has no usable target data', async () => {
    global.fetch
      .mockResolvedValueOnce({ ok: true, json: async () => ({ endpoint: '/content/forms/af/AcmeApp/test-form.model.json' }) })
      .mockResolvedValueOnce({ json: async () => ({ id: 'test-form' }) })
      .mockResolvedValueOnce({ json: async () => ({ status: 'success', workflowId: 'WF-99' }) });

    render(<App />);
    await waitFor(() => expect(capturedOnSubmitSuccess).toBeInstanceOf(Function));

    expect(() => capturedOnSubmitSuccess({ type: 'submitSuccess', payload: {} })).not.toThrow();

    await waitFor(() =>
      expect(global.fetch).toHaveBeenNthCalledWith(3, '/bin/bmad/headless-submit', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      })
    );
  });

  test('shows a clear error instead of crashing when formPath is missing', async () => {
    window.history.pushState({}, '', '/');

    render(<App />);

    expect(await screen.findByText(/Missing required "formPath"/)).toBeInTheDocument();
    expect(global.fetch).not.toHaveBeenCalled();
  });
});
