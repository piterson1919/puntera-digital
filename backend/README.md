# Backend local de sincronización para Puntera Digital

Este backend usa:
- FastAPI
- SQLite
- WebSocket para sincronización en tiempo real
- Red local en 192.168.0.121

## Ejecutar

1. Crear entorno virtual
   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   ```

2. Instalar dependencias
   ```bash
   pip install -r requirements.txt
   ```

3. Iniciar servidor
   ```bash
   python app.py
   ```

4. El servidor quedará disponible en:
   - HTTP: http://192.168.0.121:8081
   - WebSocket: ws://192.168.0.121:8081/ws/inventory

## Endpoints

- GET /health
- GET /api/inventory
- POST /api/inventory/sync
- POST /api/inventory/sync/batch
- WebSocket /ws/inventory

## Importante

La app Android no va a estar siempre online. Por eso este backend funciona con:
- sincronización en tiempo real cuando hay conectividad
- persistencia central en SQLite
- cola de eventos si el cliente se desconecta

Para el siguiente paso, la app Android debe enviar eventos a este backend cada vez que cambie un producto o movimiento.
