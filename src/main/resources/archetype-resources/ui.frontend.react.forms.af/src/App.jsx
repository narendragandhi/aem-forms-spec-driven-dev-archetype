import React, { useEffect, useState } from 'react';
import { AdaptiveForm } from '@aemforms/af-react-renderer';
import { mappings } from '@aemforms/af-react-components';
import CustomAddressField from './main/webpack/components/CustomAddressField';
import './App.css';

const customMappings = {
  ...mappings,
  'custom-address-field': CustomAddressField
};

const App = () => {
  const [formJson, setFormJson] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [workflowId, setWorkflowId] = useState(null);
  const [status, setStatus] = useState(null);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const formPath = params.get('formPath');
    if (!formPath) {
      setError('Missing required "formPath" query parameter (e.g. ?formPath=/content/forms/af/<appName>/<slug>)');
      setLoading(false);
      return;
    }

    fetch(`/bin/bmad/headless-form-service?formPath=${formPath}`)
      .then(response => {
        if (!response.ok) throw new Error('Network response was not ok');
        return response.json();
      })
      .then(data => fetch(data.endpoint))
      .then(response => response.json())
      .then(json => {
        setFormJson(json);
        setLoading(false);
      })
      .catch(err => {
        console.error('Error fetching form:', err);
        setError(err.message);
        setLoading(false);
      });
  }, []);

  useEffect(() => {
    let interval;
    if (workflowId) {
      interval = setInterval(() => {
        fetch(`/bin/bmad/headless-status?workflowId=${workflowId}`)
          .then(res => res.json())
          .then(data => {
            setStatus(data);
            if (data.dorStatus === 'GENERATED') clearInterval(interval);
          })
          .catch(err => console.error('Status check error:', err));
      }, 3000);
    }
    return () => clearInterval(interval);
  }, [workflowId]);

  const onFormSubmit = (event) => {
    // event is the real @aemforms/af-core 'submitSuccess' action - its
    // payload is the raw response from the framework's own native submit
    // action (e.g. {redirectUrl: ...}), not custom data, and it has no
    // .body property. The submitted form data itself comes from
    // event.target.getState().data (a real getter backed by
    // FormModel.exportData(), confirmed against the published
    // @aemforms/af-core package source). Forward that to this
    // archetype's own HeadlessSubmitServlet (backed by the real,
    // live-verified FormSubmissionService) to kick off Sign/DoR
    // post-processing - the framework's native submission already
    // completed by the time this fires; this is a second, additive step.
    const submittedData = event?.target?.getState ? event.target.getState().data : undefined;

    fetch('/bin/bmad/headless-submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(submittedData || {})
    })
      .then(response => response.json())
      .then(data => {
        if (data && data.workflowId) {
          setWorkflowId(data.workflowId);
        }
      })
      .catch(err => console.error('Failed to dispatch headless submission for post-processing:', err));
  };

  if (loading) return <div className="headless-form-container">Loading form...</div>;
  if (error) return <div className="headless-form-container">Error: {error}</div>;

  return (
    <div className="headless-form-container">
      <h1>Headless AEM Form</h1>
      
      {workflowId ? (
        <div className="status-panel">
          <h3>Submission Successful!</h3>
          <p>Workflow ID: <code>{workflowId}</code></p>
          {status && (
            <div className="status-details">
              <div className={`status-badge ${status.state}`}>System: {status.state}</div>
              <div className={`status-badge ${status.signingStatus}`}>Signing: {status.signingStatus}</div>
              <div className={`status-badge ${status.dorStatus}`}>Document: {status.dorStatus}</div>
            </div>
          )}
          {status?.dorStatus === 'GENERATED' && (
            <div className="success-msg">Your Document of Record is ready for download.</div>
          )}
        </div>
      ) : (
        <AdaptiveForm 
          formJson={formJson} 
          mappings={customMappings} 
          onSubmitSuccess={onFormSubmit}
        />
      )}
    </div>
  );
};

export default App;
