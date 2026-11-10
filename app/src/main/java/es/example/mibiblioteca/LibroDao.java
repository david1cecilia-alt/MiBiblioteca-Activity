package es.example.mibiblioteca;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO (Data Access Object): las operaciones sobre la tabla.
 * Solo escribimos el QUÉ; Room genera el código que lo hace.
 */
@Dao
public interface LibroDao {

    @Query("SELECT * FROM libros ORDER BY titulo")
    List<Libro> obtenerTodos();

    @Query("SELECT COUNT(*) FROM libros")
    int contarTodos();

    @Query("SELECT COUNT(*) FROM libros WHERE leido = 1")
    int contarLeidos();

    // Ejercicio 1: buscar por título (se le pasa "%" + texto + "%")
    @Query("SELECT * FROM libros WHERE titulo LIKE :texto ORDER BY titulo")
    List<Libro> buscar(String texto);

    // Ejercicio 2: solo pendientes (también filtrando por título)
    @Query("SELECT * FROM libros WHERE leido = 0 AND titulo LIKE :texto ORDER BY titulo")
    List<Libro> buscarPendientes(String texto);

    @Insert
    long insertar(Libro libro);         // devuelve el id generado

    @Update
    void actualizar(Libro libro);

    @Delete
    void eliminar(Libro libro);
}
