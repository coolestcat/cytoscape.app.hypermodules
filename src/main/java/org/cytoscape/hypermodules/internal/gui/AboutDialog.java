package org.cytoscape.hypermodules.internal.gui;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLEditorKit;

import org.cytoscape.hypermodules.internal.CytoscapeUtils;
import org.cytoscape.util.swing.OpenBrowser;
/**
 * 
 * A short JDialog explaining the app, with a link to the paper where the algorithm comes from. Right 
 * now only a short outline.
 * @author alvinleung
 *
 */
public class AboutDialog extends JDialog {

	private CytoscapeUtils utils;
	private String version;
	private OpenBrowser openBrowser;
	
	private JEditorPane editorPane;
	private JPanel buttonPanel;
	
	public AboutDialog(CytoscapeUtils utils){
		super(utils.swingApp.getJFrame(), "About HyperModules", false);
		this.utils = utils;
		this.version = "1.0";
		this.openBrowser = utils.openBrowser;
		setResizable(false);
		getContentPane().add(getMainContainer(), BorderLayout.CENTER);
		getContentPane().add(getButtonPanel(), BorderLayout.SOUTH);
		pack();
	}
	
	private static final long serialVersionUID = 1L;

	
	private JEditorPane getMainContainer(){
		editorPane = new JEditorPane();
		editorPane.setMargin(new Insets(10, 10, 10, 10));
		editorPane.setEditable(false);
		editorPane.setEditorKit(new HTMLEditorKit());
		editorPane.addHyperlinkListener(new HyperlinkAction());

		String text = "<html><body>" +
					  "<P align=center><b>HyperModules v" + version + "</b><BR>" +
					  "A Cytoscape App<BR><BR>" +

					  "Version " + version + " by <a href='http://www.baderlab.org/'>Bader Lab</a>, University of Toronto<BR>" +
					  "Version 1.0 by Alvin Leung, McGill University<BR>" +

					  "If you use this app in your research, please cite:<BR>" +
					  "Reimand J, Bader GD" +
					  "<a href='http://www.ncbi.nlm.nih.gov/pmc/articles/PMC3564258/'>Systematic analysis of somatic mutations in phosphorylation signaling<BR>" + 
					  "predicts novel cancer drivers<BR>" +
					  "</P></body></html>";

		editorPane.addKeyListener(new KeyListener() {
			
			@Override
			public void keyTyped(KeyEvent e) {
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
					case KeyEvent.VK_ESCAPE:
						dispose();
						break;
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
			}
		});
		editorPane.setText(text);
		return editorPane;
		
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
	
	
	
	
	private class HyperlinkAction implements HyperlinkListener {
		@Override
		public void hyperlinkUpdate(HyperlinkEvent event) {
			if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				openBrowser.openURL(event.getURL().toString());
			}
		}
	}
	
	
	
	
	
	
	
	
}
