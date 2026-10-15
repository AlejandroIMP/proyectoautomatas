package com.analizador;

import com.analizador.gui.VentanaPrincipal;
import javax.swing.SwingUtilities;

public final class MainApp {
    private MainApp() {}
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
