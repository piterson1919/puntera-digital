from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import Response
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import Column, Integer, String, Text, DateTime, create_engine, func
from sqlalchemy.orm import declarative_base, sessionmaker
from typing import List, Optional, Dict, Any
import asyncio
import json
from fastapi import WebSocket, WebSocketDisconnect
from contextlib import asynccontextmanager
from datetime import datetime

DATABASE_URL = "sqlite:///./inventory_sync.db"

engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})
SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)
Base = declarative_base()


class InventoryItem(Base):
    __tablename__ = "inventory_items"


    id = Column(Integer, primary_key=True, index=True)
    uuid = Column(String, unique=True, index=True, nullable=False)
    origin = Column(String, nullable=False)
    model = Column(String, nullable=False)
    size = Column(String, nullable=False)
    lot = Column(String, nullable=False)
    status = Column(String, nullable=False, default="AVAILABLE")
    location = Column(String, nullable=False, default="RACK")
    entry_type = Column(String, nullable=False, default="PRODUCCION")
    parent_uuid = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    payload = Column(Text, nullable=True)


class InventoryEvent(Base):
    __tablename__ = "inventory_events"

    id = Column(Integer, primary_key=True, index=True)
    event_id = Column(String, unique=True, index=True, nullable=False)
    entity_type = Column(String, nullable=False)
    action = Column(String, nullable=False)
    payload = Column(Text, nullable=False)
    user_id = Column(String, nullable=False)
    device_id = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)


class CatalogModel(Base):
    __tablename__ = "catalog_models"

    id = Column(String, primary_key=True, index=True)
    code = Column(String, nullable=False, unique=True, index=True)
    name = Column(String, nullable=False)
    size_min = Column(Integer, nullable=False, default=36)
    size_max = Column(Integer, nullable=False, default=46)
    pairs_per_box = Column(Integer, nullable=False, default=8)
    is_active = Column(Integer, nullable=False, default=1)
    image_uri = Column(String, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


class User(Base):
    __tablename__ = "users"

    id = Column(String, primary_key=True, index=True)
    name = Column(String, nullable=False)
    pin = Column(String, nullable=False)
    role = Column(String, nullable=False)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)


Base.metadata.create_all(bind=engine)

connected_clients = set()


class InventoryEventDto(BaseModel):
    eventId: str
    entityType: str
    action: str
    payload: Dict[str, Any]
    userId: str
    deviceId: str
    timestamp: Optional[int] = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="Inventory Sync Server", lifespan=lifespan)


@app.middleware("http")
async def log_http_requests(request: Request, call_next):
    started_at = asyncio.get_running_loop().time()
    try:
        response = await call_next(request)
    except Exception as exc:
        elapsed_ms = (asyncio.get_running_loop().time() - started_at) * 1000
        print(
            f"[HTTP] {request.method} {request.url.path} -> ERROR {type(exc).__name__} "
            f"({elapsed_ms:.1f} ms)",
            flush=True,
        )
        raise

    elapsed_ms = (asyncio.get_running_loop().time() - started_at) * 1000
    print(
        f"[HTTP] {request.method} {request.url.path} -> {response.status_code} "
        f"({elapsed_ms:.1f} ms)",
        flush=True,
    )
    return response

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


async def broadcast_event(event: InventoryEventDto):
    message = json.dumps(event.model_dump())
    dead_clients = set()
    for ws in list(connected_clients):
        try:
            await ws.send_text(message)
        except Exception:
            dead_clients.add(ws)
    for ws in dead_clients:
        connected_clients.discard(ws)


@app.websocket("/ws/inventory")
async def inventory_websocket(websocket: WebSocket):
    await websocket.accept()
    connected_clients.add(websocket)
    try:
        while True:
            await websocket.receive_text()
    except WebSocketDisconnect:
        connected_clients.discard(websocket)


@app.get("/health")
async def health_check():
    return {"status": "ok", "timestamp": datetime.utcnow().isoformat()}


@app.get("/api/inventory")
async def get_inventory_snapshot():
    db = SessionLocal()
    try:
        items = db.query(InventoryItem).order_by(InventoryItem.updated_at.desc()).all()
        return [
            {
                "uuid": item.uuid,
                "origin": item.origin,
                "model": item.model,
                "size": item.size,
                "lot": item.lot,
                "status": item.status,
                "location": item.location,
                "entryType": item.entry_type,
                "parentUuid": item.parent_uuid,
                "createdAt": int(item.created_at.timestamp() * 1000) if item.created_at else 0,
                "updatedAt": int(item.updated_at.timestamp() * 1000) if item.updated_at else 0,
                "payload": item.payload or "{}",
            }
            for item in items
        ]
    finally:
        db.close()


@app.get("/api/catalog/models")
async def get_catalog_models():
    db = SessionLocal()
    try:
        models = db.query(CatalogModel).order_by(CatalogModel.name.asc()).all()
        return [
            {
                "id": model.id,
                "code": model.code,
                "name": model.name,
                "sizeMin": model.size_min,
                "sizeMax": model.size_max,
                "pairsPerBox": model.pairs_per_box,
                "isActive": bool(model.is_active),
                "imageUri": (
                    f"/api/catalog/models/{model.id}/image"
                    if model.image_uri and model.image_uri.startswith("data:")
                    else model.image_uri
                ),
                "createdAt": int(model.created_at.timestamp() * 1000) if model.created_at else 0,
                "updatedAt": int(model.updated_at.timestamp() * 1000) if model.updated_at else 0,
            }
            for model in models
        ]
    finally:
        db.close()


@app.get("/api/users")
async def get_users():
    db = SessionLocal()
    try:
        users = db.query(User).order_by(User.name.asc()).all()
        return [
            {
                "id": user.id,
                "name": user.name,
                "pin": user.pin,
                "role": user.role,
                "createdAt": int(user.created_at.timestamp() * 1000) if user.created_at else 0,
                "updatedAt": int(user.updated_at.timestamp() * 1000) if user.updated_at else 0,
            }
            for user in users
        ]
    finally:
        db.close()


@app.get("/api/catalog/models/{model_id}/image")
async def get_catalog_model_image(model_id: str):
    db = SessionLocal()
    try:
        model = db.query(CatalogModel).filter(CatalogModel.id == model_id).first()
        if not model or not model.image_uri or not model.image_uri.startswith("data:"):
            raise HTTPException(status_code=404, detail="catalog model image not found")

        header, encoded_image = model.image_uri.split(",", 1)
        media_type = header[5:].split(";", 1)[0] or "application/octet-stream"
        import base64
        return Response(content=base64.b64decode(encoded_image), media_type=media_type)
    finally:
        db.close()


@app.post("/api/inventory/sync")
async def sync_inventory_event(event: InventoryEventDto):
    db = SessionLocal()
    print(f"[SYNC] {event.entityType}/{event.action} from {event.userId} on {event.deviceId}")
    try:
        existing = db.query(InventoryEvent).filter(InventoryEvent.event_id == event.eventId).first()
        if existing:
            print(f"[SYNC] duplicate event ignored: {event.eventId}")
            return {"status": "duplicate", "message": "event ignored"}

        db_event = InventoryEvent(
            event_id=event.eventId,
            entity_type=event.entityType,
            action=event.action,
            payload=json.dumps(event.payload),
            user_id=event.userId,
            device_id=event.deviceId,
        )
        db.add(db_event)
        db.commit()

        if event.entityType == "product":
            payload = event.payload
            uuid = payload.get("uuid")
            if not uuid:
                raise HTTPException(status_code=400, detail="uuid required")

            item = db.query(InventoryItem).filter(InventoryItem.uuid == uuid).first()
            if event.action in ("create", "upsert"):
                if item is None:
                    item = InventoryItem(uuid=uuid)
                item.origin = payload.get("origin", item.origin or "FOOT_SAFE")
                item.model = payload.get("model", item.model or "")
                item.size = payload.get("size", item.size or "")
                item.lot = payload.get("lot", item.lot or "")
                item.status = payload.get("status", item.status or "AVAILABLE")
                item.location = payload.get("location", item.location or "RACK")
                item.entry_type = payload.get("entryType", item.entry_type or "PRODUCCION")
                item.parent_uuid = payload.get("parentUuid")
                item.payload = json.dumps(payload)
                item.updated_at = datetime.utcnow()
                if item.created_at is None:
                    item.created_at = datetime.utcnow()
                db.add(item)
            elif event.action == "delete":
                if item is not None:
                    db.delete(item)

        elif event.entityType == "movement":
            payload = event.payload
            if not payload.get("uuid"):
                raise HTTPException(status_code=400, detail="uuid required")

        elif event.entityType == "catalog_model":
            payload = event.payload
            model_id = payload.get("id") or payload.get("code")
            if not model_id:
                raise HTTPException(status_code=400, detail="catalog model id required")

            model = db.query(CatalogModel).filter(CatalogModel.id == model_id).first()
            if not model:
                model = CatalogModel(id=model_id, code=payload.get("code", model_id))

            model.code = payload.get("code", model.code or model_id)
            model.name = payload.get("name", model.name or "")
            model.size_min = int(payload.get("sizeMin", model.size_min or 36))
            model.size_max = int(payload.get("sizeMax", model.size_max or 46))
            model.pairs_per_box = int(payload.get("pairsPerBox", model.pairs_per_box or 8))
            model.is_active = 1 if str(payload.get("isActive", model.is_active == 1)).lower() in ("true", "1", "yes") else 0
            model.image_uri = payload.get("imageUri") or model.image_uri
            image_data = payload.get("imageData")
            if image_data:
                model.image_uri = image_data
            model.updated_at = datetime.utcnow()
            if model.created_at is None:
                model.created_at = datetime.utcnow()
            db.add(model)

            if event.action == "delete":
                db.delete(model)

        elif event.entityType == "user":
            payload = event.payload
            user_id = payload.get("id")
            if not user_id:
                raise HTTPException(status_code=400, detail="user id required")

            user = db.query(User).filter(User.id == user_id).first()
            if event.action in ("create", "upsert"):
                if user is None:
                    user = User(id=user_id)
                user.name = payload.get("name", user.name if user.name is not None else "")
                user.pin = payload.get("pin", user.pin if user.pin is not None else "")
                user.role = payload.get("role", user.role if user.role is not None else "OPERADOR")
                user.updated_at = datetime.utcnow()
                if user.created_at is None:
                    user.created_at = datetime.utcnow()
                db.add(user)
            elif event.action == "delete":
                if user is not None:
                    db.delete(user)

        db.commit()
        await broadcast_event(event)
        print(f"[SYNC] accepted and saved {event.entityType}/{event.action}")
        return {"status": "ok"}
    except Exception as exc:
        db.rollback()
        print(f"[SYNC] ERROR {event.entityType}/{event.action}: {exc}")
        raise HTTPException(status_code=500, detail=str(exc))
    finally:
        db.close()


@app.post("/api/inventory/sync/batch")
async def sync_inventory_batch(events: List[InventoryEventDto]):
    for event in events:
        await sync_inventory_event(event)
    return {"status": "ok", "count": len(events)}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="192.168.0.121", port=8081, reload=False)