package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;

public class ErrorDialog extends JDialog{

	private static final long serialVersionUID = 1L;
	private CytoscapeUtils utils;
	private JPanel buttonPanel;
	private JLabel errorMessage;
	
	public ErrorDialog(CytoscapeUtils utils, String errorMessage){
		super(utils.swingApp.getJFrame(), "Error", false);
		this.utils = utils;
		this.errorMessage = new JLabel(errorMessage, JLabel.CENTER);
		
		//this.errorMessage.setFont(new Font("Serif", Font.BOLD, 13));
		this.errorMessage.setAlignmentX(0.5f);
		
		this.setPreferredSize(new Dimension(500, 200));
		getContentPane().add(this.errorMessage, BorderLayout.CENTER);
		getContentPane().add(getButtonPanel(), BorderLayout.SOUTH);
		pack();
	}
	
	private JPanel getButtonPanel(){
		buttonPanel = new JPanel();
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		buttonPanel.add(close);
		close.setAlignmentX(CENTER_ALIGNMENT);
		return buttonPanel;
	}

}
