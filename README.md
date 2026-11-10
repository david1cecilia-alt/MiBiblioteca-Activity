# Mi Biblioteca: Room e hilos

Práctica de la Unidad 2 (bloque 2) del módulo 0489 · Programación Multimedia y Dispositivos Móviles (2.º DAM).

**Integrantes:** Sergio Salido, David Cecilia y Sergio Lastras.

App Android en Java para llevar una biblioteca personal. Los libros se guardan en una base de datos SQLite con **Room**, así que no se pierden al cerrar la app. Todo el acceso a datos se hace en un **hilo secundario** (`ExecutorService`) y la interfaz se actualiza en el hilo principal con `runOnUiThread`.

## Funcionalidad

| Acción | Operación | Room |
|---|---|---|
| Abrir la app | Leer todos los libros y contar los leídos | `@Query` |
| Añadir | Insertar una fila | `@Insert` |
| Marcar «Leído» | Modificar una fila | `@Update` |
| Papelera | Borrar una fila (con «Deshacer») | `@Delete` |

## Ejercicios de ampliación

1. **Buscar:** búsqueda por título desde la barra superior (`WHERE titulo LIKE :texto`).
2. **Solo pendientes:** opción del menú que muestra solo los libros con `leido = 0`.
3. **Nueva columna con migración:** campo `valoracion` (1 a 5 estrellas), `version = 2` y `Migration(1, 2)` con `ALTER TABLE libros ADD COLUMN valoracion INTEGER NOT NULL DEFAULT 0`.
4. **Confirmar antes de borrar:** diálogo de confirmación antes de eliminar un libro.

## Estructura

```
app/src/main/java/es/example/mibiblioteca/
├── Libro.java          Entidad (@Entity, tabla "libros")
├── LibroDao.java       DAO (@Query, @Insert, @Update, @Delete)
├── AppDatabase.java    Base de datos (@Database, singleton y migración 1→2)
├── LibroAdapter.java   Adaptador del RecyclerView
└── MainActivity.java   Pantalla principal, hilos y operaciones CRUD
```

## Documentación

Las capturas de las pruebas (lista de libros, confirmación de borrado, persistencia y Database Inspector) y las respuestas a las preguntas de comprobación están en [`docs/MiBiblioteca_Room_Hilos_Capturas_y_Respuestas.pdf`](docs/MiBiblioteca_Room_Hilos_Capturas_y_Respuestas.pdf).

## Cómo ejecutarlo

1. Abre la carpeta en Android Studio y sincroniza Gradle.
2. Ejecuta la configuración `app` en un emulador o dispositivo con API 24 o superior.

Tecnologías: Java, layouts XML, View Binding, RecyclerView, Material 3 y Room 2.7.2.
