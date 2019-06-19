package bt.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import bt.compiler.Printer;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.TransactionBroadcast;
import burst.kit.entity.response.http.BRSError;

/**
 * Dialog to compile AT bytecode as well as to publish the contract on chain.
 * 
 * @author jjos
 */
class CompileDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 7650231418854302279L;

    HintTextField nameField, descField;
    JComboBox<String> nodeField;
    JPasswordField passField;
    HintTextField feeField, actFeeField;
    HintTextField deadlineField;
    HintTextField pagesField, csField, usField;

    private Compiler comp;

    private JTextArea codeArea;

    private JTextArea codeAreaForm;

    private Class<? extends Contract> atClass;

    private JButton closeButton, publishButton;

    CompileDialog(Window parent, Class<? extends Contract> atClass) {
        super(parent, "Compile/Publish Contract", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());

        this.atClass = atClass;

        JPanel center = new JPanel(new BorderLayout());
        JPanel left = new JPanel(new BorderLayout());
        center.add(left, BorderLayout.LINE_START);

        JPanel codePanel = new JPanel(new BorderLayout());
        codePanel.setBorder(new TitledBorder("AT BYTECODE"));
        codeArea = new JTextArea(10, 20);
        codeArea.setEditable(false);
        codeArea.setLineWrap(true);
        JScrollPane codeScroll = new JScrollPane(codeArea);
        codePanel.add(codeScroll, BorderLayout.CENTER);

        left.add(codePanel, BorderLayout.PAGE_START);

        JPanel config = new JPanel(new GridLayout(0, 1));
        config.setBorder(new TitledBorder("PUBLISH"));
        config.add(nameField = new HintTextField("Contract name", null));
        nameField.setText(atClass.getSimpleName());
        config.add(descField = new HintTextField("Contract description", null));
        descField.setText(atClass.getSimpleName() + ", created with BlockTalk");
        config.add(nodeField = new JComboBox<>());
        nodeField.setToolTipText("BURST node address");
        nodeField.setEditable(true);
        nodeField.addItem(BT.NODE_AT_TESTNET);
        nodeField.addItem(BT.NODE_TESTNET);
        nodeField.addItem(BT.NODE_LOCAL_TESTNET);
        nodeField.addItem(BT.NODE_BURSTCOIN_RO);
        nodeField.addItem(BT.NODE_BURST_ALLIANCE);
        nodeField.addItem(BT.NODE_BURST_TEAM);
        nodeField.addItem(BT.NODE_BURSTCOIN_RO2);

        config.add(passField = new JPasswordField());
        passField.setToolTipText("Passphrase, never sent over the wire");
        config.add(deadlineField = new HintTextField("Deadline in minutes", null));
        deadlineField.setText("1440"); // 4 days
        config.add(feeField = new HintTextField("Fee in BURST", null));
        feeField.setText("7.0");
        config.add(actFeeField = new HintTextField("Activation fee in BURST", null));
        actFeeField.setText("10.0");

        left.add(config, BorderLayout.CENTER);

        config.add(publishButton = new JButton("Publish"));
        publishButton.addActionListener(this);

        JPanel codePanelForm = new JPanel(new BorderLayout());
        codePanelForm.setBorder(new TitledBorder("AT FORMATTED BYTECODE"));
        codeAreaForm = new JTextArea(10, 30);
        codeAreaForm.setEditable(false);
        JScrollPane codeScrollForm = new JScrollPane(codeAreaForm);
        codePanelForm.add(codeScrollForm, BorderLayout.CENTER);
        center.add(codePanelForm, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        add(buttons, BorderLayout.SOUTH);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        buttons.add(closeButton);

        add(center, BorderLayout.CENTER);
        pack();
    }

    public void execute() {
        try {
            comp = new Compiler(atClass);

            comp.compile();
            if (comp.getErrors().size() == 0) {
                comp.link();
            }

            if (comp.getErrors().size() > 0) {
                JOptionPane.showMessageDialog(getParent(),
                        "<html>AT compile problem:<br><b>" + comp.getErrors().get(0).getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream out = new PrintStream(baos, true, "UTF-8");
            Printer.printCode(comp.getCode(), out);
            String code = new String(baos.toByteArray(), StandardCharsets.UTF_8);

            baos.reset();
            Printer.print(comp.getCode(), out, comp);
            String codeForm = new String(baos.toByteArray(), StandardCharsets.UTF_8);

            BurstValue fee = BT.getMinRegisteringFee(comp);
            feeField.setText(fee.toFormattedString());

            codeArea.setText(code);
            codeAreaForm.setText(codeForm);
            codeArea.setCaretPosition(0);
            codeAreaForm.setCaretPosition(0);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(getParent(), "AT compile problem: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void publishAT() {
        try {
            String passphrase = String.valueOf(passField.getPassword());
            String name = nameField.getText();
            String description = descField.getText();

            BurstValue fee = BurstValue.fromBurst(feeField.getText());
            BurstValue actFee = BurstValue.fromBurst(actFeeField.getText());
            int deadline = Integer.parseInt(deadlineField.getText());

            BT.registerContract(passphrase, comp, name, description, actFee, fee, deadline)
                    .subscribe(this::onTransactionSent, this::handleError);
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void onTransactionSent(TransactionBroadcast transactionBroadcast) {
        setCursor(Cursor.getDefaultCursor());
        closeButton.setEnabled(true);
        publishButton.setEnabled(true);

        JOptionPane.showMessageDialog(getParent(),
                "Transaction ID: " + transactionBroadcast.getTransactionId(), "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleError(Throwable t) {
        setCursor(Cursor.getDefaultCursor());
        closeButton.setEnabled(true);
        publishButton.setEnabled(true);

        String msg = t.getMessage();
        if (t instanceof BRSError) {
            msg = ((BRSError) t).getDescription();
        }
        JOptionPane.showMessageDialog(getParent(), msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == closeButton) {
            setVisible(false);
        } else if (e.getSource() == publishButton) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            closeButton.setEnabled(false);
            publishButton.setEnabled(false);
            publishAT();
        }
    }
}