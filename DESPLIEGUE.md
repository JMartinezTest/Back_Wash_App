# Despliegue

Tres piezas: la base de datos en **MongoDB Atlas**, el backend en **Railway** y el
frontend en **Vercel**. Hazlo en ese orden: cada paso necesita un dato del anterior.

---

## 1. Base de datos — MongoDB en Railway

Se usa el propio MongoDB de Railway, no Atlas. El plan gratuito de Atlas (M0)
comparte un proxy que devuelve nodos con el handshake TLS roto, y el nodo
defectuoso cambia según desde dónde te conectes: la aplicación arranca desde una
máquina y muere desde el hosting. Aquí la conexión es interna, sin TLS ni DNS
SRV, y además evita el viaje hasta AWS Virginia en cada consulta.

1. En tu proyecto de Railway: **New → Database → Add MongoDB**.
2. Railway crea el servicio y sus variables. La que interesa es `MONGO_URL`,
   que apunta a la red interna del proyecto y tiene esta forma:

   ```
   mongodb://mongo:CONTRASEÑA@mongo.railway.internal:27017
   ```

   Viene sin nombre de base y sin parámetros: los añadimos al referenciarla.
   (`MONGO_PUBLIC_URL` solo existe si habilitas el acceso público, y no hace
   falta para que el backend hable con la base.)

No hace falta abrir puertos ni configurar cortafuegos: los dos servicios se ven
entre sí por la red privada del proyecto.

**No añadas `-Djava.net.preferIPv4Stack=true`.** Los dominios `.railway.internal`
resuelven a IPv6 (en entornos anteriores a octubre de 2025, solo a IPv6), y esa
opción deja a la JVM sin poder abrir sockets IPv6: el backend no vería la base de
datos. Java usa IPv6 por defecto cuando está disponible, así que no hay que
configurar nada.

## 2. Backend — Railway

Railway detecta Maven y compila solo; no hace falta Dockerfile.

1. **New Project → Deploy from GitHub repo** → `Back_Wash_App`.
2. En **Variables**, define:

   | Variable | Valor | Obligatoria |
   |---|---|---|
   | `SPRING_DATA_MONGODB_URI` | `${{Mongo.MONGO_URL}}/DB_SanFelipe?authSource=admin` | sí |
   | `ADMIN_PASSWORD` | contraseña de la primera cuenta | sí |
   | `LLM_API_KEY` | tu clave de Cerebras (`csk-...`) | para el chat |
   | `ADMIN_USUARIO` | `admin` | no |
   | `LLM_MODEL` | `qwen-3.8-27b` | no |
   | `LLM_API_URL` | `https://api.cerebras.ai/v1/chat/completions` | no |
   | `CLIMA_LATITUD` / `CLIMA_LONGITUD` | `10.3910` / `-75.4794` | no |
   | `CORS_ORIGENES` | dominios propios, separados por comas | no |

   No definas `PORT`: lo inyecta Railway.

   Sustituye `Mongo` por el nombre real que tenga tu servicio de base de datos.
   Los dos añadidos importan: sin `/DB_SanFelipe` la aplicación escribiría en la
   base por defecto, y **sin `?authSource=admin` el arranque falla con
   `AuthenticationFailed`**, porque el usuario de Railway vive en la base `admin`.

3. **Settings → Networking → Generate Domain**. Guarda la URL.
4. Comprueba que responde:

   ```bash
   curl https://TU-BACKEND.up.railway.app/auth
   ```

   Debe devolver `respuesta cierta`.

### La cuenta inicial

Solo se crea si la base de datos no tiene ningún usuario, y solo si definiste
`ADMIN_PASSWORD`. Si la dejas vacía, el backend arranca pero **nadie podrá entrar**:
lo avisa en los registros. Antes la contraseña estaba escrita en el código, que es
público, así que ahora viene del entorno.

---

## 3. Frontend — Vercel

1. **Add New → Project** → `Front-Wash-App`. Vercel lee `vercel.json`, así que el
   comando de compilación y la carpeta de salida ya vienen configurados.
2. En **Environment Variables**, define:

   | Variable | Valor |
   |---|---|
   | `PUBLIC_API_BASE_URL` | la URL del paso 2, **sin barra final** |

   Es la única imprescindible. Sin ella la aplicación apunta al backend antiguo.
3. Despliega y entra con `admin` y la contraseña que pusiste en `ADMIN_PASSWORD`.

---

## Comprobaciones

```bash
# El backend responde
curl https://TU-BACKEND.up.railway.app/auth

# El inicio de sesión funciona
curl -X POST https://TU-BACKEND.up.railway.app/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"TU_CONTRASEÑA"}'

# El navegador puede llamarlo desde Vercel
curl -i -X OPTIONS https://TU-BACKEND.up.railway.app/clients \
  -H "Origin: https://TU-FRONT.vercel.app" \
  -H "Access-Control-Request-Method: GET" | grep -i access-control-allow-origin
```

`*.vercel.app` ya está permitido, incluidas las vistas previas de cada rama. Para un
dominio propio, añádelo en `CORS_ORIGENES`.

---

## Si algo falla

| Síntoma | Causa habitual |
|---|---|
| Railway no compila | Falta `.mvn/wrapper/maven-wrapper.properties` en el repositorio, o `mvnw` no tiene permiso de ejecución |
| El despliegue arranca pero no responde | La aplicación no leyó `PORT`. Comprueba que no has definido `SERVER_PORT` a mano |
| No se puede iniciar sesión | Falta `ADMIN_PASSWORD`, o la base de datos ya tenía usuarios de antes |
| `AuthenticationFailed` al arrancar | Falta `?authSource=admin` al final de la cadena de conexión |
| `Failed looking up TXT record` | La cadena usa `mongodb+srv://`. Ese formato necesita registros DNS SRV y TXT que el contenedor no resuelve: usa la forma `mongodb://` |
| `Only one host allowed when using mongodb+srv` | La cadena lista varios nodos pero conserva el prefijo `+srv`. Quítalo |
| `SSLException: Received fatal alert: internal_error` | Nodo de Atlas con el handshake roto. No se arregla desde el cliente: ni forzando TLS 1.2 ni IPv4 |
| `IPv6 protocol family unavailable` | Hay un `-Djava.net.preferIPv4Stack=true` puesto. Quítalo: la red privada de Railway es IPv6 |
| El navegador bloquea las llamadas | El dominio no está permitido: añádelo a `CORS_ORIGENES` |
| El chat dice que no está configurado | Falta `LLM_API_KEY` |
| Recargar una página da 404 | Falta `vercel.json` con las reescrituras |
| Error de hooks de React en la compilación | Se instaló con un gestor distinto a pnpm (ver nota abajo) |

### Nota sobre los gestores de paquetes

El frontend tiene tres archivos de bloqueo a la vez: `pnpm-lock.yaml`,
`package-lock.json` y `bun.lock`. El árbol de dependencias real está construido con
**pnpm**, y `vercel.json` lo fuerza. Conviene borrar los otros dos: instalar con el
gestor equivocado provoca dos copias de React y la aplicación no arranca, con un
error que no explica la causa.
