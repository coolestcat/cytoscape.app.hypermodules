package org.cytoscape.hypermodules.internal;

import java.net.URL;
/**
 * 
 * gets the images for the expanded and collapsed icon for collapsible panels
 * @author alvinleung
 *
 */
public class Resources {
	public static enum ImageName {

		ARROW_EXPANDED("/img/arrow_expanded.gif"),
		ARROW_COLLAPSED("/img/arrow_collapsed.gif");

		private final String name;

		private ImageName(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}
	
	public static URL getUrl(ImageName img) {
		return Resources.class.getResource(img.toString());
	}
}
