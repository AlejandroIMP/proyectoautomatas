package com.analizador.gui;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.sintactico.TokenStream;
import java.awt.BorderLayout;
import java.awt.Font;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

public class VentanaPrincipal extends JFrame {

    private static final String[] DIFICULTADES = {
        "Dificultad Baja",
        "Dificultad Media",
        "Dificultad Alta",
        "Dificultad Muy Compleja",
        "Prueba Errores Léxicos"
    };

    private static final String[] ARCHIVOS_PRUEBA = {
        "algoritmo_bajo.js",
        "algoritmo_medio.js",
        "algoritmo_alto.js",
        "algoritmo_muy_complejo.js",
        "algoritmo_errores.js"
    };

    private final JTextArea fuente = new JTextArea();

    private final DefaultTableModel tokens = new DefaultTableModel(
            new Object[]{"#", "Token", "Lexema", "Línea", "Columna", "Valor Literal"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel simbolos = new DefaultTableModel(
            new Object[]{"Identificador", "Categoría", "1ra Línea", "1ra Columna", "Ocurrencias"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel errores = new DefaultTableModel(
            new Object[]{"Lexema Inválido", "Línea", "Columna", "Descripción"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JComboBox<String> dificultad = new JComboBox<>(DIFICULTADES);

    public VentanaPrincipal() {
        super("Analizador Léxico JavaScript - Java 21 & JFlex");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 720);
        setLocationRelativeTo(null);

        fuente.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fuente.setTabSize(4);

        JButton cargar = new JButton("Cargar .js");
        cargar.addActionListener(e -> cargarArchivo());

        JButton ejecutar = new JButton("Ejecutar Análisis Léxico");
        ejecutar.addActionListener(e -> analizar());

        // Conectar el selector rápido de dificultad para cargar automáticamente el algoritmo
        dificultad.addActionListener(e -> cargarDificultadSeleccionada());

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.add(cargar);
        barra.addSeparator();
        barra.add(dificultad);
        barra.addSeparator();
        barra.add(ejecutar);

        JTabbedPane resultados = new JTabbedPane();
        resultados.addTab("Flujo de Tokens", new JScrollPane(new JTable(tokens)));
        resultados.addTab("Tabla de Símbolos", new JScrollPane(new JTable(simbolos)));
        resultados.addTab("Árbol Sintáctico", new JScrollPane(new JTree(new DefaultMutableTreeNode("AST (fase sintáctica)"))));

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(fuente), resultados);
        centro.setResizeWeight(0.45);

        JScrollPane scrollErrores = new JScrollPane(new JTable(errores));
        scrollErrores.setBorder(BorderFactory.createTitledBorder("Consola de Errores Léxicos"));
        scrollErrores.setPreferredSize(new java.awt.Dimension(100, 160));

        add(barra, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(scrollErrores, BorderLayout.SOUTH);

        // Cargar por defecto la primera prueba seleccionada
        cargarDificultadSeleccionada();
    }

    private void cargarDificultadSeleccionada() {
        int idx = dificultad.getSelectedIndex();
        if (idx >= 0 && idx < ARCHIVOS_PRUEBA.length) {
            cargarDesdeRecursoOArchivo(ARCHIVOS_PRUEBA[idx]);
        }
    }

    private void cargarDesdeRecursoOArchivo(String nombreArchivo) {
        try {
            // Intentar primero desde el classpath (/pruebas/...)
            InputStream is = getClass().getResourceAsStream("/pruebas/" + nombreArchivo);
            if (is == null) {
                // Alternativa sin barra inicial
                is = getClass().getClassLoader().getResourceAsStream("pruebas/" + nombreArchivo);
            }

            if (is != null) {
                try (var reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buffer = new char[4096];
                    int leidos;
                    while ((leidos = reader.read(buffer)) != -1) {
                        sb.append(buffer, 0, leidos);
                    }
                    fuente.setText(sb.toString());
                    fuente.setCaretPosition(0);
                    analizar();
                    return;
                }
            }

            // Fallback: leer directamente del sistema de archivos local
            Path path = Path.of("src/main/resources/pruebas/" + nombreArchivo);
            if (!Files.exists(path)) {
                path = Path.of("target/classes/pruebas/" + nombreArchivo);
            }
            if (Files.exists(path)) {
                fuente.setText(Files.readString(path, StandardCharsets.UTF_8));
                fuente.setCaretPosition(0);
                analizar();
            }
        } catch (Exception ex) {
            mostrarError(ex);
        }
    }

    private void cargarArchivo() {
        JFileChooser chooser = new JFileChooser("src/main/resources/pruebas");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                fuente.setText(Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8));
                fuente.setCaretPosition(0);
                analizar();
            } catch (java.io.IOException ex) {
                mostrarError(ex);
            }
        }
    }

    private void analizar() {
        tokens.setRowCount(0);
        simbolos.setRowCount(0);
        errores.setRowCount(0);

        TablaSimbolos tabla = new TablaSimbolos();
        try {
            AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(fuente.getText()), tabla);
            TokenStream flujo = new TokenStream(lexer);

            int i = 0;
            for (Token token : flujo.getTodosLosTokens()) {
                tokens.addRow(new Object[]{
                    ++i,
                    token.tipo(),
                    token.lexema(),
                    token.linea(),
                    token.columna(),
                    token.valorLiteral() != null ? token.valorLiteral() : "-"
                });
            }

            tabla.obtenerTodos().forEach(s -> simbolos.addRow(new Object[]{
                s.nombre(),
                s.categoria(),
                s.primeraLinea(),
                s.primeraColumna(),
                s.ocurrencias()
            }));

            for (ErrorLexico e : lexer.getErrores()) {
                errores.addRow(new Object[]{
                    e.lexemaInvalido(),
                    e.linea(),
                    e.columna(),
                    e.descripcion()
                });
            }
        } catch (java.io.IOException ex) {
            mostrarError(ex);
        }
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
