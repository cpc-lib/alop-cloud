# UTILITY / PARKING STATE MACHINES

## UtilityMeter
`DRAFT -> ACTIVE -> SUSPENDED -> ACTIVE -> REPLACED/RETIRED`

## MeterReading
| Current | Action | Target |
|---|---|---|
| DRAFT | SUBMIT | SUBMITTED |
| SUBMITTED | AUTO_VALIDATE_OK | VERIFIED |
| SUBMITTED | ANOMALY_FOUND | REVIEW_REQUIRED |
| REVIEW_REQUIRED | APPROVE | VERIFIED |
| REVIEW_REQUIRED | REJECT | REJECTED |
| VERIFIED | MARK_BILLED | BILLED |
| VERIFIED/BILLED | CORRECT | CORRECTED (old) + new DRAFT version |

Rules:
- `BILLED` is historical fact; correction creates a new version.
- Only VERIFIED/BILLED reading can drive billing.

## ParkingVehicleBinding
`PENDING -> ACTIVE -> ENDED/CANCELLED`。Change vehicle = end old ACTIVE + create new binding; no in-place overwrite of historical plate association.

## Parking Occupancy
Uses common Reservation/Occupancy state machines; PARKING_SPACE has no separate inventory truth.
