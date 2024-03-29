package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import logic.OptimumExporter;
import util.Constants;

public class OptimumPaintingPanel extends JPanel {

	private static final long serialVersionUID = -2130969442515268077L;
	
	private class TokenPainter implements OptimumPainter {
		@Override
		public void paint(List<GUIOptimum> newOptimums) {
			OptimumPaintingPanel.this.askPaint(newOptimums, this);
		}
	}
	
	private class BinListModel extends AbstractListModel {
		private static final long serialVersionUID = 8557643349450515292L;
		private List<GUIBin> actualList = Collections.emptyList(); 
		
		public void setData(List<GUIBin> bins) {
			removeAllData();
			
			actualList = bins;
			if (bins.size() == 0) return;
			fireIntervalAdded(this, 0, actualList.size() - 1);
		}
		
		public void removeAllData() {
			int size = actualList.size();
			if (size == 0) {
				return;
			}
			
			actualList = Collections.emptyList();
			fireIntervalRemoved(this, 0, size - 1);
		}

		@Override
		public int getSize() {
			return actualList.size();
		}

		@Override
		public Object getElementAt(int index) {
			return actualList.get(index);
		}
	}
	
	private class OptimumListModel extends AbstractListModel {
		
		private static final long serialVersionUID = 226008869736676280L;
		
		private final ArrayList<GUIOptimum> optimums = new ArrayList<GUIOptimum>();
		
		public void addAll(List<? extends GUIOptimum> newOptimums) {
			if (newOptimums.isEmpty()) {
				return;
			}
			
			int startIndex = this.optimums.size();
			this.optimums.addAll(newOptimums);
			fireIntervalAdded(this, startIndex, startIndex + newOptimums.size() - 1);
		}
		
		public void removeAllElements() {
			if (optimums.isEmpty()) {
				return;
			}
			
			int size = optimums.size();
			this.optimums.clear();
			fireIntervalRemoved(this, 0, size - 1);
		}
		
		@Override
		public int getSize() {
			return this.optimums.size();
		}

		@Override
		public Object getElementAt(int index) {
			return this.optimums.get(this.optimums.size() - 1 - index);
		}
		
	}
	
	private class BinDisplayerPane extends JPanel implements Scrollable {
		private static final long serialVersionUID = -2032186887504479184L;
		private boolean noHScroll = true;
		
		private static final int FRACTION = 2;
		
		/**
		 * Returns the size of a component inside this panel: we know
		 * that every component has the same size, so we just take the
		 * first one.
		 * 
		 * @return
		 */
		private int computeVScrollSize() {
			if (getComponentCount() == 0) {
				return 1;
			}
			
			return getComponent(0).getPreferredSize().height / FRACTION + 1;
		}
		
		private int computeHScrollSize() {
			if (getComponentCount() == 0) {
				return 1;
			}
			
			return getComponent(0).getPreferredSize().width / FRACTION + 1;
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return this.getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			if (orientation == SwingConstants.VERTICAL) {
				return this.computeVScrollSize();
			} else {
				return this.computeHScrollSize();
			}
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			if (orientation == SwingConstants.VERTICAL) {
				return this.computeVScrollSize();
			} else {
				return this.computeHScrollSize();
			}
		}

		public void setHScroll(boolean enable) {
			this.noHScroll = !enable;
		}
		
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return noHScroll;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
		
	}
	
	private BinListModel binList;
	/**
	 * Please note that although elements are inserted sequentially,
	 * they are printed backwards by overriding the 
	 * {@link DefaultListModel#getElementAt(int)} method.
	 */
	private OptimumListModel optimumList;
	// use unmodifiable JTextField so text can be selected but not modified
	private JTextField nIteration;
	private JTextField fitnessValue;
	private JTextField nBins;
	private JTextField elapsedTime;
	/**
	 * !!MUST!! CONTAIN AT LEAST ONE {@link JComponent} FOR EACH DISPLAYED BIN,
	 * ADDED IN THE SAME ORDER AS {@link GUIOptimum} STORES THEM.
	 * This means that {@link JPanel#getComponent(int) binDisplayer.getComponent(i)}
	 * returns the component that displays GUIOptimum.getBins().get(i).
	 */
	private BinDisplayerPane binDisplayer;
	private TokenPainter lastToken = null;
	
	private List<Box> boxList = new LinkedList<Box>();
	private int binPerRow = -1;
	private int magnificationFactor = -1;
	private final int MAX_BIN_PER_ROW = 3;
	private boolean firstPrint = true;
	
	public OptimumPaintingPanel() {
		init();
	}
	
	private void init() {
		TitledBorder optTitleBorder = BorderFactory.createTitledBorder("OPTIMUM DISPLAYER");
		optTitleBorder.setTitleFont(GUIUtils.TITLE_FONT);
		optTitleBorder.setTitleJustification(TitledBorder.LEFT);
		this.setBorder(optTitleBorder);
		
		// create components
		final JList binJList = new JList();
		binList = new BinListModel();
		binJList.setModel(binList);
		binJList.setCellRenderer(new DefaultListCellRenderer(){
			private static final long serialVersionUID = 7043632887488233064L;
			private static final String START_STR = "Bin ";
			private final StringBuilder sBuilder = new StringBuilder(START_STR);
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				GUIBin bin = (GUIBin) value;
				sBuilder.setLength(START_STR.length());
				sBuilder.append(bin.getID()).append("  [").append(bin.getNPackets()).
						append(']');
				return super.getListCellRendererComponent(list, sBuilder.toString(), index, isSelected,
						cellHasFocus);
			}
		});
		binJList.setVisibleRowCount(7);
		binJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		binJList.setToolTipText("<HTML>Bins:" +
				"<UL><LI>ID" +
				"<LI>[number of contained packets]</UL>" +
				"Double-click to ensure that selected bin is visible</HTML>");
		JScrollPane binListScroller = new JScrollPane(binJList, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, 
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		binListScroller.setPreferredSize(new Dimension(150, 150));
		TitledBorder binTitle = BorderFactory.createTitledBorder("Bin List");
		binListScroller.setBorder(binTitle);
		
		final JList optimumJList = new JList();
		optimumList = new OptimumListModel();
		optimumJList.setModel(optimumList);
		optimumJList.setCellRenderer(new DefaultListCellRenderer() {
			private static final long serialVersionUID = 9056812305053790194L;
			private static final String startStr = "Opt. #"; 
			private final StringBuilder builder = new StringBuilder(startStr);
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				GUIOptimum opt = (GUIOptimum) value;
				builder.setLength(startStr.length());
				builder.append(list.getModel().getSize()-index).append(' ').
						append('[').append(opt.getBins().size()).append("] ").
						append('@').append(GUIUtils.elapsedTime2String(opt.getElapsedTime()));
				return super.getListCellRendererComponent(list, builder.toString(), index, isSelected,
						cellHasFocus);
			}
		});
		optimumJList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					paintSelectedOptimum(optimumJList);
				}
			}
		});
		optimumJList.getInputMap(JList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
				put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "_printSelOpt");
		optimumJList.getActionMap().put("_printSelOpt", new AbstractAction() {
			private static final long serialVersionUID = 5405232541977075728L;
			@Override
			public void actionPerformed(ActionEvent e) {
				paintSelectedOptimum(optimumJList);
			}
		});
		optimumJList.setVisibleRowCount(7);
		optimumJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		optimumJList.setToolTipText("<HTML>History of found optimum:" +
				"<UL><LI>#incremental optimum number<BR/>" +
				"<LI> [number of bins in the solution]<BR/>" +
				"<LI> @elapsed time before finding the solution</UL>" +
				"<P>Double-click to show selected optimum</P></HTML>");
		JScrollPane optListScroller = new JScrollPane(optimumJList,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		optListScroller.setPreferredSize(new Dimension(240, 150));
		TitledBorder optListTitle = BorderFactory.createTitledBorder("Optimum history");
		optListScroller.setBorder(optListTitle);
		
		JLabel elapsedTimeLbl = new JLabel("elapsed time");
		elapsedTimeLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		nIteration = new JTextField(10);
		nIteration.setEditable(false);
		
		JLabel nIterationLbl = new JLabel("# iterations");
		nIterationLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		elapsedTime = new JTextField(10);
		elapsedTime.setEditable(false);
		
		JLabel fitnessValueLbl = new JLabel("fitness value");
		fitnessValueLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		fitnessValue = new JTextField(10);
		fitnessValue.setEditable(false);
		
		JLabel nBinsLbl = new JLabel("# bins");
		nBinsLbl.setHorizontalAlignment(SwingConstants.RIGHT);
		nBins = new JTextField(10);
		nBins.setEditable(false);
		
		final JFileChooser fileChooser = new JFileChooser(Constants.CURRENT_DIR);
		final OptimumExporter optimumExporter = new OptimumExporter();
		
		JButton exportBtn = new JButton("EXPORT OPTIMUM");
		exportBtn.setFont(GUIUtils.BUTTON_FONT);
		exportBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GUIOptimum selOpt = (GUIOptimum) optimumJList.getSelectedValue();
				
				if (selOpt == null) {
					GUIUtils.showErrorMessage(OptimumPaintingPanel.this, "Please select an optimum first");
					return;
				}
				
				int ret = fileChooser.showSaveDialog(OptimumPaintingPanel.this);
				
				if (ret != JFileChooser.APPROVE_OPTION) {
					return;
				}
				
				try {
					optimumExporter.saveOptimum(fileChooser.getSelectedFile(), selOpt);
				} catch (Exception ex1) {
					GUIUtils.showErrorMessage(OptimumPaintingPanel.this, ex1.toString());
					return;
				}
			}
		});
		
		JButton loadBtn = new JButton("LOAD OPTIMUM");
		loadBtn.setFont(GUIUtils.BUTTON_FONT);
		loadBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int ret = fileChooser.showOpenDialog(OptimumPaintingPanel.this);
				
				if (ret != JFileChooser.APPROVE_OPTION) {
					return;
				}
				
				GUIOptimum loadOpt;
				try {
					loadOpt = optimumExporter.loadOptimum(fileChooser.getSelectedFile());
				} catch (Exception ex1) {
					GUIUtils.showErrorMessage(OptimumPaintingPanel.this, ex1.toString());
					return;
				}
				
				OptimumPaintingPanel.this.paintOptimum(loadOpt, true);
			}
		});
		
		binDisplayer = new BinDisplayerPane();
		final JScrollPane scrollPane = new JScrollPane(binDisplayer, 
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		binJList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int selected = binJList.getSelectedIndex();
					
					if (selected < 0) return;
					
					// binDisplayer contains ONLY one component for each bin
					Component c = binDisplayer.getComponent(selected);
					binDisplayer.scrollRectToVisible(c.getBounds());
				}
			}
		});
		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.add(GUIUtils.getHorizontalSeparator(5, 5), BorderLayout.PAGE_START);
		centerPane.add(scrollPane, BorderLayout.CENTER);
		
		// lay out components
		this.setLayout(new BorderLayout());
		
		JPanel tmpInfoPane = new JPanel();
		GroupLayout layout = new GroupLayout(tmpInfoPane);
		tmpInfoPane.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
						.addComponent(nIterationLbl)
						.addComponent(elapsedTimeLbl)
						.addComponent(fitnessValueLbl)
						.addComponent(nBinsLbl)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
						.addComponent(nIteration, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(elapsedTime, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(fitnessValue, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(nBins, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addComponent(exportBtn)
						.addComponent(loadBtn)
				)
		);
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addGroup(layout.createSequentialGroup()
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
										.addComponent(nIterationLbl)
										.addComponent(nIteration, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								)
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
										.addComponent(elapsedTimeLbl)
										.addComponent(elapsedTime, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								)
						)
						.addComponent(exportBtn)
				)
				.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
						.addGroup(layout.createSequentialGroup()
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
										.addComponent(fitnessValueLbl)
										.addComponent(fitnessValue, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								)
								.addGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
										.addComponent(nBinsLbl)
										.addComponent(nBins, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								)
						)
						.addComponent(loadBtn)
				)
		);
		
		Box infoPanel = Box.createVerticalBox();
		infoPanel.add(Box.createVerticalGlue());
		infoPanel.add(tmpInfoPane);
		infoPanel.add(Box.createVerticalGlue());
		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(binListScroller, BorderLayout.LINE_START);
		topPanel.add(infoPanel, BorderLayout.CENTER);
		topPanel.add(optListScroller, BorderLayout.LINE_END);
		this.add(topPanel, BorderLayout.PAGE_START);
		
		this.add(centerPane, BorderLayout.CENTER);
		
	}
	
	private void paintSelectedOptimum(JList optimumList) {
		GUIOptimum opt = (GUIOptimum) optimumList.getSelectedValue();
		if (opt == null) return;
		this.paintOptimum(opt, false);
	}
	
	public void askPaint(List<GUIOptimum> newOptimums, OptimumPainter painter) {
		// print only if given painter is the last one (avoid spurious)
		if (this.lastToken == null || !this.lastToken.equals(painter)) {
			return;
		}
		
		if (newOptimums.isEmpty()) {
			return;
		}
		
		// add all new optimum to the history...
		this.optimumList.addAll(newOptimums);
		
		// ...but print only the last one!
		paintOptimum(newOptimums.get(newOptimums.size() - 1), false);
	}
	
	private GUIOptimum lastPaintedOpt;
	
	public void paintOptimum(GUIOptimum newOptimum, boolean isLoaded) {
		
		List<GUIBin> bins = newOptimum.getBins();
		assert bins.size() > 0 : "NO BINS NO PARTY";
		
		if (lastPaintedOpt == null) lastPaintedOpt = newOptimum;
		
		this.binList.setData(bins);
		this.elapsedTime.setText(GUIUtils.elapsedTime2String(newOptimum.getElapsedTime()));
		this.fitnessValue.setText(Float.toString(newOptimum.getFitness()));
		this.nIteration.setText(Integer.toString(newOptimum.getNIterations()));
		this.nBins.setText(Integer.toString(bins.size()));
		/* **** DO NOT ADD newOptimum TO optimumList HERE!! ****
		 * in fact this method could be called also to display an optimum
		 * from history
		 */
		
		// ***** display bins *****
		
		/* first calculate maxBinPerRow, magnification factor and set layout.
		 * This should be done at first print OR every time # of bins <
		 * actual binPerRow OR every time current optimum has more bins
		 * than the previous one (this last case should happens only during
		 * testing)
		 */
		if (firstPrint || isLoaded || bins.size() < binPerRow || 
				bins.size() > lastPaintedOpt.getBins().size()) {
			
			firstPrint = false;
			if (isLoaded) {
				firstPrint = true;
			}
			
			Dimension binSize = bins.get(0).getBaseDimension();
			// use dimension of binDisplayer ScrollPane
			Dimension paneSize = binDisplayer.getParent().getSize();
			int hSpace = 5, vSpace = 5;
			
			// calculate maximum bin per row...
			int tmpBinPerRow = (paneSize.width + hSpace) / (binSize.width + hSpace);
			// if tmpBinPerRow < 1 => set at least 1
			tmpBinPerRow = Math.max(tmpBinPerRow, 1);
			binPerRow = Math.min(bins.size(), Math.min(tmpBinPerRow, MAX_BIN_PER_ROW));
			
			/* calculate magnification factor checking also that 
			 * bin height will remain inside the binDispalyer height
			 */
			int widthAvailable = (paneSize.width - binPerRow * hSpace) / binPerRow;
			int magnificationFactorW = Math.max(widthAvailable / binSize.width, 1);
			int magnificationFactorH = Math.max(paneSize.height / binSize.height, 1);
			magnificationFactor = Math.min(magnificationFactorW, magnificationFactorH);
			
			if (binSize.width * magnificationFactor >= widthAvailable) {
				binDisplayer.setHScroll(true);
			} else {
				binDisplayer.setHScroll(false);
			}
			
			// set layout manager (revalidation is automatically performed by setLayout)
			binDisplayer.setLayout(new GridLayout(0, binPerRow, hSpace, vSpace));
		}
		
		// check if there is enough boxes to display all bins
		if (boxList.size() < bins.size()) {
			for (int i = boxList.size(); i < bins.size(); i++) {
				Box binContainer = Box.createVerticalBox();
				boxList.add(binContainer);
				binDisplayer.add(binContainer);
			}
			// binDisplayer layout is changed -> need revalidation
			binDisplayer.revalidate();
		}
		
		Iterator<Box> boxIt = boxList.iterator();
		for (GUIBin bin : bins) {
			
			bin.setMagnificationFactor(magnificationFactor);
			// get panel (previous code ensure that there is enough boxes)
			Box binPanel = boxIt.next();
			
			GUIBin prevBin = null;
			if (binPanel.getComponentCount() > 0) {
				prevBin = (GUIBin) binPanel.getComponent(0);
			}
			
			// reprint the panel only if bin is changed
			if (!bin.equals(prevBin)) {
				binPanel.removeAll();
				
				// always put bin as the first component!!
				bin.setAlignmentX(Component.CENTER_ALIGNMENT);
				binPanel.add(bin);
				binPanel.add(Box.createVerticalStrut(5));
				JLabel label = new JLabel("Bin " + bin.getID());
				label.setAlignmentX(Component.CENTER_ALIGNMENT);
				binPanel.add(label);
				
				binPanel.revalidate();
				binPanel.repaint();
			}
		}
		
		while (boxIt.hasNext()) {
			Box box = (Box) boxIt.next();
			// empty & repaint only not-already-empty boxes
			if (box.getComponentCount() > 0) {
				box.removeAll();
				box.repaint();
			}
		}
		
		// update last painted optimum
		lastPaintedOpt = newOptimum;
	}
	
	public OptimumPainter setUp() {
		reset();
		
		this.lastToken = new TokenPainter();
		return this.lastToken;
	}
	
	public void reset() {
		this.lastToken = null;
		
		this.elapsedTime.setText(null);
		this.fitnessValue.setText(null);
		this.nIteration.setText(null);
		this.nBins.setText(null);
		this.binList.removeAllData();
		this.optimumList.removeAllElements();
		
		this.firstPrint = true;
		this.boxList.clear();
		
		this.binDisplayer.removeAll();
		this.binDisplayer.revalidate();
		this.binDisplayer.repaint();
	}

}
