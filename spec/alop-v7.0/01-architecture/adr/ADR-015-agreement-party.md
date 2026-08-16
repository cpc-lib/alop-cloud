# ADR-015 — Agreement Party Model

Decision: signed agreements use explicit effective-dated `agreement_party`, not a single `customer_id` assumption.

Reason: payer, lessee, owner, guarantor and invoice party frequently differ.
