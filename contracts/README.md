# contracts/

Shared by everyone and **owned by QA**. This is the only thing `android/` and `server/` share.

- `openapi.yaml`: API contract
- `snapshot.schema.json`: snapshot file format
- `tolerance-bands.json`: fit tolerances, each with its source
- `fixtures/`: sample snapshots, catalogue, photos

Changes here affect both sides. Get QA review before merging.
