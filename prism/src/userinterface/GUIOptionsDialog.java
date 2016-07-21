//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package userinterface;

import java.util.*;
import javax.swing.*;
import prism.*;
import settings.*;

public class GUIOptionsDialog extends javax.swing.JDialog
{

	private ArrayList panels;
	private PrismSettings settings;

	/** Creates new form GUIOptionsDialog */
	public GUIOptionsDialog(GUIPrism parent)
	{
		super(parent, true);
		settings = parent.getPrism().getSettings();
		panels = new ArrayList();
		initComponents();
		this.getRootPane().setDefaultButton(cancelButton);
		setLocationRelativeTo(getParent()); // centre
		//setResizable(false);

		for (int i = 0; i < settings.optionOwners.length; i++) {
			SettingTable table = new SettingTable(this);

			ArrayList al = new ArrayList();
			settings.optionOwners[i].setDisplay(table);
			al.add(settings.optionOwners[i]);
			table.setOwners(al);
			panels.add(table);
			theTabs.add(table);
			theTabs.setTitleAt(panels.indexOf(table), settings.propertyOwnerNames[i]);
		}
	}

	public void addPanel(OptionsPanel p)
	{
		// defunct
	}

	public void show()
	{
		super.show();
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	private void initComponents()//GEN-BEGIN:initComponents
	{
		jPanel1 = new javax.swing.JPanel();
		theTabs = new javax.swing.JTabbedPane();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		defaultButton = new javax.swing.JButton();
		saveSettingsButton = new javax.swing.JButton();
		jPanel4 = new javax.swing.JPanel();
		cancelButton = new javax.swing.JButton();

		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog(evt);
			}
		});

		jPanel1.setLayout(new java.awt.BorderLayout());

		theTabs.setMinimumSize(new java.awt.Dimension(400, 50));
		theTabs.setPreferredSize(new java.awt.Dimension(400, 500));
		jPanel1.add(theTabs, java.awt.BorderLayout.CENTER);

		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

		jPanel2.setLayout(new java.awt.BorderLayout());

		jPanel2.setMinimumSize(new java.awt.Dimension(400, 35));
		jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

		defaultButton.setMnemonic('D');
		defaultButton.setText("Load Defaults");
		defaultButton.setMaximumSize(new java.awt.Dimension(220, 50));
		defaultButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				defaultButtonActionPerformed(evt);
			}
		});

		jPanel3.add(defaultButton);

		saveSettingsButton.setMnemonic('S');
		saveSettingsButton.setText("Save Options");
		saveSettingsButton.setPreferredSize(new java.awt.Dimension(120, 25));
		saveSettingsButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				saveSettingsButtonActionPerformed(evt);
			}
		});

		jPanel3.add(saveSettingsButton);

		jPanel2.add(jPanel3, java.awt.BorderLayout.CENTER);

		jPanel4.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		cancelButton.setText("Okay");
		cancelButton.setMaximumSize(new java.awt.Dimension(200, 50));
		cancelButton.setMinimumSize(new java.awt.Dimension(80, 25));
		cancelButton.setPreferredSize(new java.awt.Dimension(80, 25));
		cancelButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				cancelButtonActionPerformed(evt);
			}
		});

		jPanel4.add(cancelButton);

		jPanel2.add(jPanel4, java.awt.BorderLayout.EAST);

		getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

		pack();
	}//GEN-END:initComponents

	private void saveSettingsButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_saveSettingsButtonActionPerformed
	{//GEN-HEADEREND:event_saveSettingsButtonActionPerformed

		settings.notifySettingsListeners();

		try {
			settings.saveSettingsFile();
		} catch (PrismException e) {
			GUIPrism.getGUI().errorDialog("Error saving settings:\n" + e.getMessage());
		}

	}//GEN-LAST:event_saveSettingsButtonActionPerformed

	private void defaultButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_defaultButtonActionPerformed
	{//GEN-HEADEREND:event_defaultButtonActionPerformed

		String[] selection = { "Yes", "No" };
		int selectionNo = -1;

		selectionNo = JOptionPane.showOptionDialog(this, "Are you sure you wish to load the default settings?\nAll previous settings will be lost.",
				"Save Settings", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, selection, selection[0]);
		if (selectionNo == 0) {
			settings.loadDefaults();
			settings.notifySettingsListeners();
		}

	}//GEN-LAST:event_defaultButtonActionPerformed

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
		hide();
	}//GEN-LAST:event_cancelButtonActionPerformed

	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog

	// Variables declaration - do not modify//GEN-BEGIN:variables
	javax.swing.JButton cancelButton;
	javax.swing.JButton defaultButton;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JButton saveSettingsButton;
	javax.swing.JTabbedPane theTabs;
	// End of variables declaration//GEN-END:variables

}
