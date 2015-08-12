package org.spekl.spm.utils;

import javax.swing.*;

public class GetPasswordDialog extends javax.swing.JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPasswordField passwordField1;
    private JLabel label;

    public GetPasswordDialog(String who) {
        setContentPane(contentPane);
        setModal(true);
        setTitle("Enter Spekl Password");
        label.setText("Enter Password For:" + who);

        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onCancel();
            }
        }, javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static String getPasswordFor(String who){
        GetPasswordDialog dialog = new GetPasswordDialog(who);
        dialog.pack();
        dialog.setModal(true);

        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return dialog.passwordField1.getText();
    }

    public static void main(String[] args) {
        GetPasswordDialog dialog = new GetPasswordDialog("jsinglet@gmail.com");
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        System.exit(0);
    }
}
