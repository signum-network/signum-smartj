package bt;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JButton;
import javax.swing.JTextField;

/**
 * A textfield with a hint value.
 * 
 * @author jjos
 */
class HintTextField extends JTextField implements FocusListener, ActionListener {
	private static final long serialVersionUID = 1L;
	
	private String hint;
	private JButton button;
	private boolean showingHint;
	private Color fgColor;
	
	public HintTextField(String hint, JButton b) {
		super(hint);
		this.hint = hint;
		this.button = b;
		this.showingHint = true;
		this.fgColor = getForeground();
		this.setForeground(getDisabledTextColor());
		super.addFocusListener(this);
		super.addActionListener(this);
	}

	@Override
	public void focusGained(FocusEvent e) {
		if(this.getText().isEmpty()) {
			super.setText("");
			this.setForeground(fgColor);
			showingHint = false;
		}
	}
	@Override
	public void focusLost(FocusEvent e) {
		if(this.getText().isEmpty()) {
			this.setForeground(getDisabledTextColor());
			super.setText(hint);
			showingHint = true;
		}
	}

	@Override
	public String getText() {
		return showingHint ? "" : super.getText();
	}
	
	public boolean isShowingHint() {
		return showingHint;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(button!=null)
			button.doClick();
	}

	@Override
	public void setText(String t) {
		super.setText(t);
		this.setForeground(fgColor);
		showingHint = false;
	}
}

