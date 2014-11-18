/***********************************************************************
 *
 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis
 * The University of Liverpool
 *
 *
 * BranchingStoryGenerator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * BranchingStoryGenerator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 *
 ************************************************************************/

package uk.ac.liverpool.narrative;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;
import javax.swing.JTextArea;

public class SolutionPreview extends JTextArea implements
		PropertyChangeListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 21391093991659429L;

	public SolutionPreview(JFileChooser fc) {
		setPreferredSize(new Dimension(240, 100));
		fc.addPropertyChangeListener(this);
		this.setLineWrap(true);
		// this.setWrapStyleWord(false);
	}

	File file;

	@Override
	public void propertyChange(PropertyChangeEvent e) {
		boolean update = false;
		String prop = e.getPropertyName();

		// If the directory changed, don't show an image.
		if (JFileChooser.DIRECTORY_CHANGED_PROPERTY.equals(prop)) {
			file = null;
			update = true;

			// If a file became selected, find out which one.
		} else if (JFileChooser.SELECTED_FILE_CHANGED_PROPERTY.equals(prop)) {
			file = (File) e.getNewValue();
			update = true;
		}

		// Update the preview accordingly.
		if (update) {
			if (isShowing()) {
				doPreview();
				repaint();
			}
		}
	}

	private void doPreview() {
		if (file == null)
			return;
		ObjectInputStream dao;
		try {
			dao = new ObjectInputStream(new GZIPInputStream(
					new FileInputStream(file)));

			List<Solution> so1;
			so1 = (List<Solution>) dao.readObject();
			dao.close();

			StringBuffer sb = new StringBuffer();
			sb.append("Domain name: " + so1.get(0).domainName).append('\n');
			sb.append("R.seed: " + so1.get(0).seed).append('\n');
			sb.append("Invocation : ").append('\n')
					.append(so1.get(0).generatingConditions).append('\n');
			sb.append("Nr. of sol.: " + so1.size()).append('\n');

			this.setText(sb.toString());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
