# Despliegue local

## Topología del Sprint 0

El entorno local está compuesto por:

- Siete procesos Java independientes, iniciados desde Maven.
- Una instancia MySQL 8.4 administrada por Docker Compose y publicada localmente en el puerto `3307`.
- Siete esquemas y siete usuarios MySQL, uno por servicio.

Compartir una instancia física en desarrollo reduce el consumo de recursos. La propiedad lógica continúa separada porque cada usuario solo recibe permisos sobre su propio esquema. El contenedor conserva el puerto interno `3306`; se usa `3307` en el host para evitar conflictos con instalaciones locales de MySQL.

## Preparación

```bash
cp .env.example .env
docker compose up -d
docker compose ps
```

El script `infra/mysql/init/01-create-databases.sh` se ejecuta solamente al inicializar un volumen vacío. Si cambia la definición de usuarios o esquemas durante el desarrollo, debe aplicarse una migración explícita o recrearse voluntariamente el volumen local.

## Compilación

```bash
./mvnw clean verify
```

`verify` ejecuta dos capas de pruebas. Surefire corre las de arranque de contexto, que excluyen la
autoconfiguración de base de datos y no necesitan Docker. Failsafe corre las `*IT`, que levantan un MySQL
real con Testcontainers, aplican las migraciones de Flyway y validan el mapeo JPA ([ADR 0006](../adr/0006-verificacion-de-persistencia-con-testcontainers.md)).

Sin Docker disponible, `./mvnw test` verifica sólo la primera capa.

El esquema lo crea Flyway desde `src/main/resources/db/migration` de cada módulo; Hibernate está en
`validate` y nunca genera tablas ([ADR 0005](../adr/0005-versionado-de-esquema-con-flyway.md)).

## Ejecución de un servicio

```bash
./mvnw -pl msvc-comercial spring-boot:run
```

Cada aplicación toma la URL, el usuario y la contraseña desde variables específicas de su contexto. Los valores predeterminados coinciden con el Compose local y no deben utilizarse en otros ambientes.

## Prueba de arranque completa

Después de compilar y con MySQL saludable, el siguiente script inicia temporalmente los siete JAR, consulta sus endpoints de salud y los detiene al terminar:

```bash
./scripts/smoke-test.sh
```

La prueba utiliza los puertos `18010` a `18070` para evitar colisiones con aplicaciones que estén usando los puertos normales. El desplazamiento predeterminado de `10000` se puede cambiar, por ejemplo con `SMOKE_PORT_OFFSET=20000 ./scripts/smoke-test.sh`. Los registros se escriben en `target/smoke-logs/`, un directorio ignorado por Git.

## Salud

```text
http://localhost:8010/actuator/health
http://localhost:8020/actuator/health
http://localhost:8030/actuator/health
http://localhost:8040/actuator/health
http://localhost:8050/actuator/health
http://localhost:8060/actuator/health
http://localhost:8070/actuator/health
```

Compose levanta únicamente MySQL. Las aplicaciones todavía no se empaquetan como contenedores.
