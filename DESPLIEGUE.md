# Despliegue

Tres piezas: la base de datos en **MongoDB Atlas**, el backend en **Railway** y el
frontend en **Vercel**. Hazlo en ese orden: cada paso necesita un dato del anterior.

---

## 1. Base de datos — MongoDB Atlas

1. Crea una cuenta en <https://cloud.mongodb.com> y un clúster **M0** (gratuito).
2. En **Database Access**, crea un usuario con contraseña.
3. En **Network Access**, añade `0.0.0.0/0`. Railway no publica un rango fijo de
   direcciones, así que restringirlo por IP dejaría el backend fuera.
4. Copia la cadena de conexión y añádele el nombre de la base:

   ```
   mongodb+srv://USUARIO:CONTRASEÑA@cluster0.xxxxx.mongodb.net/DB_SanFelipe?retryWrites=true&w=majority
   ```

   Si la contraseña lleva `@`, `:` o `/`, codifícala (`@` → `%40`).

---

## 2. Backend — Railway

Railway detecta Maven y compila solo; no hace falta Dockerfile.

1. **New Project → Deploy from GitHub repo** → `Back_Wash_App`.
2. En **Variables**, define:

   | Variable | Valor | Obligatoria |
   |---|---|---|
   | `SPRING_DATA_MONGODB_URI` | la cadena del paso 1 | sí |
   | `ADMIN_PASSWORD` | contraseña de la primera cuenta | sí |
   | `LLM_API_KEY` | tu clave de Cerebras (`csk-...`) | para el chat |
   | `ADMIN_USUARIO` | `admin` | no |
   | `LLM_MODEL` | `qwen-3.8-27b` | no |
   | `LLM_API_URL` | `https://api.cerebras.ai/v1/chat/completions` | no |
   | `CLIMA_LATITUD` / `CLIMA_LONGITUD` | `10.3910` / `-75.4794` | no |
   | `CORS_ORIGENES` | dominios propios, separados por comas | no |

   No definas `PORT`: lo inyecta Railway.

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
