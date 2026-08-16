# Utility Usage Period SPEC

## Goal
Utility bills consume auditable usage-period snapshots instead of subtracting two arbitrary readings at bill time.

## Model
`UtilityUsagePeriod`

Fields:
- id, tenant_id
- agreement_id, agreement_item_id
- resource_unit_id, meter_id, utility_type
- period_start, period_end
- start_reading_id, end_reading_id
- start_value, end_value, meter_multiplier
- raw_usage, adjusted_usage
- allocation_method, allocation_factor
- billable_usage
- tariff_plan_id, tariff_version
- estimated_flag
- correction_of_usage_period_id
- calculation_trace_json
- status, version

Status:
DRAFT -> CALCULATED -> VERIFIED -> BILLED.
Alternative: CALCULATED -> REVIEW_REQUIRED.
Correction creates a new corrected usage period; the billed record remains immutable.

## Edge cases
meter replacement; rollover/reset; multiplier change; negative delta anomaly;
estimated/supplemental reading; shared master meter; submeter; area/fixed/manual allocation;
time-of-use electricity; tiered tariff; move-in and move-out readings.

## Meter replacement
Split usage at replacement time:
old final reading -> replacement record -> new initial reading.
Never overwrite old meter history.

## Correction
If already billed:
new reading/version -> new UtilityUsagePeriod referencing original
-> Utility Adjustment BillItem/Receivable.
Never edit historical billed amount.

## Invariants
- period_start < period_end
- billable_usage >= 0 except explicit negative adjustment documents
- tariff version is snapshotted
- one usage period cannot be billed twice
- meter/readings/agreement/tariff belong to same tenant

## Events
billing.utility-usage.calculated.v1
billing.utility-usage.corrected.v1
