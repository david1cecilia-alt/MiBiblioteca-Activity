package es.example.mibiblioteca;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * BASE DE DATOS: une las entidades y da acceso a los DAO.
 * Patrón singleton: una sola instancia para toda la app.
 */
@Database(entities = {Libro.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract LibroDao libroDao();

    private static volatile AppDatabase instancia;

    // Ejercicio 3: de la versión 1 a la 2 se añade la columna "valoracion"
    static final Migration MIGRACION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE libros ADD COLUMN valoracion INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "biblioteca.db")          // nombre del fichero SQLite
                            .addMigrations(MIGRACION_1_2)
                            .build();
                }
            }
        }
        return instancia;
    }
}
