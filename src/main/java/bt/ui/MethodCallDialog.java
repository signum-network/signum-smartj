package bt.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import bt.Address;
import bt.Contract;
import bt.Emulator;
import bt.Register;
import bt.compiler.Compiler;

/**
 * A dialog for filling method call arguments.
 * 
 * @author jjos
 */
class MethodCallDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -6579707913480210871L;

    int ret = JOptionPane.CANCEL_OPTION;
    private JComboBox<String> methodComboBox;
    private HintTextField[] args;
    private JLabel[] argTypes;
    private Contract contract;
    private HashMap<String, Method> methodMap = new HashMap<String, Method>();
    private Register msg;

    MethodCallDialog(Frame parent, Contract c) {
        super(parent, "Method call", Dialog.ModalityType.APPLICATION_MODAL);

        this.contract = c;

        getContentPane().setLayout(new BorderLayout());

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ret = buildMessage();
                setVisible(false);
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel grid = new JPanel(new GridLayout(0, 4));
        grid.setBorder(new TitledBorder("Call method for : " + c.getClass().getName()));
        getContentPane().add(grid, BorderLayout.CENTER);

        methodComboBox = new JComboBox<String>();
        methodComboBox.addActionListener(this);
        grid.add(methodComboBox);
        args = new HintTextField[3];
        for (int i = 0; i < args.length; i++) {
            grid.add(args[i] = new HintTextField("", okButton));
            args[i].setEnabled(false);
        }

        argTypes = new JLabel[3];
        grid.add(new JLabel());
        for (int i = 0; i < argTypes.length; i++) {
            grid.add(argTypes[i] = new JLabel());
        }

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttons, BorderLayout.PAGE_END);

        buttons.add(cancelButton);
        buttons.add(okButton);
    }

    int execute() {
        // read in, build classNode
        ClassNode classNode = new ClassNode();
        ClassReader cr;
        try {
            cr = new ClassReader(contract.getClass().getName());
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(getParent(), e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return JOptionPane.CANCEL_OPTION;
        }
        cr.accept(classNode, 0);

        for (Method m : contract.getClass().getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers()) || Modifier.isStatic(m.getModifiers()))
                continue;
            if (m.getName().equals(Compiler.INIT_METHOD) || m.getName().equals(Compiler.MAIN_METHOD)
                    || m.getName().equals(Compiler.TX_RECEIVED_METHOD))
                continue;

            methodComboBox.addItem(m.getName());
            methodMap.put(m.getName(), m);
        }
        if (methodComboBox.getItemCount() > 0)
            methodComboBox.setSelectedIndex(0);

        if (methodComboBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(getParent(), "Contract has no public methods to call", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return ret;
        }

        pack();
        setLocationRelativeTo(this.getParent());
        setVisible(true);

        getMessage();
        return ret;
    }

    int buildMessage() {
        Method m = methodMap.get(methodComboBox.getSelectedItem());

        Object[] argValues = new Object[3];

        Parameter[] pars = m.getParameters();
        for (int i = 0; i < pars.length && i < 3; i++) {
            String type = pars[i].getType().getName();
            if (type.equals(Address.class.getName())) {
                Address addr = Emulator.getInstance().getAddress(args[i].getText());
                argValues[i] = addr;
            } else if (type.equals(Long.class.getName()) || type.equals("long")) {
                argValues[i] = Long.valueOf(args[i].getText());
            } else if (type.equals(Integer.class.getName()) || type.equals("int")) {
                argValues[i] = Integer.valueOf(args[i].getText());
            } else if (type.equals(Boolean.class.getName()) || type.equals("boolean")) {
                argValues[i] = Boolean.valueOf(args[i].getText());
            } else {
                // unsuported value type

                return JOptionPane.CANCEL_OPTION;
            }
        }

		try {
			Compiler comp = new Compiler(contract.getClass());
			comp.compile();
	        msg = Register.newMethodCall(comp, m, argValues);
		} catch (IOException e) {
			e.printStackTrace();
		}

        return JOptionPane.OK_OPTION;
    }

    Register getMessage() {
        return msg;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == methodComboBox) {

            for (int i = 0; i < args.length; i++) {
                argTypes[i].setText("");
                args[i].setEnabled(false);
            }

            Method m = methodMap.get(methodComboBox.getSelectedItem());
            if (m == null)
                return;

            Parameter[] pars = m.getParameters();
            for (int i = 0; i < args.length && pars != null && i < pars.length; i++) {
                // FIXME find a way to show argument names
                argTypes[i].setText(pars[i].getType().getName()); // + " " + pars[i].getName());
                args[i].setEnabled(true);
            }
        }
    }
}