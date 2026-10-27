"""Standard-library contract checks; no pip install is required.

The YAML reader accepts the registry/generator's flat mappings and string lists,
not general YAML. Unsupported syntax fails explicitly rather than being ignored.
"""

import ast
import copy
import json
import re
import unittest
from datetime import datetime
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SPEC_ROOT = ROOT / "spec" / "alop-v7.0"
REGISTRY_PATH = SPEC_ROOT / "05-events" / "event-registry.yaml"
REGISTRY_MD_PATH = SPEC_ROOT / "05-events" / "registry.md"
MATRIX_PATH = SPEC_ROOT / "11-codegen/EVENT-PRODUCER-CONSUMER-MATRIX.yaml"


def load_event_yaml(path):
    document = {}
    current = document
    sequence = None
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.startswith("#"):
            continue
        if line.startswith("- eventType: "):
            current = {}
            document["events"].append(current)
            line = "  " + line[2:]
        elif line.startswith("  - "):
            if sequence is None or not re.fullmatch(r"[a-z0-9.-]+", line[4:]):
                raise ValueError(f"{path}:{number}: unsupported list item")
            sequence.append(line[4:])
            continue
        match = re.fullmatch(r"(  )?([A-Za-z][A-Za-z0-9-]*):(?: (.+))?", line)
        if not match:
            raise ValueError(f"{path}:{number}: unsupported YAML: {line}")
        indent, key, value = match.groups()
        if not indent:
            current = document
        if key in current:
            raise ValueError(f"{path}:{number}: duplicate key: {key}")
        sequence = None
        if value is None:
            if key == "byProducer" and not indent:
                current[key] = {}
                current = current[key]
            else:
                current[key] = sequence = []
        else:
            if value.startswith(("'", '"', "[")):
                value = ast.literal_eval(value)
                if not isinstance(value, str) and value != []:
                    raise ValueError(f"{path}:{number}: unsupported scalar")
            elif value.isdecimal():
                value = int(value)
            elif not re.fullmatch(r"[A-Za-z0-9_./-]+", value):
                raise ValueError(f"{path}:{number}: unsupported scalar")
            current[key] = value
    return document


CONTRACTS = {
    "tenant.provisioning.requested.v1": {
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
        "data": {
            "tenantId": "1001",
            "adminUserId": 42,
            "membershipId": 10001,
            "adminRoleId": 10002,
            "tenantCreateRequestId": "req-1001",
        },
    },
    "iam.tenant.provisioning.failed.v1": {
        "data": {
            "tenantId": "1001",
            "adminUserId": 42,
            "tenantCreateRequestId": "req-1001",
            "errorCode": "TENANT_ADMIN_ROLE_CONFLICT",
            "retryable": False,
        },
    },
    "tenant.tenant.created.v1": {
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
    registry = load_event_yaml(REGISTRY_PATH)
    entry = next(entry for entry in registry["events"] if entry["eventType"] == event_type)
    schema_path = SPEC_ROOT / entry["schema"]
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
        self.assert_contract(load_schema(event_type), event)

    def assert_contract(self, schema, value):
        # Contract checks for the keywords used by these four events, not a
        # replacement for a general JSON Schema validator.
        supported = {
            "$schema", "$id", "title", "type", "const", "enum", "required",
            "properties", "additionalProperties", "pattern", "minLength",
            "format", "minimum",
        }
        self.assertFalse(schema.keys() - supported, "unsupported contract keyword")
        if "const" in schema:
            self.assertEqual(schema["const"], value)
        if "enum" in schema:
            self.assertIn(value, schema["enum"])
        if "type" in schema:
            types = schema["type"]
            if isinstance(types, str):
                types = [types]
            python_types = {
                "object": dict, "string": str, "integer": int,
                "boolean": bool, "null": type(None),
            }
            self.assertIn(type(value), [python_types[kind] for kind in types])
        if isinstance(value, dict):
            self.assertFalse(set(schema.get("required", [])) - value.keys())
            properties = schema.get("properties", {})
            if schema.get("additionalProperties") is False:
                self.assertFalse(value.keys() - properties.keys())
            for key in value.keys() & properties.keys():
                self.assert_contract(properties[key], value[key])
        if isinstance(value, str):
            if "pattern" in schema:
                self.assertIsNotNone(re.search(schema["pattern"], value))
            if "minLength" in schema:
                self.assertGreaterEqual(len(value), schema["minLength"])
            if "format" in schema:
                self.assertEqual("date-time", schema["format"])
                self.assertRegex(
                    value,
                    r"^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?(?:Z|[+-]\d\d:\d\d)$",
                )
                datetime.fromisoformat(value.replace("Z", "+00:00"))
        if "minimum" in schema:
            self.assertGreaterEqual(value, schema["minimum"])

    def assert_invalid(self, event_type, event):
        with self.assertRaises(AssertionError):
            self.assert_valid(event_type, event)

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

    def test_wrong_event_version_is_rejected(self):
        for event_type in CONTRACTS:
            with self.subTest(event_type=event_type):
                event = valid_envelope(event_type)
                event["eventVersion"] = "9.9"
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
        registry = load_event_yaml(REGISTRY_PATH)
        entries = {entry["eventType"]: entry for entry in registry["events"]}
        self.assertEqual("7.0", registry["version"])
        self.assertEqual(len(registry["events"]), len(entries))
        rows = []
        table = REGISTRY_MD_PATH.read_text(encoding="utf-8").split(
            "| Event Type | Producer | Consumers | Schema |\n", 1
        )[1].split("\n\n", 1)[0]
        for line in table.splitlines()[1:]:
            cells = [cell.strip() for cell in line.strip("|").split("|")]
            self.assertEqual(4, len(cells), line)
            event_type, producer, consumers, schema = cells
            # Markdown encodes the event major version in its .vN suffix.
            version = re.search(r"\.v([1-9][0-9]*)$", event_type)
            self.assertIsNotNone(version, event_type)
            rows.append({
                "eventType": event_type,
                "version": f"{version.group(1)}.0",
                "producer": producer,
                "consumers": [] if consumers == "(none registered)" else consumers.split(", "),
                "schema": f"05-events/schemas/{schema}",
            })
        expected = [
            {key: entry[key] for key in ("eventType", "version", "producer", "consumers", "schema")}
            for entry in registry["events"]
        ]
        self.assertEqual(
            sorted(expected, key=lambda row: row["eventType"]),
            sorted(rows, key=lambda row: row["eventType"]),
        )

        for event_type, entry in entries.items():
            with self.subTest(event_type=event_type):
                schema_path = SPEC_ROOT / entry["schema"]
                self.assertTrue(schema_path.is_file(), str(schema_path))
                schema = json.loads(schema_path.read_text(encoding="utf-8"))
                properties = schema["properties"]
                self.assertEqual(entry["version"], properties["eventVersion"]["const"])
                event_schema = properties["eventType"]
                if "const" in event_schema:
                    self.assertEqual(event_type, event_schema["const"])
                    if "$id" in schema:
                        self.assertEqual(event_type, schema["$id"])
                else:
                    # registry.md explicitly documents this one shared schema.
                    self.assertEqual(
                        "05-events/schemas/notification-delivery-result-v1.schema.json",
                        entry["schema"],
                    )
                    self.assertEqual("notification.delivery.result.v1", schema["$id"])
                    self.assertCountEqual(
                        [item["eventType"] for item in entries.values() if item["schema"] == entry["schema"]],
                        event_schema["enum"],
                    )
                    self.assertIn(event_type, event_schema["enum"])
                self.assertEqual("at-least-once", entry["delivery"])
                self.assertEqual("Inbox", entry["consumerIdempotency"])

    def test_generated_matrix_matches_registry(self):
        registry = load_event_yaml(REGISTRY_PATH)
        matrix = load_event_yaml(MATRIX_PATH)
        expected = [
            {key: entry[key] for key in ("eventType", "producer", "consumers")}
            for entry in registry["events"]
        ]
        self.assertEqual(expected, matrix["events"])
        self.assertEqual(registry["version"], matrix["version"])
        self.assertEqual("05-events/event-registry.yaml", matrix["source"])
        self.assertEqual("scripts/generate_event_matrix.py", matrix["generatedBy"])
        self.assertEqual(len(expected), matrix["eventCount"])
        by_producer = {}
        for entry in expected:
            by_producer.setdefault(entry["producer"], []).append(entry["eventType"])
        self.assertEqual(by_producer, matrix["byProducer"])


if __name__ == "__main__":
    unittest.main()
