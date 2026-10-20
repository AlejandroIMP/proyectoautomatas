package com.analizador.gui;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Componente visual que dibuja los números de línea en el gutter izquierdo del editor.
 * Módulo de Alejandro - Experiencia de Usuario y Editor.
 */
public class NumeroLinea extends JComponent implements CaretListener, DocumentListener, PropertyChangeListener {

    private static final Color COLOR_FONDO = new Color(248, 250, 252);
    private static final Color COLOR_TEXTO = new Color(148, 163, 184);
    private static final Color COLOR_LINEA_ACTIVA = new Color(37, 99, 235);
    private static final Color COLOR_BORDE = new Color(226, 232, 240);

    private final JTextArea componenteTexto;

    public NumeroLinea(JTextArea componenteTexto) {
        this.componenteTexto = componenteTexto;
        setFont(componenteTexto.getFont());

        componenteTexto.getDocument().addDocumentListener(this);
        componenteTexto.addCaretListener(this);
        componenteTexto.addPropertyChangeListener("font", this);
        componenteTexto.addPropertyChangeListener("document", this);
    }

    @Override
    public Dimension getPreferredSize() {
        int ancho = calcularAncho();
        int alto = Math.max(componenteTexto.getHeight(), componenteTexto.getPreferredSize().height);
        return new Dimension(ancho, Math.max(alto, 32767));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private int calcularAncho() {
        int lineas = Math.max(1, componenteTexto.getLineCount());
        int digitos = Math.max(String.valueOf(lineas).length(), 3);
        Font font = getFont() != null ? getFont() : componenteTexto.getFont();
        FontMetrics fm = getFontMetrics(font);
        int charWidth = (fm != null && fm.charWidth('0') > 0) ? fm.charWidth('0') : 8;
        return (digitos * charWidth) + 24;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int ancho = getWidth();
        int alto = getHeight();

        // 1. Fondo del gutter
        g2.setColor(COLOR_FONDO);
        g2.fillRect(0, 0, ancho, alto);

        // 2. Línea divisoria derecha
        g2.setColor(COLOR_BORDE);
        g2.drawLine(ancho - 1, 0, ancho - 1, alto);

        // 3. Tipografía y métricas
        Font font = componenteTexto.getFont();
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics(font);
        int fontAscent = fm.getAscent();
        int fontHeight = Math.max(1, fm.getHeight());
        Insets textInsets = componenteTexto.getInsets();
        int topOffset = (textInsets != null) ? textInsets.top : 0;

        Element raiz = componenteTexto.getDocument().getDefaultRootElement();
        int totalLineas = raiz.getElementCount();

        int caretLine = 0;
        try {
            int caretOffset = componenteTexto.getCaretPosition();
            caretLine = raiz.getElementIndex(caretOffset);
        } catch (Exception ignored) {}

        Rectangle clip = g2.getClipBounds();
        int clipY = (clip != null) ? clip.y : 0;
        int clipHeight = (clip != null) ? clip.height : alto;

        for (int i = 0; i < totalLineas; i++) {
            int yPos = topOffset + (i * fontHeight);

            // Coordenada exacta usando Rectangle2D (sin castear a java.awt.Rectangle)
            try {
                Element elemento = raiz.getElement(i);
                Rectangle2D r2d = componenteTexto.modelToView2D(elemento.getStartOffset());
                if (r2d != null) {
                    yPos = (int) r2d.getY();
                }
            } catch (Exception ignored) {}

            // Solo pintar las líneas visibles en el viewport
            if (yPos + fontHeight >= clipY && yPos <= clipY + clipHeight) {
                String numeroStr = String.valueOf(i + 1);
                int numWidth = fm.stringWidth(numeroStr);
                int x = ancho - numWidth - 10;
                int y = yPos + fontAscent;

                if (i == caretLine) {
                    g2.setColor(COLOR_LINEA_ACTIVA);
                    g2.setFont(font.deriveFont(Font.BOLD));
                    g2.drawString(numeroStr, x, y);
                    g2.setFont(font);
                } else {
                    g2.setColor(COLOR_TEXTO);
                    g2.drawString(numeroStr, x, y);
                }
            }
        }
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        repaint();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        revalidate();
        repaint();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        revalidate();
        repaint();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        revalidate();
        repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("font".equals(evt.getPropertyName())) {
            if (evt.getNewValue() instanceof Font f) {
                setFont(f);
            }
            revalidate();
            repaint();
        } else if ("document".equals(evt.getPropertyName())) {
            revalidate();
            repaint();
        }
    }
}
