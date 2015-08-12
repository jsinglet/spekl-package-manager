package org.spekl.spm.utils;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;

/**
 * Created by jls on 8/12/2015.
 */
public class GetPasswordPrompt  {

    public static class RequestFocusListener implements AncestorListener
    {
        private boolean removeListener;

        /*
         *  Convenience constructor. The listener is only used once and then it is
         *  removed from the component.
         */
        public RequestFocusListener()
        {
            this(true);
        }

        /*
         *  Constructor that controls whether this listen can be used once or
         *  multiple times.
         *
         *  @param removeListener when true this listener is only invoked once
         *                        otherwise it can be invoked multiple times.
         */
        public RequestFocusListener(boolean removeListener)
        {
            this.removeListener = removeListener;
        }

        @Override
        public void ancestorAdded(AncestorEvent e)
        {
            JComponent component = e.getComponent();
            component.requestFocusInWindow();

            if (removeListener)
                component.removeAncestorListener( this );
        }

        @Override
        public void ancestorMoved(AncestorEvent e) {}

        @Override
        public void ancestorRemoved(AncestorEvent e) {}
    }



    public static String getPasswordFor(String who) {
        JPasswordField password = new JPasswordField();
        password.addAncestorListener(new RequestFocusListener());

        JPanel panel = new JPanel(new GridLayout(2,2));

        panel.add(new JLabel("Password For: " + who ));
        panel.add(password);

        JOptionPane.showConfirmDialog(null, panel, "Enter Spekl Password", JOptionPane.OK_CANCEL_OPTION);

        return password.getText();
    }

    public static void main(String[] args) {


    }

}