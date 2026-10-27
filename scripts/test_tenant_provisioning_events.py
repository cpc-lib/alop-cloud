import copy
import json
import unittest
from pathlib import Path

import yaml
from jsonschema import Draft202012Validator, FormatChecker, ValidationError


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "spec" / "alop-v7.0" / "05-events" / "schemas"
REGISTRY_PATH = ROOT / "spec" / "alop-v7.0" / "05-events" / "event-registry.yaml"
REGISTRY_MD_PATH = ROOT / "spec" / "alop-v7.0" / "05-events" / "registry.md"

CONTRACTS = {
    "tenant.provisioning.requested.v1": {
        "schema": "tenant-provisioning-requested-v1.schema.json",
        "producer": "alop-tenant",
        "consumers": ["alop-iam"],
        "data": {
            "tenantId": "1001",
            "tenantCreateRequestId": "req-1001",
            "adminUserId": 42,
            "tenantPackageId": 7,
            "timezone": "UTC",
            "currency": "USD",
        },
    },
    "iam.tenant.provisioning.completed.v1": {
        "schema": "iam-tenant-provisioning-completed-v1.schema.json",
        "producer": "alop-iam",
        "consumers": ["alop-tenant"],
        "data": {
            "tenantId": "1001",
            "adminUserId": 42,
            "membershipId": 10001,
            "adminRoleId": 10002,
            "tenantCreateRequestId": "req-1001",
        },
    },
    "iam.tenant.provisioning.failed.v1": {
        "schema": "iam-tenant-provisioning-failed-v1.schema.json",
        "producer": "alop-iam",
        "consumers": ["alop-tenant"],
        "data": {
            "tenantId": "1001",
            "adminUserId": 42,
            "tenantCreateRequestId": "req-1001",
            "errorCode": "TENANT_ADMIN_ROLE_CONFLICT",
            "retryable": False,
        },
    },
    "tenant.tenant.created.v1": {
        "schema": "tenant-created-v1.schema.json",
        "producer": "alop-tenant",
        "consumers": [],
        "data": {
            "tenantId": "1001",
            "tenantCreateRequestId": "req-1001",
            "tenantCode": "acme",
            "tenantName": "Acme",
            "tenantType": "CUSTOMER",
            "packageId": 7,
            "adminUserId": 42,
            "timezone": "UTC",
            "currency": "USD",
        },
    },
}


def load_schema(event_type):
    schema_path = SCHEMA_DIR / CONTRACTS[event_type]["schema"]
    return json.loads(schema_path.read_text(encoding="utf-8"))


def valid_envelope(event_type):
    return {
        "eventId": f"event-{event_type}",
        "eventType": event_type,
        "eventVersion": "1.0",
        "aggregateType": "Tenant",
        "aggregateId": "1001",
        "aggregateVersion": 0,
        "tenantId": "1001",
        "traceId": "trace-1001",
        "correlationId": "req-1001",
        "occurredAt": "2026-10-27T12:00:00Z",
        "data": copy.deepcopy(CONTRACTS[event_type]["data"]),
    }


class TenantProvisioningEventContractTest(unittest.TestCase):
    def assert_valid(self, event_type, event):
        schema = load_schema(event_type)
        Draft202012Validator(schema, format_checker=FormatChecker()).validate(event)

    def assert_invalid(self, event_type, event):
        schema = load_schema(event_type)
        with self.assertRaises(ValidationError):
            Draft202012Validator(schema, format_checker=FormatChecker()).validate(event)

    def test_valid_envelope_for_each_event(self):
        for event_type in CONTRACTS:
            with self.subTest(event_type=event_type):
                self.assert_valid(event_type, valid_envelope(event_type))

    def test_missing_tenant_id_is_rejected(self):
        event_type = "tenant.provisioning.requested.v1"
        event = valid_envelope(event_type)
        del event["tenantId"]
        self.assert_invalid(event_type, event)

    def test_wrong_event_type_is_rejected(self):
        event_type = "iam.tenant.provisioning.completed.v1"
        event = valid_envelope(event_type)
        event["eventType"] = "tenant.provisioning.requested.v1"
        self.assert_invalid(event_type, event)

    def test_missing_required_data_is_rejected(self):
        event_type = "iam.tenant.provisioning.failed.v1"
        event = valid_envelope(event_type)
        del event["data"]["errorCode"]
        self.assert_invalid(event_type, event)

    def test_extra_data_property_is_rejected(self):
        event_type = "tenant.tenant.created.v1"
        event = valid_envelope(event_type)
        event["data"]["unexpected"] = "not-part-of-contract"
        self.assert_invalid(event_type, event)

    def test_registry_contains_contract_metadata_and_schema_paths(self):
        registry = yaml.safe_load(REGISTRY_PATH.read_text(encoding="utf-8"))
        entries = {entry["eventType"]: entry for entry in registry["events"]}
        registry_markdown = REGISTRY_MD_PATH.read_text(encoding="utf-8")
        self.assertEqual(len(registry["events"]), len(entries))

        for event_type, contract in CONTRACTS.items():
            with self.subTest(event_type=event_type):
                self.assertIn(event_type, entries)
                entry = entries[event_type]
                self.assertEqual("1.0", entry["version"])
                self.assertEqual(contract["producer"], entry["producer"])
                self.assertEqual(contract["consumers"], entry["consumers"])
                self.assertEqual(
                    f"05-events/schemas/{contract['schema']}", entry["schema"]
                )
                self.assertEqual("at-least-once", entry["delivery"])
                self.assertEqual("Inbox", entry["consumerIdempotency"])
                self.assertIn(
                    f"| {event_type} | {contract['producer']} |",
                    registry_markdown,
                )
                self.assertIn(contract["schema"], registry_markdown)


if __name__ == "__main__":
    unittest.main()
