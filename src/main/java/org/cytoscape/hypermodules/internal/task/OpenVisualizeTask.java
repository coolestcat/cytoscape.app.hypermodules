package org.cytoscape.hypermodules.internal.task;

import javax.swing.JFrame;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.hypermodules.internal.gui.VisualizePanel;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;

/**
 * 
 * Opens the visualize network panel
 * @author alvinleung
 *
 */
public class OpenVisualizeTask implements Task{

	private CytoscapeUtils utils;
	
	public OpenVisualizeTask(CytoscapeUtils utils){
		this.utils = utils;
	}
	
	@Override
	public void cancel() {

	}

	@Override
	public void run(TaskMonitor tm) throws Exception {
		JFrame frame = new JFrame("Visualize Network");
		frame.setSize(350, 100);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		
		VisualizePanel vp = new VisualizePanel(utils);
		frame.add(vp);
		frame.pack();
		
	}

}
