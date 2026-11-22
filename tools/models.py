"""
Sidecar JSON kimeneti adatmodellek (stdlib dataclass, külső függőség nélkül).
A Java oldal (SidecarCliExecutor) ezeket a struktúrákat deszerializálja Jackson-nal.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass(init=False)
class SidecarItem:
    """Egyetlen hirdetés a sidecar kimenetében."""

    id: str = ""
    url: str = ""
    title: str = ""
    price_million: float | None = None
    price_raw: str = ""
    area_square_meters: float | None = None
    room_count: float | None = None
    image_url: str | None = None

    def __init__(
        self,
        id: str = "",
        url: str = "",
        title: str = "",
        price_million: float | None = None,
        price_raw: str = "",
        area_square_meters: float | None = None,
        room_count: float | None = None,
        image_url: str | None = None,
        *,
        priceMillion: float | None = None,
        priceRaw: str | None = None,
        areaSquareMeters: float | None = None,
        roomCount: float | None = None,
        imageUrl: str | None = None,
    ) -> None:
        self.id = id
        self.url = url
        self.title = title
        self.price_million = priceMillion if priceMillion is not None else price_million
        self.price_raw = priceRaw if priceRaw is not None else price_raw
        self.area_square_meters = (
            areaSquareMeters if areaSquareMeters is not None else area_square_meters
        )
        self.room_count = roomCount if roomCount is not None else room_count
        self.image_url = imageUrl if imageUrl is not None else image_url

    def model_dump(self, *, by_alias: bool = False) -> dict[str, Any]:
        if by_alias:
            return {
                "id": self.id,
                "url": self.url,
                "title": self.title,
                "priceMillion": self.price_million,
                "priceRaw": self.price_raw,
                "areaSquareMeters": self.area_square_meters,
                "roomCount": self.room_count,
                "imageUrl": self.image_url,
            }
        return {
            "id": self.id,
            "url": self.url,
            "title": self.title,
            "price_million": self.price_million,
            "price_raw": self.price_raw,
            "area_square_meters": self.area_square_meters,
            "room_count": self.room_count,
            "image_url": self.image_url,
        }


@dataclass(init=False)
class SidecarResponse:
    """A sidecar teljes stdout JSON kimenete."""

    portal_id: str = ""
    status: str = "SUCCESS"  # "SUCCESS" | "FAILED"
    items: list[SidecarItem] = field(default_factory=list)
    error_message: str | None = None
    elapsed_ms: int = 0
    pages_processed: int = 0
    stop_reason: str | None = None

    def __init__(
        self,
        portal_id: str = "",
        status: str = "SUCCESS",
        items: list[SidecarItem] | None = None,
        error_message: str | None = None,
        elapsed_ms: int = 0,
        pages_processed: int = 0,
        stop_reason: str | None = None,
        *,
        portalId: str | None = None,
        errorMessage: str | None = None,
        elapsedMs: int | None = None,
        pagesProcessed: int | None = None,
        stopReason: str | None = None,
    ) -> None:
        self.portal_id = portalId if portalId is not None else portal_id
        self.status = status
        self.items = list(items) if items is not None else []
        self.error_message = errorMessage if errorMessage is not None else error_message
        self.elapsed_ms = elapsedMs if elapsedMs is not None else elapsed_ms
        self.pages_processed = (
            pagesProcessed if pagesProcessed is not None else pages_processed
        )
        self.stop_reason = stopReason if stopReason is not None else stop_reason

    def model_dump(self, *, by_alias: bool = False) -> dict[str, Any]:
        serialized_items = [item.model_dump(by_alias=by_alias) for item in self.items]
        if by_alias:
            return {
                "portalId": self.portal_id,
                "status": self.status,
                "items": serialized_items,
                "errorMessage": self.error_message,
                "elapsedMs": self.elapsed_ms,
                "pagesProcessed": self.pages_processed,
                "stopReason": self.stop_reason,
            }
        return {
            "portal_id": self.portal_id,
            "status": self.status,
            "items": serialized_items,
            "error_message": self.error_message,
            "elapsed_ms": self.elapsed_ms,
            "pages_processed": self.pages_processed,
            "stop_reason": self.stop_reason,
        }
