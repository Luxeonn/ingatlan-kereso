"""
Sidecar JSON kimeneti adatmodellek.
A Java oldal (SidecarCliExecutor) ezeket a struktúrákat deszerializálja Gson-nal.
"""

from __future__ import annotations

from pydantic import BaseModel, Field


class SidecarItem(BaseModel):
    """Egyetlen hirdetés a sidecar kimenetében."""

    id: str = ""
    url: str = ""
    title: str = ""
    price_million: float | None = Field(None, alias="priceMillion")
    price_raw: str = Field("", alias="priceRaw")
    area_square_meters: float | None = Field(None, alias="areaSquareMeters")
    room_count: float | None = Field(None, alias="roomCount")

    model_config = {"populate_by_name": True}


class SidecarResponse(BaseModel):
    """A sidecar teljes stdout JSON kimenete."""

    portal_id: str = Field("", alias="portalId")
    status: str = "SUCCESS"  # "SUCCESS" | "FAILED"
    items: list[SidecarItem] = []
    error_message: str | None = Field(None, alias="errorMessage")
    elapsed_ms: int = Field(0, alias="elapsedMs")

    model_config = {"populate_by_name": True}
