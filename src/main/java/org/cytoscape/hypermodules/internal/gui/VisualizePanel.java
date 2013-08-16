package org.cytoscape.hypermodules.internal.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.task.VisualizeNetworkTask;
import org.cytoscape.work.TaskIterator;

/**
 * 
 * Diagnostic - visualize a submodule of the current network given a string of the following
 * format: seedName,gene2,gene3,gene4
 * (seedName followed by other genes in the module, separated by commas)
 * @author alvinleung
 *
 */
public class VisualizePanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private JTextField text;
	private JButton execute;
	private CytoscapeUtils utils;

	public VisualizePanel(CytoscapeUtils utils){
		this.utils = utils;
		this.text = new JTextField("");
		this.execute = new JButton("execute");
		this.execute.addActionListener(this);
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(text);
		this.text.setPreferredSize(new Dimension(330, 40));
		this.add(execute);

	}
	
	@Override
	public void actionPerformed(ActionEvent ae) {
		String s = text.getText();
		utils.taskMgr.execute(new TaskIterator(new VisualizeNetworkTask(utils, s)));
	}

}
