package es.example.mibiblioteca;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import es.example.mibiblioteca.databinding.ActivityMainBinding;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements LibroAdapter.OnLibroListener {

    private ActivityMainBinding binding;
    private LibroDao dao;
    private LibroAdapter adapter;

    // HILO SECUNDARIO: aquí se hace TODO el trabajo con la base de datos
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Filtros (ejercicios 1 y 2). Solo se leen/escriben en el hilo principal
    // y se copian a variables locales antes de pasar al executor.
    private String textoBusqueda = "";
    private boolean soloPendientes = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom);
            return insets;
        });
        setSupportActionBar(binding.toolbar);

        // 1) Obtener la base de datos y su DAO
        dao = AppDatabase.getInstance(this).libroDao();

        // 2) Preparar la lista
        adapter = new LibroAdapter(this);
        binding.rvLibros.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLibros.setAdapter(adapter);

        binding.btnAnadir.setOnClickListener(v -> anadirLibro());

        // 3) Leer lo que ya había guardado
        cargarLibros();
    }

    // =====================================================================
    //  MENÚ: búsqueda (ejercicio 1) y «Ver solo pendientes» (ejercicio 2)
    // =====================================================================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_pendientes).setChecked(soloPendientes);

        SearchView searchView = (SearchView) menu.findItem(R.id.action_buscar).getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.buscar_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    textoBusqueda = newText == null ? "" : newText.trim();
                    cargarLibros();
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_pendientes) {
            soloPendientes = !item.isChecked();
            item.setChecked(soloPendientes);
            cargarLibros();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =====================================================================
    //  LEER: hilo secundario -> resultado al hilo principal
    // =====================================================================
    private void cargarLibros() {
        // Copiamos los filtros en el hilo principal antes de saltar al secundario
        final String patron = "%" + textoBusqueda + "%";
        final boolean pendientes = soloPendientes;
        final boolean hayFiltro = pendientes || !textoBusqueda.isEmpty();

        executor.execute(() -> {
            // Esto se ejecuta en el HILO SECUNDARIO (no puede tocar la interfaz)
            List<Libro> lista = pendientes ? dao.buscarPendientes(patron) : dao.buscar(patron);
            int total = dao.contarTodos();
            int leidos = dao.contarLeidos();

            // Volvemos al HILO PRINCIPAL para pintar
            runOnUiThread(() -> mostrar(lista, total, leidos, hayFiltro));
        });
    }

    private void mostrar(List<Libro> lista, int total, int leidos, boolean hayFiltro) {
        adapter.setLibros(lista);
        binding.tvVacio.setText(hayFiltro && total > 0 ? R.string.sin_resultados : R.string.sin_libros);
        binding.tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(getString(R.string.resumen_libros, total, leidos));
        }
    }

    // =====================================================================
    //  INSERTAR
    // =====================================================================
    private void anadirLibro() {
        String titulo = texto(binding.etTitulo.getText());
        String autor = texto(binding.etAutor.getText());
        String paginasTexto = texto(binding.etPaginas.getText());
        int valoracion = Math.round(binding.rbNuevaValoracion.getRating());

        binding.tilTitulo.setError(null);
        binding.tilAutor.setError(null);
        binding.tilPaginas.setError(null);

        boolean correcto = true;
        if (titulo.isEmpty()) { binding.tilTitulo.setError(getString(R.string.error_obligatorio)); correcto = false; }
        if (autor.isEmpty()) { binding.tilAutor.setError(getString(R.string.error_obligatorio)); correcto = false; }
        int paginas = 0;
        try { paginas = Integer.parseInt(paginasTexto); } catch (NumberFormatException ignored) { }
        if (paginas <= 0) { binding.tilPaginas.setError(getString(R.string.error_paginas)); correcto = false; }
        if (!correcto) return;

        Libro nuevo = new Libro(titulo, autor, paginas, valoracion);

        executor.execute(() -> {
            dao.insertar(nuevo);                        // hilo secundario
            runOnUiThread(() -> {                       // hilo principal
                binding.etTitulo.setText("");
                binding.etAutor.setText("");
                binding.etPaginas.setText("");
                binding.rbNuevaValoracion.setRating(0);
                binding.etTitulo.requestFocus();
                Toast.makeText(this, R.string.libro_guardado, Toast.LENGTH_SHORT).show();
                cargarLibros();                         // volver a leer para refrescar la lista
            });
        });
    }

    // =====================================================================
    //  ACTUALIZAR y ELIMINAR (avisos que llegan desde el adaptador)
    // =====================================================================
    @Override
    public void onCambiarLeido(Libro libro, boolean leido) {
        libro.leido = leido;
        executor.execute(() -> {
            dao.actualizar(libro);
            runOnUiThread(this::cargarLibros);
        });
    }

    // Ejercicio 4: pedir confirmación antes de borrar
    @Override
    public void onEliminar(Libro libro) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirmar_titulo)
                .setMessage(getString(R.string.confirmar_mensaje, libro.titulo))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> eliminarLibro(libro))
                .show();
    }

    private void eliminarLibro(Libro libro) {
        executor.execute(() -> {
            dao.eliminar(libro);
            runOnUiThread(() -> {
                cargarLibros();
                Snackbar.make(binding.getRoot(), R.string.libro_eliminado, Snackbar.LENGTH_LONG)
                        .setAction(R.string.deshacer, v -> executor.execute(() -> {
                            dao.insertar(libro);        // se vuelve a insertar con el mismo id
                            runOnUiThread(this::cargarLibros);
                        }))
                        .show();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();    // liberar el hilo al cerrar la pantalla
    }

    private String texto(CharSequence cs) {
        return cs == null ? "" : cs.toString().trim();
    }
}
