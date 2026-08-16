CREATE TABLE parking_vehicle_binding (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agreement_id BIGINT NOT NULL,
    agreement_item_id BIGINT NOT NULL,
    parking_resource_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    effective_from DATETIME(3) NOT NULL,
    effective_to DATETIME(3) NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    version INT NOT NULL DEFAULT 0,
    KEY idx_parking_binding_agreement (tenant_id, agreement_id, status),
    KEY idx_parking_binding_resource (tenant_id, parking_resource_id, status, effective_from, effective_to),
    KEY idx_parking_binding_vehicle (tenant_id, vehicle_id, status)
);
