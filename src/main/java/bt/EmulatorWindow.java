package bt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import bt.compiler.Compiler;
import bt.compiler.Printer;
import burst.kit.burst.BurstCrypto;
import burst.kit.entity.BurstValue;
import burst.kit.entity.response.BRSError;
import burst.kit.entity.response.BroadcastTransactionResponse;
import burst.kit.entity.response.GenerateTransactionResponse;
import burst.kit.service.BurstNodeService;
import io.reactivex.Single;

/**
 * Graphical user interface for the blockchain emulator.
 * 
 * @author jjos
 *
 */
@SuppressWarnings("serial")
public class EmulatorWindow extends JFrame implements ActionListener {

	public static void main(String[] args) {
		new EmulatorWindow(null);
	}

	private JButton forgeButton;
	private JButton airDropButton;
	private JButton sendButton;
	private JButton createATButton;
	private JTextField airDropAddress;
	private JTextField airDropAmount;
	private JTextField sendFrom;
	private JTextField sendTo;
	private JTextField sendAmount;
	private HintTextField sendMessage;
	private JTextField atCreator;
	private JTextField atClassField;
	private JTextField atActivation;
	private JTable addressesTable;
	private JTable txsTable;
	private AbstractTableModel addrsTableModel;
	private AbstractTableModel txsTableModel;
	private JTextField atAddressField;
	private JButton compileATButton;

	public EmulatorWindow(Class<?> c) {
		super("BlockTalk Emulator");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// tooltip configuration
		ToolTipManager.sharedInstance().setInitialDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(15000);

		try {
			Class<?> lafc = null;
			try {
				lafc = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				lafc = Class.forName("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			}
			LookAndFeel laf = (LookAndFeel) lafc.getConstructor().newInstance();
			UIManager.setLookAndFeel(laf);
		} catch (Exception e) {
		}

		JPanel topPanel = new JPanel(new BorderLayout());
		getContentPane().add(topPanel, BorderLayout.PAGE_START);

		JPanel cmdPanel = new JPanel(new GridLayout(0, 5, 2, 0));
		cmdPanel.setBorder(new TitledBorder("ACTIONS"));
		topPanel.add(cmdPanel, BorderLayout.LINE_START);

		cmdPanel.add(forgeButton = new JButton("Forge block"));
		forgeButton.addActionListener(this);
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());

		cmdPanel.add(airDropButton = new JButton("Air drop"));
		airDropButton.addActionListener(this);
		cmdPanel.add(airDropAddress = new HintTextField("Receiver", airDropButton));
		airDropAddress.setToolTipText("The address receiving the air drop");
		cmdPanel.add(airDropAmount = new HintTextField("Amount", airDropButton));
		airDropAmount.setToolTipText("The amount to air drop in BURST = 10\u2078 NQT");
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());

		cmdPanel.add(sendButton = new JButton("Send"));
		sendButton.addActionListener(this);
		cmdPanel.add(sendFrom = new HintTextField("From", sendButton));
		sendFrom.setToolTipText("Sender address");
		cmdPanel.add(sendTo = new HintTextField("To", sendButton));
		sendTo.setToolTipText("Receiver address");
		cmdPanel.add(sendAmount = new HintTextField("Amount", sendButton));
		sendAmount.setToolTipText("The amount to send in BURST = 10\u2078 NQT");
		cmdPanel.add(sendMessage = new HintTextField("Message", sendButton));
		sendMessage.setToolTipText("The message to send");

		cmdPanel.add(createATButton = new JButton("Create Contract"));
		createATButton.addActionListener(this);
		cmdPanel.add(atCreator = new HintTextField("Creator", createATButton));
		atCreator.setToolTipText("Contract creator address");
		cmdPanel.add(atAddressField = new HintTextField("Contract address", createATButton));
		atAddressField.setToolTipText("Address to be assigned to this contract");
		cmdPanel.add(atActivation = new HintTextField("Activation fee", createATButton));
		atActivation.setToolTipText("Contract activation fee in BURST = 10\u2078 NQT");
		cmdPanel.add(atClassField = new HintTextField("Java class path", createATButton));
		atClassField.setToolTipText("Full path for the contract java class");
		if (c != null)
			atClassField.setText(c.getName());

		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(new JLabel());
		cmdPanel.add(compileATButton = new JButton("Compile Contract"));
		compileATButton.addActionListener(this);

		JPanel accountsPanel = new JPanel(new BorderLayout());
		accountsPanel.setBorder(new TitledBorder("ACCOUNTS"));
		topPanel.add(accountsPanel, BorderLayout.CENTER);

		class CellRenderer extends DefaultTableCellRenderer {
			public Component getTableCellRendererComponent(
								JTable table, Object value,
								boolean isSelected, boolean hasFocus,
								int row, int column) {
				JLabel c = (JLabel)super.getTableCellRendererComponent(
					table, value, isSelected, hasFocus, row, column);
				c.setToolTipText(null);
				if(value instanceof Address){
					Address add = (Address)value;
					if(add.contract!=null)
						c.setToolTipText(add.contract.getFieldValues());
				}
				return c;
			}
		}

		addrsTableModel = new AbstractTableModel() {
			@Override
			public String getColumnName(int column) {
				return column == 0 ? "Address" : "Balance";
			}

			@Override
			public Object getValueAt(int r, int c) {
				Address a = Emulator.getInstance().addresses.get(r);
				return c == 0 ? a : ((double) a.balance) / Contract.ONE_BURST;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}

			@Override
			public int getRowCount() {
				return Emulator.getInstance().addresses.size();
			}

			@Override
			public int getColumnCount() {
				return 2;
			}
		};

		addressesTable = new JTable(addrsTableModel);
		addressesTable.getColumnModel().getColumn(0).setCellRenderer(new CellRenderer());
		JScrollPane sp = new JScrollPane(addressesTable);
		sp.setPreferredSize(new Dimension(300, 40));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		addressesTable.setFillsViewportHeight(true);
		accountsPanel.add(sp, BorderLayout.CENTER);

		JPanel txsPanel = new JPanel(new BorderLayout());
		txsPanel.setBorder(new TitledBorder("TRANSACTIONS"));
		getContentPane().add(txsPanel, BorderLayout.CENTER);

		txsTableModel = new AbstractTableModel() {
			static final int SENDER_COL = 0;
			static final int RECEIVER_COL = 1;
			static final int AMOUNT_COL = 2;
			static final int TYPE_COL = 3;
			static final int MSG_COL = 4;
			static final int CONF_COL = 5;

			@Override
			public String getColumnName(int column) {
				switch (column) {
				case SENDER_COL:
					return "Sender";
				case RECEIVER_COL:
					return "Receiver";
				case AMOUNT_COL:
					return "Amount";
				case TYPE_COL:
					return "Type";
				case MSG_COL:
					return "Message";
				case CONF_COL:
					return "Confirmations";
				default:
					return "";
				}
			}

			@Override
			public Object getValueAt(int r, int c) {
				ArrayList<Transaction> txs = Emulator.getInstance().txs;
				Transaction tx = txs.get(txs.size() - r - 1);
				switch (c) {
				case CONF_COL:
					return Emulator.getInstance().currentBlock.height - tx.block.height - 1;
				case TYPE_COL:
					if (tx.type == 2)
						return "New contract";
					else if (tx.type == 1)
						return "Message";
					else
						return "Payment";
				case SENDER_COL:
					return tx.sender == null ? null : tx.sender.rsAddress;
				case RECEIVER_COL:
					return tx.receiver == null ? null : tx.receiver.rsAddress;
				case MSG_COL:
					return tx.msgString!=null ? tx.msgString : tx.msg;
				case AMOUNT_COL:
					return ((double) tx.amount) / Contract.ONE_BURST;
				default:
					break;
				}
				return null;
			}

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}

			@Override
			public int getRowCount() {
				return Emulator.getInstance().txs.size();
			}

			@Override
			public int getColumnCount() {
				return 6;
			}
		};

		txsTable = new JTable(txsTableModel);
		new JScrollPane(txsTable);
		txsTable.setFillsViewportHeight(true);

		sp = new JScrollPane(txsTable);
		sp.setPreferredSize(new Dimension(100, 200));
		sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		txsPanel.add(sp, BorderLayout.CENTER);

		pack();

		setLocationRelativeTo(null);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == forgeButton) {
			try {
				Emulator.getInstance().forgeBlock();
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
			txsTableModel.fireTableDataChanged();
			addrsTableModel.fireTableDataChanged();
		} else if (e.getSource() == airDropButton) {
			String rs = airDropAddress.getText();
			if (rs == null || rs.trim().length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid receiver address", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			double amount = 0;
			try {
				amount = Double.parseDouble(airDropAmount.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Could not parse the amount", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			amount *= Contract.ONE_BURST;

			Address receiver = Emulator.getInstance().getAddress(airDropAddress.getText());
			Emulator.getInstance().airDrop(receiver, (long) amount);

			addrsTableModel.fireTableDataChanged();
		} else if (e.getSource() == sendButton) {
			String from = sendFrom.getText();
			String to = sendTo.getText();

			if (from == null || from.trim().length() == 0 || to == null || to.length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid receiver address", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}

			double amount = 0;
			try {
				amount = Double.parseDouble(sendAmount.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Could not parse the amount", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			amount *= Contract.ONE_BURST;

			Emulator emu = Emulator.getInstance();

			String msg = sendMessage.isShowingHint() ? null : sendMessage.getText();

			emu.send(emu.getAddress(from), emu.getAddress(to), (long) amount, msg);
			txsTableModel.fireTableDataChanged();
			addrsTableModel.fireTableDataChanged();
		} else if (e.getSource() == createATButton) {
			Emulator emu = Emulator.getInstance();

			String creatorRS = atCreator.getText();
			if (creatorRS == null || creatorRS.trim().length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Creator address is empty", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			Address creatorAddr = emu.getAddress(creatorRS);

			String atAddrRS = atAddressField.getText();
			if (atAddrRS == null || atAddrRS.trim().length() == 0) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Contract address is empty", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			Address atAddress = emu.findAddress(atAddrRS);
			if (atAddress != null) {
				JOptionPane.showMessageDialog(EmulatorWindow.this,
						"Contract address already registered, choose a new one", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			atAddress = emu.getAddress(atAddrRS);

			double actAmount = 0;
			try {
				actAmount = Double.parseDouble(atActivation.getText());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Invalid activation fee", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
			actAmount *= Contract.ONE_BURST;

			String atClass = atClassField.getText();
			try {
				Class.forName(atClass);
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(EmulatorWindow.this, "Contract instantiation: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE);
				return;
			}

			emu.createConctract(creatorAddr, atAddress, atClass, (long) actAmount);

			addrsTableModel.fireTableDataChanged();
			txsTableModel.fireTableDataChanged();
		} else if (e.getSource() == compileATButton) {
			String atClass = atClassField.getText();
			try {
				Class.forName(atClass);

				Compiler comp = new Compiler(atClass);

				comp.compile();
				comp.link();

				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintStream out = new PrintStream(baos, true, "UTF-8");
				Printer.printCode(comp.getCode(), out);
				String code = new String(baos.toByteArray(), StandardCharsets.UTF_8);

				baos.reset();
				Printer.print(comp.getCode(), out);
				String codeForm = new String(baos.toByteArray(), StandardCharsets.UTF_8);

				final JDialog dlg = new JDialog(this, "Compiled Contract", ModalityType.APPLICATION_MODAL);
				dlg.setLayout(new BorderLayout());

				byte[] bcode = new byte[comp.getCode().position()];
				System.arraycopy(comp.getCode().array(), 0, bcode, 0, bcode.length);

				if (false) {
					String passphrase = "raised optimal wired corner future designs easier middle hoped missouri sessions clinton";
					String name = atClass;
					String description = atClass;

					BurstCrypto bc = BurstCrypto.getInstance();
					BurstNodeService bns = BurstNodeService.getInstance("http://at-testnet.burst-alliance.org:6876/");

					int deadline = 1440; // 4 days (in blocks of 4 minutes)
					byte[] pubkey = bc.getPublicKey(passphrase);
					Single<GenerateTransactionResponse> createAT = bns.generateCreateATTransaction(pubkey,
							BurstValue.fromBurst(1), deadline, name, description, new byte[0], bcode, new byte[0], 1, 1,
							1, BurstValue.fromBurst(1));

					createAT.flatMap(response -> {
						// Now we need to locally sign the transaction.
						// Get the unsigned transaction bytes from the node's response
						byte[] unsignedTransactionBytes = response.getUnsignedTransactionBytes().getBytes();
						// Locally sign the transaction using our passphrase
						byte[] signedTransactionBytes = bc.signTransaction(passphrase, unsignedTransactionBytes);
						// Broadcast the transaction through the node, still not sending it any
						// sensitive information. Use this as the result of the flatMap so we do not
						// have to call subscribe() twice
						return bns.broadcastTransaction(signedTransactionBytes);
					}).subscribe(this::onTransactionSent, this::handleError);

				}

				JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				dlg.add(buttons, BorderLayout.SOUTH);

				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						dlg.setVisible(false);
					}
				});
				buttons.add(okButton);

				JPanel center = new JPanel(new BorderLayout());

				JPanel codePanel = new JPanel(new BorderLayout());
				codePanel.setBorder(new TitledBorder("AT BYTECODE"));
				JTextArea codeArea = new JTextArea(code, 20, 20);
				codeArea.setLineWrap(true);
				JScrollPane codeScroll = new JScrollPane(codeArea);
				codePanel.add(codeScroll, BorderLayout.CENTER);
				center.add(codePanel, BorderLayout.LINE_START);

				JPanel codePanelForm = new JPanel(new BorderLayout());
				codePanelForm.setBorder(new TitledBorder("AT FORMATTED BYTECODE"));
				JTextArea codeAreaForm = new JTextArea(codeForm, 10, 30);
				JScrollPane codeScrollForm = new JScrollPane(codeAreaForm);
				codePanelForm.add(codeScrollForm, BorderLayout.CENTER);
				center.add(codePanelForm, BorderLayout.CENTER);

				dlg.add(center, BorderLayout.CENTER);

				dlg.pack();
				dlg.setLocationRelativeTo(this);
				dlg.setVisible(true);
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(EmulatorWindow.this, "AT compile problem: " + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
	}

	private void onTransactionSent(BroadcastTransactionResponse response) {
		// Get the transaction ID of the newly sent transaction!
		System.out.println("Transaction sent! Transaction ID: " + response.getTransactionID().getID());
	}

	private void handleError(Throwable t) {
		if (t instanceof BRSError) {
			System.out.println("Caught BRS Error: " + ((BRSError) t).getDescription());
		} else {
			t.printStackTrace();
		}
	}
}
