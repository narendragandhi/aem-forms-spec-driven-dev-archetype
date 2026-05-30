package ${package}.services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdobeSignOrchestratorImplTest {

    private AdobeSignOrchestratorImpl orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AdobeSignOrchestratorImpl();
    }

    @Test
    void testCreateAgreementReturnsNonNullId() {
        assertNotNull(orchestrator.createAgreement("test data"));
    }

    @Test
    void testCreateAgreementIdStartsWithSign() {
        String id = orchestrator.createAgreement("form data");
        assertTrue(id.startsWith("SIGN-"), "Agreement ID should start with SIGN-");
    }

    @Test
    void testCreateAgreementReturnsUniqueIds() {
        String id1 = orchestrator.createAgreement("data1");
        String id2 = orchestrator.createAgreement("data2");
        assertNotEquals(id1, id2);
    }

    @Test
    void testCreateAgreementWithEmptyData() {
        String id = orchestrator.createAgreement("");
        assertNotNull(id);
        assertTrue(id.startsWith("SIGN-"));
    }

    @Test
    void testCreateAgreementWithNullData() {
        assertDoesNotThrow(() -> orchestrator.createAgreement(null));
    }

    @Test
    void testGetStatusForNewAgreementIsValidState() {
        String id = orchestrator.createAgreement("data");
        String status = orchestrator.getStatus(id);
        assertTrue("OUT_FOR_SIGNATURE".equals(status) || "SIGNED".equals(status),
            "Status should be OUT_FOR_SIGNATURE or SIGNED, was: " + status);
    }

    @Test
    void testGetStatusForUnknownAgreementReturnsNotFound() {
        assertEquals("NOT_FOUND", orchestrator.getStatus("SIGN-does-not-exist"));
    }

    @Test
    void testGetStatusEventuallyTransitionsToSigned() {
        String id = orchestrator.createAgreement("data");
        boolean seenSigned = false;
        for (int i = 0; i < 100; i++) {
            if ("SIGNED".equals(orchestrator.getStatus(id))) {
                seenSigned = true;
                break;
            }
        }
        assertTrue(seenSigned, "Status should eventually transition to SIGNED within 100 polls");
    }

    @Test
    void testGetStatusRemainsSignedOnceTransitioned() {
        String id = orchestrator.createAgreement("data");
        for (int i = 0; i < 100; i++) {
            if ("SIGNED".equals(orchestrator.getStatus(id))) break;
        }
        assertEquals("SIGNED", orchestrator.getStatus(id), "Status should stay SIGNED once transitioned");
    }

    @Test
    void testMultipleAgreementsTrackedIndependently() {
        String id1 = orchestrator.createAgreement("data1");
        String id2 = orchestrator.createAgreement("data2");
        assertNotEquals(id1, id2);
        // Both should return valid statuses (not NOT_FOUND)
        assertNotEquals("NOT_FOUND", orchestrator.getStatus(id1));
        assertNotEquals("NOT_FOUND", orchestrator.getStatus(id2));
    }

    @Test
    void testUnknownIdDoesNotAffectKnownAgreement() {
        String id = orchestrator.createAgreement("data");
        orchestrator.getStatus("SIGN-unknown");
        assertNotEquals("NOT_FOUND", orchestrator.getStatus(id));
    }
}
