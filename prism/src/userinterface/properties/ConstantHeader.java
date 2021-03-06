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

package userinterface.properties;

import java.awt.*;

public class ConstantHeader extends javax.swing.JPanel
{

	/** Creates new form ConstantLine */
	public ConstantHeader()
	{
		initComponents();
		setPreferredSize(new Dimension(1, 2 * (getFontMetrics(getFont()).getHeight() + 4)));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents()
	{
		java.awt.GridBagConstraints gridBagConstraints;

		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();

		setLayout(new java.awt.GridBagLayout());

		setPreferredSize(new java.awt.Dimension(640, 38));
		jLabel1.setText("Name");
		jLabel1.setMaximumSize(new java.awt.Dimension(100, 15));
		jLabel1.setMinimumSize(new java.awt.Dimension(50, 15));
		jLabel1.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.1;
		add(jLabel1, gridBagConstraints);

		jLabel2.setText("Type");
		jLabel2.setMaximumSize(new java.awt.Dimension(150, 15));
		jLabel2.setMinimumSize(new java.awt.Dimension(50, 15));
		jLabel2.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.1;
		add(jLabel2, gridBagConstraints);

		jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabel3.setText("Single Value:");
		jLabel3.setMaximumSize(new java.awt.Dimension(1079, 15));
		jLabel3.setMinimumSize(new java.awt.Dimension(5, 15));
		jLabel3.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.2;
		add(jLabel3, gridBagConstraints);

		jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabel4.setText("Start");
		jLabel4.setMaximumSize(new java.awt.Dimension(1079, 15));
		jLabel4.setMinimumSize(new java.awt.Dimension(5, 15));
		jLabel4.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.2;
		add(jLabel4, gridBagConstraints);

		jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabel5.setText("End");
		jLabel5.setMaximumSize(new java.awt.Dimension(1079, 15));
		jLabel5.setMinimumSize(new java.awt.Dimension(5, 15));
		jLabel5.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 6;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.2;
		add(jLabel5, gridBagConstraints);

		jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		jLabel6.setText("Step");
		jLabel6.setMinimumSize(new java.awt.Dimension(5, 15));
		jLabel6.setPreferredSize(new java.awt.Dimension(100, 15));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 7;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.ipadx = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 0.2;
		add(jLabel6, gridBagConstraints);

		jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		jLabel7.setText("Range:");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 5;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.gridwidth = 3;
		add(jLabel7, gridBagConstraints);

		jPanel1.setMaximumSize(new java.awt.Dimension(21, 21));
		jPanel1.setMinimumSize(new java.awt.Dimension(21, 21));
		jPanel1.setPreferredSize(new java.awt.Dimension(21, 21));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		add(jPanel1, gridBagConstraints);

		jPanel2.setMaximumSize(new java.awt.Dimension(21, 21));
		jPanel2.setMinimumSize(new java.awt.Dimension(21, 21));
		jPanel2.setPreferredSize(new java.awt.Dimension(21, 21));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 4;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.ipadx = 5;
		add(jPanel2, gridBagConstraints);

	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	// End of variables declaration//GEN-END:variables

}
