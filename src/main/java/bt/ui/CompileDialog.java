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
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import bt.BT;
import bt.Contract;
import bt.compiler.Compiler;
import bt.compiler.Method;
import bt.compiler.Printer;
import signumj.entity.SignumAddress;
import signumj.entity.SignumValue;
import signumj.entity.response.AT;
import signumj.entity.response.TransactionBroadcast;
import signumj.entity.response.http.BRSError;

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

    private JTextArea methodHashArea;

    private Class<? extends Contract> atClass;

    private JButton closeButton, publishButton, listContractsButton;

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

        config.add(deadlineField = new HintTextField("Deadline in minutes", null));
        deadlineField.setText("1440"); // 4 days
        config.add(feeField = new HintTextField("Deploy fee in BURST", null));
        feeField.setText("7.0");
        config.add(actFeeField = new HintTextField("Gas fee in BURST", null));
        actFeeField.setText(SignumValue.fromNQT(Contract.FEE_QUANT*40).toUnformattedString());
        config.add(passField = new JPasswordField());
        passField.setToolTipText("Passphrase, never sent over the wire");

        config.add(nodeField = new JComboBox<>());
        nodeField.setToolTipText("Node address");
        nodeField.setEditable(true);
        nodeField.addItem(BT.NODE_SIGNUM_EU);
        nodeField.addItem(BT.NODE_SIGNUM_BR);
        nodeField.addItem(BT.NODE_BURSTCOIN_RO);
        nodeField.addItem(BT.NODE_LOCAL);
        nodeField.addItem(BT.NODE_TESTNET_RO);
        nodeField.addItem(BT.NODE_LOCAL_TESTNET);

        left.add(config, BorderLayout.CENTER);

        config.add(publishButton = new JButton("Publish"));
        publishButton.setToolTipText("Publish your contract on the selected network");
        publishButton.addActionListener(this);

        config.add(listContractsButton = new JButton("List My Contracts"));
        listContractsButton.setToolTipText("List all contracts you published");
        listContractsButton.addActionListener(this);

        JPanel codePanelForm = new JPanel(new BorderLayout());
        codeAreaForm = new JTextArea(10, 30);
        codeAreaForm.setEditable(false);
        JScrollPane codeScrollForm = new JScrollPane(codeAreaForm);
        codeScrollForm.setBorder(new TitledBorder("AT FORMATTED BYTECODE"));
        codePanelForm.add(codeScrollForm, BorderLayout.CENTER);
        methodHashArea = new JTextArea(10, 30);
        methodHashArea.setEditable(false);
        JScrollPane methodHashAreaScroll = new JScrollPane(methodHashArea);
        methodHashAreaScroll.setBorder(new TitledBorder("METHOD HASH"));
        codePanelForm.add(methodHashAreaScroll, BorderLayout.PAGE_END);

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

            SignumValue fee = BT.getMinRegisteringFee(comp);
            feeField.setText(fee.toUnformattedString());

            codeArea.setText(code);
            codeAreaForm.setText(codeForm);
            codeArea.setCaretPosition(0);
            codeAreaForm.setCaretPosition(0);
            
            methodHashArea.setText("");
            for(Method m : comp.getMethods()) {
            	if (m.getName().equals(Compiler.MAIN_METHOD) || m.getName().equals(Compiler.INIT_METHOD) || !Modifier.isPublic(m.getNode().access))
					continue;
            	methodHashArea.append(m.getName() + ": " + m.getHash() + "\n");
            }

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
            BT.setNodeAddress(nodeField.getItemAt(nodeField.getSelectedIndex()));

            SignumValue fee = SignumValue.fromSigna(feeField.getText());
            SignumValue actFee = SignumValue.fromSigna(actFeeField.getText());
            int deadline = Integer.parseInt(deadlineField.getText());

            BT.registerContract(passphrase, comp, name, description, actFee, fee, deadline)
                    .subscribe(this::onTransactionSent, this::handleError);
        } catch (Exception e) {
            handleError(e);
        }
    }

    private void listContracts() {
        try {
            String passphrase = String.valueOf(passField.getPassword());

            if (passphrase == null || passphrase.length() == 0) {
                JOptionPane.showMessageDialog(getParent(), "Passphrase is empty", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BT.setNodeAddress(nodeField.getItemAt(nodeField.getSelectedIndex()));
            SignumAddress address = BT.getAddressFromPassphrase(passphrase);

            AT ats[] = BT.getContracts(address);

            String rowData [][] = new String[ats.length][3];
            for (int i = 0; i < ats.length; i++) {
                AT at = ats[i];
                rowData[i][0] = at.getName();
                rowData[i][1] = at.getId().getFullAddress();
                rowData[i][2] = Integer.toString(at.getCreationHeight());
            }
            String [] columnNames = {"Name", "Address", "Creation Block"};
            JTable table = new JTable(rowData, columnNames);
            table.getColumnModel().getColumn(0).setPreferredWidth(30);
            table.getColumnModel().getColumn(1).setPreferredWidth(120);
            table.getColumnModel().getColumn(2).setPreferredWidth(30);
            JScrollPane scroll = new JScrollPane(table);

            JOptionPane.showMessageDialog(getParent(), scroll, "Your Contracts", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            handleError(e);
        }
    }

    private void onTransactionSent(TransactionBroadcast transactionBroadcast) {
        setCursor(Cursor.getDefaultCursor());
        closeButton.setEnabled(true);
        publishButton.setEnabled(true);
        listContractsButton.setEnabled(true);

        JOptionPane.showMessageDialog(getParent(), "Transaction ID: " + transactionBroadcast.getTransactionId(),
                "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleError(Throwable t) {
    	t.printStackTrace();
        setCursor(Cursor.getDefaultCursor());
        closeButton.setEnabled(true);
        publishButton.setEnabled(true);
        listContractsButton.setEnabled(true);

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
            listContractsButton.setEnabled(false);
            publishAT();
        } else if (e.getSource() == listContractsButton) {
            listContracts();
        }
    }
}