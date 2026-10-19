package agrocripto;

import javax.swing.*;
import java.awt.*;

/** GUI en Swing del Sistema Criptografico Agrocomercial (Cifrado Hill). */
public class VentanaPrincipal extends JFrame {
    private final ListaDoble<Auditor> auditores = new ListaDoble<>(); // LISTA DOBLE
    private final JLabel lblAuditor = new JLabel();
    private final JTextField txtClave = new JTextField(22);
    private final JTextArea txtPlano = crearArea(9);
    private final JTextArea txtCifrado = crearArea(9);
    private final JTextArea txtLog = crearArea(13);

    public VentanaPrincipal() {
        super("AgroCripto - Cifrado Hill para contratos agrocomerciales");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        // --- Panel superior: auditores y clave
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        JButton btnAnt = new JButton("< Anterior");
        JButton btnSig = new JButton("Siguiente >");
        JButton btnGuardar = new JButton("Guardar como auditor");
        norte.add(lblAuditor);
        norte.add(btnAnt);
        norte.add(btnSig);
        norte.add(new JLabel("  Matriz clave (filas con ;):"));
        norte.add(txtClave);
        norte.add(btnGuardar);
        add(norte, BorderLayout.NORTH);

        // --- Centro: texto plano | botones | texto cifrado
        JPanel centro = new JPanel(new GridLayout(1, 2, 8, 0));
        centro.add(conTitulo(txtPlano, "Contrato / montos (texto plano)"));
        centro.add(conTitulo(txtCifrado, "Texto cifrado"));
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton btnCifrar = new JButton("Cifrar  >>");
        JButton btnDescifrar = new JButton("<<  Descifrar");
        JButton btnLimpiar = new JButton("Limpiar");
        botones.add(btnCifrar);
        botones.add(btnDescifrar);
        botones.add(btnLimpiar);
        JPanel medio = new JPanel(new BorderLayout());
        medio.add(centro, BorderLayout.CENTER);
        medio.add(botones, BorderLayout.SOUTH);
        add(medio, BorderLayout.CENTER);

        // --- Sur: bitacora de las estructuras
        add(conTitulo(txtLog, "Estado de las estructuras (matriz, cola, lista simple, alfabeto circular, pila)"),
                BorderLayout.SOUTH);

        cargarAuditores();
        refrescarAuditor();

        btnAnt.addActionListener(e -> { auditores.anterior(); refrescarAuditor(); });
        btnSig.addActionListener(e -> { auditores.siguiente(); refrescarAuditor(); });
        btnGuardar.addActionListener(e -> guardarAuditor());
        btnCifrar.addActionListener(e -> ejecutar(true));
        btnDescifrar.addActionListener(e -> ejecutar(false));
        btnLimpiar.addActionListener(e -> {
            txtPlano.setText(""); txtCifrado.setText(""); txtLog.setText("");
        });

        txtPlano.setText("CONTRATO DE COMPRAVENTA LOTE 12 VEREDA EL ROSAL MONTO 85000000 PESOS");
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    private static JTextArea crearArea(int filas) {
        JTextArea a = new JTextArea(filas, 40);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        a.setLineWrap(true);
        return a;
    }

    private static JComponent conTitulo(JTextArea a, String titulo) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }

    private void cargarAuditores() {
        auditores.agregar(new Auditor("Auditor A", new int[][]{{3, 3}, {2, 5}}));
        auditores.agregar(new Auditor("Auditor B", new int[][]{{1, 2}, {3, 5}}));
        auditores.agregar(new Auditor("Auditor C", new int[][]{{2, 4, 5}, {9, 2, 1}, {3, 17, 7}}));
    }

    private void refrescarAuditor() {
        Auditor a = auditores.actual();
        lblAuditor.setText("Auditor " + auditores.posicionActual() + "/" + auditores.tamano() + ": " + a.nombre);
        txtClave.setText(MatrizMod.aTexto(a.clave));
    }

    private void guardarAuditor() {
        try {
            int[][] clave = MatrizMod.parsear(txtClave.getText(), MotorHill.M);
            if (MatrizMod.inversa(clave, MotorHill.M) == null)
                throw new IllegalArgumentException("Esa matriz no es invertible modulo " + MotorHill.M + ".");
            String nombre = JOptionPane.showInputDialog(this, "Nombre del auditor:");
            if (nombre == null || nombre.trim().isEmpty()) return;
            auditores.agregar(new Auditor(nombre.trim(), clave));
            auditores.irAlUltimo();
            refrescarAuditor();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Clave invalida", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ejecutar(boolean cifrar) {
        try {
            int[][] clave = MatrizMod.parsear(txtClave.getText(), MotorHill.M);
            String entrada = cifrar ? txtPlano.getText() : txtCifrado.getText();
            MotorHill.Resultado r = MotorHill.procesar(entrada, clave, cifrar);
            (cifrar ? txtCifrado : txtPlano).setText(r.texto);
            mostrarLog(cifrar, clave, r);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarLog(boolean cifrar, int[][] clave, MotorHill.Resultado r) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(cifrar ? "CIFRADO" : "DESCIFRADO").append(" con ")
          .append(auditores.actual().nombre).append(" (modulo ").append(MotorHill.M).append(") ===\n");
        sb.append("Matriz clave (2D):\n").append(MatrizMod.mostrar(clave));
        sb.append("Matriz inversa mod ").append(MotorHill.M).append(":\n").append(MatrizMod.mostrar(r.inversa));
        sb.append("Fragmentos que pasaron por la COLA: ").append(r.fragmentos).append('\n');
        sb.append("LISTA SIMPLE de bloques procesados: [").append(r.bloques.unir("] -> [")).append("]\n");
        sb.append("LISTA CIRCULAR (alfabeto tras la ultima rotacion, _ = espacio): ").append(r.alfabetoFinal).append('\n');
        sb.append("PILA de retroalimentacion (LIFO: sale primero el ultimo bloque):\n");
        while (!r.bitacora.estaVacia()) sb.append("   ").append(r.bitacora.pop()).append('\n');
        txtLog.setText(sb.toString());
        txtLog.setCaretPosition(0);
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignorada) { }
        SwingUtilities.invokeLater(() -> new VentanaPrincipal().setVisible(true));
    }
}
