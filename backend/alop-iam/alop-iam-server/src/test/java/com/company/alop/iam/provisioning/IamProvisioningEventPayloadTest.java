package com.company.alop.iam.provisioning;

import static org.junit.jupiter.api.Assertions.*;

import com.company.alop.iam.api.event.IamTenantProvisioningCompleted;
import com.company.alop.iam.api.event.IamTenantProvisioningFailed;
import com.company.alop.iam.api.event.TenantProvisioningRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IamProvisioningEventPayloadTest {
    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.USE_LONG_FOR_INTS);

    @Test void completedContainsExactlyTheSchemaDataFieldsAndTypes() throws Exception {
        var payload = new IamTenantProvisioningCompleted("42", 7, 90, 80, "request-42");
        var expected = json.readTree("""
                {"tenantId":"42","adminUserId":7,"membershipId":90,"adminRoleId":80,
                 "tenantCreateRequestId":"request-42"}
                """);
        assertEquals(expected, json.valueToTree(payload));
        assertEquals(payload, json.treeToValue(expected, IamTenantProvisioningCompleted.class));
    }

    @Test void failedContainsExactlyTheSchemaDataFieldsAndTypes() throws Exception {
        var payload = new IamTenantProvisioningFailed("42", 7, "request-42", "TENANT_ADMIN_ROLE_CONFLICT", false);
        var expected = json.readTree("""
                {"tenantId":"42","adminUserId":7,"tenantCreateRequestId":"request-42",
                 "errorCode":"TENANT_ADMIN_ROLE_CONFLICT","retryable":false}
                """);
        assertEquals(expected, json.valueToTree(payload));
        assertEquals(payload, json.treeToValue(expected, IamTenantProvisioningFailed.class));
    }

    @Test void requestedContainsExactlyTheSchemaDataFieldsAndTypes() throws Exception {
        var payload = new TenantProvisioningRequested("42", "request-42", 7, 1, "Asia/Shanghai", "CNY");
        var expected = json.readTree("""
                {"tenantId":"42","tenantCreateRequestId":"request-42","adminUserId":7,
                 "tenantPackageId":1,"timezone":"Asia/Shanghai","currency":"CNY"}
                """);
        assertEquals(expected, json.valueToTree(payload));
        assertEquals(payload, json.treeToValue(expected, TenantProvisioningRequested.class));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"0", "-1", "01", "abc", " "})
    void tenantIdsMustBePositiveDecimalStrings(String tenantId) {
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningCompleted(tenantId, 7, 90, 80, "request"));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningFailed(tenantId, 7, "request", "ERROR", false));
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested(tenantId, "request", 7, 1, "UTC", "USD"));
    }

    @ParameterizedTest @ValueSource(longs = {0, -1})
    void numericIdentifiersMustBePositive(long id) {
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningCompleted("42", id, 90, 80, "request"));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningCompleted("42", 7, id, 80, "request"));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningCompleted("42", 7, 90, id, "request"));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningFailed("42", id, "request", "ERROR", false));
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested("42", "request", id, 1, "UTC", "USD"));
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested("42", "request", 7, id, "UTC", "USD"));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {" "})
    void requiredTextMustNotBeBlank(String value) {
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningCompleted("42", 7, 90, 80, value));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningFailed("42", 7, value, "ERROR", false));
        assertThrows(IllegalArgumentException.class, () -> new IamTenantProvisioningFailed("42", 7, "request", value, false));
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested("42", value, 7, 1, "UTC", "USD"));
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested("42", "request", 7, 1, value, "USD"));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"usd", "US", "USDD", "123"})
    void currencyMatchesTheRequestedSchema(String currency) {
        assertThrows(IllegalArgumentException.class, () -> new TenantProvisioningRequested("42", "request", 7, 1, "UTC", currency));
    }
}
