package com.analizador.gui;

import com.analizador.modelo.ErrorLexico;
import com.analizador.modelo.ErrorSintactico;
import com.analizador.modelo.TablaSimbolos;
import com.analizador.modelo.Token;
import com.analizador.reporte.GeneradorReporte;
import com.analizador.servicio.CompiladorJS;
import com.analizador.sintactico.NodoAST;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultHighlighter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Interfaz Gráfica de Usuario Integrada (Módulo de Alejandro - Arquitectura y GUI).
 * Conecta el pipeline de compilación (JFlex + JCUP), visualización de AST,
 * números de línea, barra de estado, resaltado de errores y exportación Mermaid.
 */
public class VentanaPrincipal extends JFrame {

    private static final String[] DIFICULTADES = {
        "Dificultad Baja",
        "Dificultad Media",
        "Dificultad Alta",
        "Dificultad Muy Compleja",
        "Prueba Sintáctica Completa (Grupo)",
        "Prueba IF / ELSE (Pablo)",
        "Prueba Errores Léxicos"
    };

    private static final String[] ARCHIVOS_PRUEBA = {
        "algoritmo_bajo.js",
        "algoritmo_medio.js",
        "algoritmo_alto.js",
        "algoritmo_muy_complejo.js",
        "algoritmo_sintactico_completo.js",
        "algoritmo_if_else.js",
        "algoritmo_errores.js"
    };

    private String nombreArchivoActual = "algoritmo_bajo.js";
    private List<Token> ultimosTokens = new ArrayList<>();
    private TablaSimbolos ultimaTabla = new TablaSimbolos();
    private List<ErrorLexico> ultimosErrores = new ArrayList<>();
    private List<ErrorSintactico> ultimosErroresSintacticos = new ArrayList<>();
    private List<String> ultimasAdvertencias = new ArrayList<>();
    private NodoAST ultimoAst = null;
    private long ultimoTiempoMs = 0;

    private final JTextArea fuente = new JTextArea();
    private final JEditorPane vistaReporte = new JEditorPane("text/html", "");
    private final JTree arbolSintactico = new JTree(new DefaultMutableTreeNode("AST (sin analizar)"));
    private final JLabel barraEstado = new JLabel("Listo | Línea: 1, Columna: 1");

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

    private final DefaultTableModel advertencias = new DefaultTableModel(
            new Object[]{"#", "Aviso / Advertencia de Análisis Estático"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JComboBox<String> dificultad = new JComboBox<>(DIFICULTADES);
    private final NumeroLinea numeroLinea;

    public VentanaPrincipal() {
        super("Analizador Léxico y Sintáctico JS - Java 21, JFlex & JCUP");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        fuente.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        fuente.setTabSize(4);
        fuente.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        numeroLinea = new NumeroLinea(fuente);

        // Actualizar coordenadas en la barra de estado según el cursor
        fuente.addCaretListener(e -> actualizarBarraEstado());

        vistaReporte.setEditable(false);

        JButton cargar = new JButton("Cargar .js");
        cargar.addActionListener(e -> cargarArchivo());

        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> {
            fuente.setText("");
            analizar();
        });

        JButton ejecutar = new JButton("▶ Ejecutar Análisis Completo");
        ejecutar.setFont(ejecutar.getFont().deriveFont(Font.BOLD));
        ejecutar.addActionListener(e -> analizar());

        JButton exportar = new JButton("Exportar Reporte");
        exportar.addActionListener(e -> exportarReporte());

        // Conectar el selector rápido de dificultad para cargar automáticamente el algoritmo
        dificultad.addActionListener(e -> cargarDificultadSeleccionada());

        JToolBar barra = new JToolBar();
        barra.setFloatable(false);
        barra.add(cargar);
        barra.add(limpiar);
        barra.addSeparator();
        barra.add(dificultad);
        barra.addSeparator();
        barra.add(ejecutar);
        barra.addSeparator();
        barra.add(exportar);

        // Panel del AST con botones para expandir y colapsar
        JPanel panelAst = new JPanel(new BorderLayout());
        JToolBar barraAst = new JToolBar();
        barraAst.setFloatable(false);
        JButton btnExpandir = new JButton("Expandir Todo");
        btnExpandir.addActionListener(e -> expandirArbol(true));
        JButton btnColapsar = new JButton("Colapsar Todo");
        btnColapsar.addActionListener(e -> expandirArbol(false));
        barraAst.add(btnExpandir);
        barraAst.add(btnColapsar);
        panelAst.add(barraAst, BorderLayout.NORTH);
        panelAst.add(new JScrollPane(arbolSintactico), BorderLayout.CENTER);

        JTabbedPane resultados = new JTabbedPane();
        resultados.addTab("Flujo de Tokens", new JScrollPane(new JTable(tokens)));
        resultados.addTab("Tabla de Símbolos", new JScrollPane(new JTable(simbolos)));
        resultados.addTab("Informe Ejecutivo", new JScrollPane(vistaReporte));
        resultados.addTab("Árbol Sintáctico (AST)", panelAst);

        // Editor con números de línea en el gutter izquierdo
        JScrollPane scrollEditor = new JScrollPane(fuente);
        scrollEditor.setRowHeaderView(numeroLinea);

        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                scrollEditor, resultados);
        centro.setResizeWeight(0.42);

        // Tablas de Errores con navegación automática y resaltado al hacer clic
        JTable tablaErroresLexicos = new JTable(errores);
        tablaErroresLexicos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaErroresLexicos.getSelectedRow();
                if (fila >= 0) {
                    try {
                        int linea = Integer.parseInt(tablaErroresLexicos.getValueAt(fila, 1).toString());
                        int col = Integer.parseInt(tablaErroresLexicos.getValueAt(fila, 2).toString());
                        irALinea(linea, col);
                    } catch (Exception ignored) {}
                }
            }
        });

        JTable tablaErroresSintacticos = new JTable(erroresSintacticos);
        tablaErroresSintacticos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaErroresSintacticos.getSelectedRow();
                if (fila >= 0) {
                    try {
                        int linea = Integer.parseInt(tablaErroresSintacticos.getValueAt(fila, 1).toString());
                        int col = Integer.parseInt(tablaErroresSintacticos.getValueAt(fila, 2).toString());
                        irALinea(linea, col);
                    } catch (Exception ignored) {}
                }
            }
        });

        JTabbedPane consolaErrores = new JTabbedPane();
        consolaErrores.addTab("Errores Léxicos", new JScrollPane(tablaErroresLexicos));
        consolaErrores.addTab("Errores Sintácticos (JCUP)", new JScrollPane(tablaErroresSintacticos));
        consolaErrores.addTab("Advertencias Semánticas", new JScrollPane(new JTable(advertencias)));
        consolaErrores.setPreferredSize(new java.awt.Dimension(100, 185));
        consolaErrores.setBorder(BorderFactory.createTitledBorder("Consola de Diagnóstico (Doble clic en error para ubicar en el código)"));

        JPanel panelInferior = new JPanel(new BorderLayout());
        barraEstado.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        barraEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        barraEstado.setOpaque(true);
        barraEstado.setBackground(new Color(248, 250, 252));
        panelInferior.add(consolaErrores, BorderLayout.CENTER);
        panelInferior.add(barraEstado, BorderLayout.SOUTH);

        add(barra, BorderLayout.NORTH);
        add(centro, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        // Cargar por defecto la primera prueba seleccionada
        cargarDificultadSeleccionada();
    }

    private void actualizarBarraEstado() {
        try {
            int caretPos = fuente.getCaretPosition();
            int totalLineas = Math.max(1, fuente.getLineCount());
            int line = fuente.getLineOfOffset(caretPos) + 1;
            int col = caretPos - fuente.getLineStartOffset(line - 1) + 1;
            String estadoComp = (ultimosErrores.isEmpty() && ultimosErroresSintacticos.isEmpty())
                    ? "✓ Compilación OK" : "⚠ Con Errores";
            barraEstado.setText("Línea: " + line + " de " + totalLineas +
                    "  |  Columna: " + col +
                    "  |  Total Líneas de Código: " + totalLineas +
                    "  |  Caracteres: " + fuente.getText().length() +
                    "  |  Tokens: " + ultimosTokens.size() +
                    "  |  Tiempo: " + ultimoTiempoMs + " ms" +
                    "  |  Estado: " + estadoComp);
        } catch (Exception ignored) {}
    }

    private void irALinea(int linea, int columna) {
        try {
            int totalLineas = fuente.getLineCount();
            int targetLinea = Math.max(1, Math.min(linea, totalLineas)) - 1;
            int startOffset = fuente.getLineStartOffset(targetLinea);
            int endOffset = fuente.getLineEndOffset(targetLinea);

            int targetCaret = Math.min(startOffset + Math.max(0, columna - 1), endOffset);
            fuente.setCaretPosition(targetCaret);
            fuente.setSelectionStart(startOffset);
            fuente.setSelectionEnd(endOffset);
            fuente.requestFocusInWindow();

            // Resaltar visualmente la línea con un suave color rojo/rosa
            fuente.getHighlighter().removeAllHighlights();
            fuente.getHighlighter().addHighlight(startOffset, endOffset,
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(254, 202, 202)));
        } catch (Exception ignored) {}
    }

    private void expandirArbol(boolean expandir) {
        for (int i = 0; i < arbolSintactico.getRowCount(); i++) {
            if (expandir) {
                arbolSintactico.expandRow(i);
            } else {
                arbolSintactico.collapseRow(i);
            }
        }
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
        advertencias.setRowCount(0);
        fuente.getHighlighter().removeAllHighlights();

        try {
            // Ejecución desacoplada mediante el coordinador del pipeline CompiladorJS
            CompiladorJS.ResultadoCompilacion resultado = CompiladorJS.compilar(fuente.getText());

            ultimosTokens = resultado.tokens();
            ultimaTabla = resultado.tablaSimbolos();
            ultimosErrores = resultado.erroresLexicos();
            ultimosErroresSintacticos = resultado.erroresSintacticos();
            ultimasAdvertencias = resultado.advertencias();
            ultimoAst = resultado.astRaiz();
            ultimoTiempoMs = resultado.tiempoCompilacionMs();

            // 1. Poblar tabla de tokens
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

            // 2. Poblar tabla de símbolos
            ultimaTabla.obtenerTodos().forEach(s -> simbolos.addRow(new Object[]{
                s.nombre(),
                s.categoria(),
                s.primeraLinea(),
                s.primeraColumna(),
                s.ocurrencias()
            }));

            // 3. Poblar errores léxicos
            for (ErrorLexico e : ultimosErrores) {
                errores.addRow(new Object[]{
                    e.lexemaInvalido(),
                    e.linea(),
                    e.columna(),
                    e.descripcion()
                });
            }

            // 4. Poblar errores sintácticos
            for (ErrorSintactico es : ultimosErroresSintacticos) {
                erroresSintacticos.addRow(new Object[]{
                    es.tokenEncontrado(),
                    es.linea(),
                    es.columna(),
                    es.descripcion()
                });
            }

            // 5. Poblar advertencias
            int advIdx = 1;
            for (String adv : ultimasAdvertencias) {
                advertencias.addRow(new Object[]{
                    advIdx++,
                    adv
                });
            }

            // 6. Actualizar Árbol Sintáctico (AST)
            if (ultimoAst != null) {
                arbolSintactico.setModel(new DefaultTreeModel(ultimoAst.toTreeNode()));
                expandirArbol(true);
            } else {
                arbolSintactico.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("Árbol vacío o con errores sintácticos")));
            }

            // 7. Actualizar vista del informe ejecutivo en HTML
            vistaReporte.setText(GeneradorReporte.generarHTMLParaSwing(
                    nombreArchivoActual, fuente.getText(), ultimosTokens, ultimaTabla,
                    ultimosErrores, ultimosErroresSintacticos, ultimoAst));
            vistaReporte.setCaretPosition(0);

            actualizarBarraEstado();
            if (numeroLinea != null) {
                numeroLinea.revalidate();
                numeroLinea.repaint();
            }

        } catch (Exception ex) {
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

        String[] opciones = {
            "Reporte HTML (Con Diagrama Mermaid y Estilos)",
            "Reporte Markdown (.md)",
            "Diagrama de Árbol Mermaid (.mmd)",
            "Cancelar"
        };
        int seleccion = JOptionPane.showOptionDialog(this,
                "Seleccione el formato en el que desea exportar:",
                "Exportar Resultados del Compilador",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (seleccion == 0) {
            exportarArchivo(true);
        } else if (seleccion == 1) {
            exportarArchivo(false);
        } else if (seleccion == 2) {
            exportarMermaid();
        }
    }

    private void exportarMermaid() {
        if (ultimoAst == null) {
            JOptionPane.showMessageDialog(this,
                    "No se dispone de un Árbol Sintáctico (AST) para exportar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sugerido = "ast_" + nombreArchivoActual.replaceAll("\\.[^.]+$", "") + ".mmd";
        JFileChooser chooser = new JFileChooser(".");
        chooser.setSelectedFile(new File(sugerido));
        chooser.setFileFilter(new FileNameExtensionFilter("Diagrama Mermaid (*.mmd)", "mmd"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File destino = chooser.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".mmd")) {
                destino = new File(destino.getParentFile(), destino.getName() + ".mmd");
            }

            try {
                GeneradorReporte.guardarEnArchivo(destino, ultimoAst.toMermaid());
                JOptionPane.showMessageDialog(this,
                        "Diagrama Mermaid guardado en:\n" + destino.getAbsolutePath(),
                        "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                mostrarError(ex);
            }
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
                        ? GeneradorReporte.generarHTML(nombreArchivoActual, fuente.getText(), ultimosTokens,
                                ultimaTabla, ultimosErrores, ultimosErroresSintacticos, ultimoAst)
                        : GeneradorReporte.generarMarkdown(nombreArchivoActual, fuente.getText(), ultimosTokens,
                                ultimaTabla, ultimosErrores, ultimosErroresSintacticos, ultimoAst);

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
