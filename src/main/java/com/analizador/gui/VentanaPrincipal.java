package com.analizador.gui;

import com.analizador.lexico.AnalizadorLexicoJS;
import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.ErrorSintactico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.reporte.GeneradorReporte;
import com.analizador.sintactico.AnalizadorSintacticoJS;
import com.analizador.sintactico.NodoAST;
import com.analizador.sintactico.TokenStream;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class VentanaPrincipal extends JFrame {

    private static final String[] DIFICULTADES = {
        "Dificultad Baja",
        "Dificultad Media",
        "Dificultad Alta",
        "Dificultad Muy Compleja",
        "Prueba Sintáctica Completa (Grupo)",
        "Prueba Errores Léxicos"
    };

    private static final String[] ARCHIVOS_PRUEBA = {
        "algoritmo_bajo.js",
        "algoritmo_medio.js",
        "algoritmo_alto.js",
        "algoritmo_muy_complejo.js",
        "algoritmo_sintactico_completo.js",
        "algoritmo_errores.js"
    };

    private String nombreArchivoActual = "algoritmo_bajo.js";
    private List<Token> ultimosTokens = new ArrayList<>();
    private TablaSimbolos ultimaTabla = new TablaSimbolos();
    private List<ErrorLexico> ultimosErrores = new ArrayList<>();
    private List<ErrorSintactico> ultimosErroresSintacticos = new ArrayList<>();

    private final JTextArea fuente = new JTextArea();
    private final JEditorPane vistaReporte = new JEditorPane("text/html", "");
    private final JTree arbolSintactico = new JTree(new DefaultMutableTreeNode("AST (sin analizar)"));

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

    private final DefaultTableModel erroresSintacticos = new DefaultTableModel(
            new Object[]{"Token No Esperado", "Línea", "Columna", "Descripción"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JComboBox<String> dificultad = new JComboBox<>(DIFICULTADES);

    public VentanaPrincipal() {
        super("Analizador Léxico y Sintáctico JS - Java 21, JFlex & JCUP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 780);
        setLocationRelativeTo(null);

        fuente.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fuente.setTabSize(4);

        vistaReporte.setEditable(false);

        JButton cargar = new JButton("Cargar .js");
        cargar.addActionListener(e -> cargarArchivo());

        JButton ejecutar = new JButton("Ejecutar Análisis Completo");
        ejecutar.addActionListener(e -> analizar());

        JButton exportar = new JButton("Exportar Reporte");
        exportar.addActionListener(e -> exportarReporte());

        // Conectar el selector rápido de dificultad para cargar automáticamente el algoritmo
        dificultad.addActionListener(e -> cargarDificultadSeleccionada());

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.add(cargar);
        barra.addSeparator();
        barra.add(dificultad);
        barra.addSeparator();
        barra.add(ejecutar);
        barra.addSeparator();
        barra.add(exportar);

        JTabbedPane resultados = new JTabbedPane();
        resultados.addTab("Flujo de Tokens", new JScrollPane(new JTable(tokens)));
        resultados.addTab("Tabla de Símbolos", new JScrollPane(new JTable(simbolos)));
        resultados.addTab("Informe Ejecutivo", new JScrollPane(vistaReporte));
        resultados.addTab("Árbol Sintáctico (AST)", new JScrollPane(arbolSintactico));

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(fuente), resultados);
        centro.setResizeWeight(0.42);

        JTabbedPane consolaErrores = new JTabbedPane();
        consolaErrores.addTab("Errores Léxicos", new JScrollPane(new JTable(errores)));
        consolaErrores.addTab("Errores Sintácticos (JCUP)", new JScrollPane(new JTable(erroresSintacticos)));
        consolaErrores.setPreferredSize(new java.awt.Dimension(100, 170));
        consolaErrores.setBorder(BorderFactory.createTitledBorder("Consola de Diagnóstico de Errores"));

        add(barra, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(consolaErrores, BorderLayout.SOUTH);

        // Cargar por defecto la primera prueba seleccionada
        cargarDificultadSeleccionada();
    }

    private void cargarDificultadSeleccionada() {
        int idx = dificultad.getSelectedIndex();
        if (idx >= 0 && idx < ARCHIVOS_PRUEBA.length) {
            nombreArchivoActual = ARCHIVOS_PRUEBA[idx];
            cargarDesdeRecursoOArchivo(ARCHIVOS_PRUEBA[idx]);
        }
    }

    private void cargarDesdeRecursoOArchivo(String nombreArchivo) {
        try {
            // Intentar primero desde el classpath (/pruebas/...)
            InputStream is = getClass().getResourceAsStream("/pruebas/" + nombreArchivo);
            if (is == null) {
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
                File archivoSeleccionado = chooser.getSelectedFile();
                nombreArchivoActual = archivoSeleccionado.getName();
                fuente.setText(Files.readString(archivoSeleccionado.toPath(), StandardCharsets.UTF_8));
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
        erroresSintacticos.setRowCount(0);

        TablaSimbolos tabla = new TablaSimbolos();
        try {
            // 1. Fase de Análisis Léxico
            AnalizadorLexicoJS lexer = new AnalizadorLexicoJS(new StringReader(fuente.getText()), tabla);
            TokenStream flujo = new TokenStream(lexer);

            ultimosTokens = flujo.getTodosLosTokens();
            ultimaTabla = tabla;
            ultimosErrores = lexer.getErrores();

            int i = 0;
            for (Token token : ultimosTokens) {
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

            for (ErrorLexico e : ultimosErrores) {
                errores.addRow(new Object[]{
                    e.lexemaInvalido(),
                    e.linea(),
                    e.columna(),
                    e.descripcion()
                });
            }

            // 2. Fase de Análisis Sintáctico con JCUP
            try {
                AnalizadorLexicoJS lexerCup = new AnalizadorLexicoJS(new StringReader(fuente.getText()), new TablaSimbolos());
                AnalizadorSintacticoJS parser = new AnalizadorSintacticoJS(lexerCup);
                parser.parse();

                NodoAST ast = parser.getAstRaiz();
                if (ast != null) {
                    arbolSintactico.setModel(new DefaultTreeModel(ast.toTreeNode()));
                    for (int r = 0; r < arbolSintactico.getRowCount(); r++) {
                        arbolSintactico.expandRow(r);
                    }
                } else {
                    arbolSintactico.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Árbol vacío o con errores sintácticos")));
                }

                ultimosErroresSintacticos = parser.getErroresSintacticos();
                for (ErrorSintactico es : ultimosErroresSintacticos) {
                    erroresSintacticos.addRow(new Object[]{
                        es.tokenEncontrado(),
                        es.linea(),
                        es.columna(),
                        es.descripcion()
                    });
                }
            } catch (Exception ex) {
                erroresSintacticos.addRow(new Object[]{
                    "Error de Ejecución",
                    1,
                    1,
                    "Excepción al parsear: " + ex.getMessage()
                });
            }

            // Actualizar vista del informe ejecutivo en HTML
            vistaReporte.setText(GeneradorReporte.generarHTMLParaSwing(
                    nombreArchivoActual, fuente.getText(), ultimosTokens, ultimaTabla, ultimosErrores));
            vistaReporte.setCaretPosition(0);

        } catch (java.io.IOException ex) {
            mostrarError(ex);
        }
    }

    private void exportarReporte() {
        if (ultimosTokens.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Primero debe ejecutar el análisis para generar el reporte.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] opciones = {"Reporte HTML (Completo con Estilos)", "Reporte Markdown (.md)", "Cancelar"};
        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el formato en el que desea exportar el reporte contextualizado:",
                "Exportar Reporte de Análisis Léxico y Sintáctico",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (seleccion == 0) {
            exportarArchivo(true);
        } else if (seleccion == 1) {
            exportarArchivo(false);
        }
    }

    private void exportarArchivo(boolean esHtml) {
        String ext = esHtml ? ".html" : ".md";
        String sugerido = "reporte_" + nombreArchivoActual.replaceAll("\\.[^.]+$", "") + ext;

        JFileChooser chooser = new JFileChooser(".");
        chooser.setSelectedFile(new File(sugerido));
        chooser.setFileFilter(new FileNameExtensionFilter(
                esHtml ? "Página Web HTML (*.html)" : "Documento Markdown (*.md)",
                esHtml ? "html" : "md"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destino = chooser.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(ext)) {
                destino = new File(destino.getParentFile(), destino.getName() + ext);
            }

            try {
                String contenido = esHtml
                        ? GeneradorReporte.generarHTML(nombreArchivoActual, fuente.getText(), ultimosTokens, ultimaTabla, ultimosErrores)
                        : GeneradorReporte.generarMarkdown(nombreArchivoActual, fuente.getText(), ultimosTokens, ultimaTabla, ultimosErrores);

                GeneradorReporte.guardarEnArchivo(destino, contenido);

                int respuesta = JOptionPane.showConfirmDialog(this,
                        "Reporte guardado con éxito en:\n" + destino.getAbsolutePath() +
                        "\n\n¿Desea abrir el reporte ahora?",
                        "Exportación Exitosa", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (respuesta == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(destino);
                }
            } catch (Exception ex) {
                mostrarError(ex);
            }
        }
    }

    private void mostrarError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
