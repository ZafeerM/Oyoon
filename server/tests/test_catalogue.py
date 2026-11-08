import json

from fastapi.testclient import TestClient
from sqlalchemy.orm import Session

from app.config import settings
from app.models.tables import Frame

CATALOGUE = json.loads((settings.contracts_dir / "fixtures" / "catalogue.json").read_text(encoding="utf-8"))
BANDS = json.loads((settings.contracts_dir / "tolerance-bands.json").read_text(encoding="utf-8"))


def test_health(client: TestClient) -> None:
    assert client.get("/health").json() == {"status": "ok"}


def test_list_returns_all_seeded_frames(client: TestClient) -> None:
    response = client.get("/frames")
    assert response.status_code == 200
    assert {f["id"] for f in response.json()} == {f["id"] for f in CATALOGUE if f["approved"]}


def test_frame_has_guide_fields(client: TestClient) -> None:
    frame = client.get(f"/frames/{CATALOGUE[0]['id']}").json()
    assert set(frame) == {
        "id", "name", "lens_width_mm", "lens_height_mm", "bridge_mm", "temple_length_mm",
        "total_width_mm", "shape", "material", "colour", "price_rs", "sprite_url", "anchors",
    }
    assert frame["name"] == CATALOGUE[0]["name"]


def test_unknown_id_returns_404(client: TestClient) -> None:
    assert client.get("/frames/does-not-exist").status_code == 404


def test_unapproved_frame_is_hidden(client: TestClient, db: Session) -> None:
    frame = db.get(Frame, CATALOGUE[0]["id"])
    frame.approved = False
    db.commit()
    assert CATALOGUE[0]["id"] not in {f["id"] for f in client.get("/frames").json()}
    assert client.get(f"/frames/{CATALOGUE[0]['id']}").status_code == 404


def test_sprite_url_is_built_from_sprite_key(client: TestClient, db: Session) -> None:
    frame = db.get(Frame, CATALOGUE[0]["id"])
    frame.sprite_key = "f-sample-1/front.png"
    db.commit()
    body = client.get(f"/frames/{frame.id}").json()
    assert body["sprite_url"] == f"{settings.sprite_base_url}/f-sample-1/front.png"


def test_tolerance_bands_match_contracts_file(client: TestClient) -> None:
    response = client.get("/tolerance-bands")
    assert response.status_code == 200
    assert sorted(response.json(), key=lambda b: b["check"]) == sorted(BANDS, key=lambda b: b["check"])
