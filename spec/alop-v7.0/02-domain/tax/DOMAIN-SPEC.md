# Tax Domain SPEC

## Scope
Centralize tax categories, effective-dated rules and tax snapshots used by Billing, Invoice, Finance and AP.

Models: TaxCategory, TaxRule, TaxCalculationSnapshot.

Categories:
RENT, PROPERTY_MANAGEMENT, PARKING, WATER, ELECTRICITY, SERVICE, DAMAGE, OTHER.

Modes:
TAX_EXCLUSIVE, TAX_INCLUSIVE, NON_TAXABLE.

## Rule
TaxRule contains tenant/platform scope, jurisdiction, category, rate, mode,
effective_from/effective_to, version and status.

Historical Bill/Invoice stores:
tax_category, tax_rate_snapshot, tax_mode_snapshot, net_amount, tax_amount, gross_amount.

## Invariants
- no overlapping ACTIVE rules for same scope/jurisdiction/category
- ACTIVE rules immutable
- old invoices never recalculate with a new rate
- MoneyRoundingPolicy shared with Billing
- net/tax/gross must balance exactly at configured precision

## APIs
GET/POST /api/admin/v1/tax/rules
POST /api/admin/v1/tax/rules/{id}/publish
POST /internal/v1/tax/calculate

## Permissions
tax:rule:view, tax:rule:manage, tax:rule:publish
