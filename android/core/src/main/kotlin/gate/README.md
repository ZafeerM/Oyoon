# gate/

Validity gate: decides whether a frame or snapshot is good enough to measure.

Now (`Gate.kt`, guide section 4.2): `GateConfig` (all limits), `RejectReason`, `GateResult`
(`Accepted` / `Rejected(reason)`), `GateState` (last 15 mean iris sizes) and
`gate(sample, cfg, state)`. Start values and extra rules: `docs/decisions.md`.
