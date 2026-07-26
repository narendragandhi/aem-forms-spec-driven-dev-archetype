# Omnichannel Sign & DoR Architecture

> **Status**: this sequence diagram is broadly accurate for the Sign+DoR
> flow (`SignToDoRProcess`, `HeadlessSubmitServlet`) — both are real,
> live-verified code, not mocked. The one label to correct: "Adobe Sign
> (Mock)" below is stale — `AdobeSignOrchestratorImpl` makes real Adobe
> Sign REST API calls now, though it hasn't been proven against a real
> Adobe Sign account yet. See `README.md#adobe-sign-integration`.

```mermaid
sequenceDiagram
    participant U as User (React App)
    participant BFF as HeadlessFormService (BFF)
    participant AEM as AEM Core (Forms/Workflow)
    participant SIG as Adobe Sign (Mock)
    participant DOR as DoR Service

    U->>BFF: Request Form + Prefill
    BFF-->>U: Form Model + Data
    U->>BFF: POST Submission
    BFF->>AEM: Start SignWorkflow
    AEM-->>U: workflowId
    loop Status Polling
        U->>BFF: Get Status
        BFF->>AEM: Check Workflow
        AEM-->>U: {signingStatus, dorStatus}
    end
```
