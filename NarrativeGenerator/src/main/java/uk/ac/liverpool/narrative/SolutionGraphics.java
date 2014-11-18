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

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.planning.STRIPSState;
import javaff.search.BranchingSearch;
import javaff.search.BranchingUtils;
import javaff.search.CharacterActionSolutionFilter;
import javaff.search.CharacterFilter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import uk.ac.liverpool.narrative.SolutionGraph.EdgeColor;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest2;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.DirectionalEdgeArrowTransformer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.LayoutTransition;
import edu.uci.ics.jung.visualization.renderers.BasicRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.util.Animator;

public class SolutionGraphics implements Runnable {

	private static final Font DERIVE_FONT = new JLabel().getFont().deriveFont(
			Font.PLAIN, 12);
	private static final Font DERIVE_FONT2 = new JLabel().getFont().deriveFont(
			Font.BOLD, 13);

	Template template;
	public final static Polygon triangle = new Polygon();
	public final static Rectangle rect = new Rectangle(-10, -10, 20, 20);
	public final static Shape ellipse = new Ellipse2D.Double(-10, -10, 20, 20);

	public SolutionGraph sg;

	Collection<Set<STRIPSState>> cliques;

	Map<STRIPSState, Color> cm = new HashMap<STRIPSState, Color>();

	@Parameter(names = { "-t", "-template" }, description = "Path to template file", required = true)
	public String templateFilePath;

	@Parameter(names = { "-ac" }, description = "Path to action cost file")
	public String actionCostsFilePath;
	@Parameter(names = { "-cc" }, description = "Path to character cost file")
	public String characterCostsFilePath;
	@Parameter(names = { "-ad" }, description = "Path to author direction file")
	public String authorDirection;
	@Parameter(names = { "-tr", "-threshold" }, description = "Value in [0-1] to indicate the percentage of best results to keep, accoding to the cost filtering")
	public double threshold;
	@Parameter(names = { "-c" }, description = "Acting character/s (comma separated)")
	public String character;
	@Parameter(names = { "-fc" }, description = "Try to remove statics from initial state")
	private static boolean fc;
	static {
		triangle.addPoint(0, -10);
		triangle.addPoint(-10, 10);
		triangle.addPoint(10, 10);
	}

	public SolutionGraphics() {
		sg = new SolutionGraph();
	}

	public SolutionGraphics(List<Solution> sol, Template t) {

		sg = new SolutionGraph(sol);
		template = t;
		sg.setTemplate(template);
	}

	public static void main(final String[] args) throws Exception {

		System.out.println("Current invocation: " + Arrays.toString(args));
		final SolutionGraphics sg = new SolutionGraphics();
		JCommander jc = new JCommander(sg);
		jc.setProgramName("BranchingStoryGenerator");
		try {
			jc.parse(args);
		} catch (ParameterException x) {
			jc.usage();
			System.out.println(x.getMessage());
			System.exit(-1);
		}
		sg.setTemplate(Template.readTemplate(new File(sg.templateFilePath)));

		SwingUtilities.invokeAndWait(sg);

		// sg.sg.simulateCleanup();

		sg.sg.simulateCleanup();
		BranchingSearch.setTemplate(sg.template);
		sg.sg.usm = BranchingSearch.CompareMethod.Jacard;
		sg.sg.doCleanup();
		// sg.sg.usm = BranchingSearch.CompareMethod.Jacard;
		// System.out.println(sg.sg.solutions.size());
		// sg.sg.doCleanup();
		// System.out.println(sg.sg.solutions.size());
		sg.sg.populateGraph();
		if (sg.charFilter != null) {
			sg.sg.keepCharacterBranching(sg.charFilter);
		}
		sg.sg.cleanupLoops(4);
		sg.printSolutions();

		SwingUtilities.invokeAndWait(new Runnable() {

			@Override
			public void run() {
				sg.paintMinSpanningTree();

			}
		});
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				sg.createVisualStateGraph(false, 900);

			}
		});

		try {
			sg.sg.runStory(sg.template, System.out, System.in);
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setTemplate(Template templ) {
		this.template = templ;

	}

	public void printSolutions() throws IOException {
		String lcs = "";

		if (sg.solutions.size() > 1) {
			String x;

			x = template.apply_template(sg.solutions.get(0).actions);
			for (int i = 1; i < sg.solutions.size(); i++) {
				String y = template.apply_template(sg.solutions.get(i).actions);
				x = BranchingUtils.longestCommonSubstring(x, y);
			}
			if (x.lastIndexOf('.') != -1) {
				lcs = x.substring(0, x.lastIndexOf('.') + 1);
			}
		}

		if (!sg.solutions.isEmpty()) {
			System.out.println("Solutions: ");

			if (template.apply_template(sg.solutions.get(0).actions)
					.startsWith(lcs)) {
				System.out.println("common text:");
				System.out.println(lcs);
			}

			for (Solution s : sg.solutions) {
				try {
					System.out.println("Nr of actions: " + s.actions.size());
					String ss = template.apply_template(s.actions);
					if (ss.startsWith(lcs)) {
						ss = ss.replaceFirst(lcs, "");
					}
					System.out.println(ss);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

	}

	@Override
	public void run() {
		// set up action cost filter
		if (actionCostsFilePath != null
				&& (new File(actionCostsFilePath)).exists()) {
			CharacterActionSolutionFilter tc = new CharacterActionSolutionFilter();
			if (actionCostsFilePath != null
					&& (new File(actionCostsFilePath)).exists()) {
				tc.setActionCosts(BranchingStoryGenerator.readCosts(
						actionCostsFilePath,
						BranchingStoryGenerator.actionCostType));
			}
			if (characterCostsFilePath != null
					&& (new File(characterCostsFilePath)).exists()) {
				tc.setCharacterCosts(BranchingStoryGenerator.readCosts(
						characterCostsFilePath,
						BranchingStoryGenerator.characterCostType));
			}
			if (authorDirection != null && (new File(authorDirection)).exists()) {
				tc.setAuthorDirection(BranchingStoryGenerator
						.readCosts(authorDirection,
								BranchingStoryGenerator.authorCostType).get(
								BranchingStoryGenerator.authorCostType));
			}
			sg.filters.add(tc);
			tc.setThreshold(threshold);
		}
		if (character != null) {
			charFilter = new CharacterFilter(character);
		}

		if (fc) {
			JFileChooser fc = new JFileChooser(
					BranchingStoryGenerator.DestinationFolder);
			fc.setAccessory(new SolutionPreview(fc));
			FileNameExtensionFilter filter = new FileNameExtensionFilter(
					"Solution files", "sol");
			fc.setFileFilter(filter);
			fc.setMultiSelectionEnabled(true);
			fc.showOpenDialog(vvv);
			File[] files = fc.getSelectedFiles();
			for (File f : files) {
				try {
					sg.readSolutionFile(f);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} else {
			for (File f : BranchingStoryGenerator.DestinationFolder.listFiles()) {
				if (!f.isFile() || !f.getName().endsWith(".sol")) {
					continue;
				}
				try {
					sg.readSolutionFile(f);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		System.out.println("done");

	}

	VisualizationViewer<STRIPSState, Action> vvv;

	public int DFSVisit(STRIPSState s, Graph<STRIPSState, Action> f,
			Set<STRIPSState> toRemove) {

		int k = 0;

		Collection<Action> aa = f.getOutEdges(s);
		if (aa != null) {
			for (Action a : aa) {
				STRIPSState e = f.getOpposite(s, a);
				k += DFSVisit(e, f, toRemove);

			}
		}
		if (sg.ends.contains(s)) {
			k = 1;
		}
		if (k == 0) {
			toRemove.add(s);
		}
		return k;
	}

	public void paintMinSpanningTree() {
		MinimumSpanningForest2<STRIPSState, Action> f = new MinimumSpanningForest2<STRIPSState, Action>(
				sg.ggraph, new DelegateForest<STRIPSState, Action>(),
				DelegateTree.<STRIPSState, Action> getFactory(),
				new ConstantTransformer(1.0));
		Forest<STRIPSState, Action> tree = f.getForest();

		Set<STRIPSState> toRemove = new SetWrapper<STRIPSState>();
		DFSVisit(sg.start, tree, toRemove);
		for (STRIPSState s : toRemove) {
			tree.removeVertex(s);
		}
		TreeLayout<STRIPSState, Action> layout1 = new TreeLayout<STRIPSState, Action>(
				tree, 220, 60);

		VisualizationModel<STRIPSState, Action> vm1 = new DefaultVisualizationModel<STRIPSState, Action>(
				layout1, new Dimension(1600, 1200));

		final VisualizationViewer<STRIPSState, Action> vv1 = new VisualizationViewer<STRIPSState, Action>(
				vm1, new Dimension(1600, 1200));
		vv1.setVertexToolTipTransformer(new ToStringLabeller());
		vv1.getRenderContext().setEdgeLabelTransformer(
				new Transformer<Action, String>() {

					@Override
					public String transform(Action a) {

						return ""
								+ template.apply_template(sg.realActions.get(a));

					}
				});
		vv1.getRenderContext().setArrowFillPaintTransformer(
				new ConstantTransformer(Color.lightGray));
		vv1.setEdgeToolTipTransformer(new Transformer<Action, String>() {

			@Override
			public String transform(Action a) {

				try {

					return "" + sg.realActions.get(a).toString();

				} catch (Exception z) {
					System.out.println(z);
					return "";
				}

			}
		});
		vv1.setBackground(Color.white);
		vv1.getRenderContext().setVertexLabelTransformer(
				new Transformer<STRIPSState, String>() {

					@Override
					public String transform(STRIPSState s) {

						return "";// +Integer.toHexString(s.hashCode());
					}
				});
		vv1.getRenderContext().setEdgeFontTransformer(
				new Transformer<Action, Font>() {
					@Override
					public Font transform(Action s) {

						Action ra = sg.realActions.get(s);
						Collection<Action> set = sg.actionToEdges.get(template
								.apply_template(ra));
						if (set != null && set.size() > 1)
							return DERIVE_FONT;
						return DERIVE_FONT2;
					}
				});

		vv1.getRenderContext().setVertexFillPaintTransformer(
				new Transformer<STRIPSState, Paint>() {
					@Override
					public Paint transform(STRIPSState s) {
						if (vv1.getPickedVertexState().isPicked(s))
							return new Color(250, 250, 0, 207);
						if (sg.start.equals(s))
							return new Color(10, 200, 10, 197);
						else if (sg.ends.contains(s))
							return new Color(220, 10, 10, 197);
						return new Color(255, 210, 40, 197);
					}
				});
		vv1.getRenderContext().setVertexShapeTransformer(
				new StoryStateTransformer());
		vv1.setRenderer(new LabelsLastRenderer());

		vv1.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
		vv1.getRenderContext().setEdgeLabelRenderer(
				new DefaultEdgeLabelRenderer(Color.gray, false));
		// vv1.getRenderContext().setLabelOffset(0);

		vv1.setLayout(new BorderLayout());
		Font font = vv1.getFont().deriveFont(Font.BOLD, 16);
		JLabel vv1Label = new JLabel("Minimum Spanning Trees");
		vv1Label.setFont(font);
		JPanel flow1 = new JPanel();
		flow1.setOpaque(false);
		flow1.add(vv1Label);
		vv1.add(flow1, BorderLayout.NORTH);
		vv1.setGraphMouse(new DefaultModalGraphMouse());
		JFrame f2 = new JFrame();
		f2.add(vv1);
		f2.pack();
		f2.setVisible(true);

	}

	public BufferedImage drawMinSpanningTree() {
		MinimumSpanningForest2<STRIPSState, Action> f = new MinimumSpanningForest2<STRIPSState, Action>(
				sg.ggraph, new DelegateForest<STRIPSState, Action>(),
				DelegateTree.<STRIPSState, Action> getFactory(),
				new ConstantTransformer(1.0));
		Forest<STRIPSState, Action> tree = f.getForest();

		Set<STRIPSState> toRemove = new SetWrapper<STRIPSState>();
		DFSVisit(sg.start, tree, toRemove);
		for (STRIPSState s : toRemove) {
			tree.removeVertex(s);
		}
		TreeLayout<STRIPSState, Action> layout1 = new TreeLayout<STRIPSState, Action>(
				tree, 400, 60);

		final VisualizationImageServer<STRIPSState, Action> vv1 = new VisualizationImageServer<STRIPSState, Action>(
				layout1, layout1.getSize());
		vv1.getRenderContext().setEdgeLabelTransformer(
				new Transformer<Action, String>() {

					@Override
					public String transform(Action a) {

						return ""
								+ template.apply_template(sg.realActions.get(a));

					}
				});
		vv1.getRenderContext().setArrowFillPaintTransformer(
				new ConstantTransformer(Color.lightGray));
		vv1.setBackground(Color.white);
		vv1.getRenderContext().setVertexLabelTransformer(
				new Transformer<STRIPSState, String>() {

					@Override
					public String transform(STRIPSState s) {

						return "";// +Integer.toHexString(s.hashCode());
					}
				});
		vv1.getRenderContext().setEdgeFontTransformer(
				new Transformer<Action, Font>() {
					@Override
					public Font transform(Action s) {

						Action ra = sg.realActions.get(s);
						Collection<Action> set = sg.actionToEdges.get(template
								.apply_template(ra));
						if (set != null && set.size() > 1)
							return DERIVE_FONT;
						return DERIVE_FONT2;
					}
				});

		vv1.getRenderContext().setVertexFillPaintTransformer(
				new Transformer<STRIPSState, Paint>() {
					@Override
					public Paint transform(STRIPSState s) {
						if (vv1.getPickedVertexState().isPicked(s))
							return new Color(250, 250, 0, 207);
						if (sg.start.equals(s))
							return new Color(10, 200, 10, 197);
						else if (sg.ends.contains(s))
							return new Color(220, 10, 10, 197);
						return new Color(255, 210, 40, 197);
					}
				});
		vv1.getRenderContext().setVertexShapeTransformer(
				new StoryStateTransformer());
		vv1.setRenderer(new LabelsLastRenderer());

		vv1.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
		vv1.getRenderContext().setEdgeLabelRenderer(
				new DefaultEdgeLabelRenderer(Color.gray, false));
		BufferedImage image = (BufferedImage) vv1.getImage(new Point2D.Double(
				layout1.getSize().getWidth() / 2,
				layout1.getSize().getHeight() / 2),
				new Dimension(layout1.getSize()));
		return image;

	}

	public static Class currentLayout = KKLayout.class;
	private CharacterFilter charFilter;

	public void repaint() {

		if (vvv != null) {
			vvv.repaint();
		}
	}

	public void relayout() {
		if (vvv != null) {
			KKLayout<STRIPSState, Action> l = new KKLayout<STRIPSState, Action>(
					sg.ggraph);// , new SDist<STRIPSState, Action>(ggraph));
			// l.setInitializer(vvv.getGraphLayout());
			l.setSize(vvv.getSize());
			LayoutTransition<STRIPSState, Action> lt = new LayoutTransition<STRIPSState, Action>(
					vvv, vvv.getGraphLayout(), l);
			Animator animator = new Animator(lt);
			animator.start();
			vvv.getRenderContext().getMultiLayerTransformer().setToIdentity();
			vvv.repaint();

		}
	}

	public void createVisualStateGraph(boolean dynamic, int iterations) {

		KKLayout<STRIPSState, Action> layout = new KKLayout<STRIPSState, Action>(
				sg.ggraph);// , new SDist<STRIPSState, Action>(ggraph));

		layout.setLengthFactor(1.7);
		layout.setAdjustForGravity(false);
		// layout.setDisconnectedDistanceMultiplier(1.5);
		// layout.setExchangeVertices(true);
		layout.setSize(new Dimension(2000, 1200)); // sets the initial size of
		int i = 0;
		if (!dynamic) {
			layout.setMaxIterations(iterations);
			while (!layout.done() && i++ < iterations) {
				layout.step();
			}
			layout.lock(true);
		}
		final VisualizationViewer<STRIPSState, Action> vv = new VisualizationViewer<STRIPSState, Action>(
				layout);
		vvv = vv;
		vv.setPreferredSize(new Dimension(2000, 1200)); // Sets the viewing area

		final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
		vv.setGraphMouse(graphMouse);

		vv.setBackground(Color.white);
		vv.getRenderContext().setVertexLabelTransformer(
				new ConstantTransformer(""));
		vv.getRenderContext().setEdgeFontTransformer(
				new Transformer<Action, Font>() {
					@Override
					public Font transform(Action s) {

						Action ra = sg.realActions.get(s);
						Collection<Action> set = sg.actionToEdges.get(template
								.apply_template(ra));
						if (set != null && set.size() > 1)
							return DERIVE_FONT;
						return DERIVE_FONT2;
					}
				});

		vv.getRenderContext().setEdgeLabelTransformer(
				new Transformer<Action, String>() {
					@Override
					public String transform(Action a) {
						try {
							return sg.actionToString(template, a);
						} catch (IOException e) {
							e.printStackTrace();
							return "";
						}
					}
				});

		vv.getRenderContext().setEdgeDrawPaintTransformer(
				new Transformer<Action, Paint>() {
					@Override
					public Paint transform(Action s) {
						sg.realActions.get(s);
						EdgeColor v = sg.edgeColors.get(s);
						if (v == null)
							return Color.yellow;
						if (v == EdgeColor.black)
							return Color.red;
						else
							return Color.lightGray;
					}
				});
		vv.getRenderContext().setVertexStrokeTransformer(
				new ConstantTransformer(new BasicStroke(0)));
		vv.getRenderContext().setVertexFillPaintTransformer(
				new Transformer<STRIPSState, Paint>() {

					@Override
					public Paint transform(STRIPSState s) {
						if (vv.getPickedVertexState().isPicked(s))
							return new Color(250, 250, 0, 207);
						Color c = cm.get(s);
						if (c != null)
							return c;

						if (sg.start.equals(s))
							return new Color(10, 200, 10, 197);
						else if (sg.ends.contains(s))
							return new Color(220, 10, 10, 197);
						return new Color(255, 210, 40, 197);
					}
				});
		vv.getRenderContext().setVertexShapeTransformer(
				new StoryStateTransformer());
		vv.getRenderContext().setEdgeArrowTransformer(
				new DirectionalEdgeArrowTransformer<STRIPSState, Action>(10, 8,
						1) {
					@Override
					public Shape transform(
							Context<Graph<STRIPSState, Action>, Action> context) {
						if (sg.frequency.get(sg.ggraph.getEndpoints(
								context.element).getFirst()) != null) {
							double c = Math.log(sg.frequency.get(sg.ggraph
									.getEndpoints(context.element).getFirst())) * 0.8;
							if (c > 1) {
								Shape ss = AffineTransform.getScaleInstance(c,
										c).createTransformedShape(
										super.transform(context));
								return ss;
							}
						}

						return super.transform(context);
					}

				});

		vv.setRenderer(new LabelsLastRenderer());

		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.AUTO);
		vv.getRenderContext().setEdgeStrokeTransformer(
				new Transformer<Action, Stroke>() {

					@Override
					public Stroke transform(Action a) {
						if (false)
							return new BasicStroke((sg.actionToEdges
									.get(template.apply_template(sg.realActions
											.get(a))).size()),
									BasicStroke.CAP_ROUND,
									BasicStroke.JOIN_ROUND);
						if (sg.frequency.get(sg.ggraph.getEndpoints(a)
								.getFirst()) != null)
							return new BasicStroke(
									(float) (Math.log(sg.frequency
											.get(sg.ggraph.getEndpoints(a)
													.getFirst())) * 6.0),
									BasicStroke.CAP_ROUND,
									BasicStroke.JOIN_ROUND);
						else
							return new BasicStroke(1);
					}
				});
		vv.getRenderContext().setEdgeLabelRenderer(
				new DefaultEdgeLabelRenderer(Color.gray, false));
		vv.getRenderContext().setLabelOffset(20);
		vv.getRenderContext().setArrowFillPaintTransformer(
				new ConstantTransformer(Color.lightGray));

		final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);

		JPanel jp = new JPanel();
		jp.setBackground(Color.WHITE);
		jp.setLayout(new BorderLayout());
		jp.add(panel, BorderLayout.CENTER);
		JPanel top = new JPanel(new FlowLayout());

		jp.add(top, BorderLayout.NORTH);
		JFrame frame = new JFrame("Story state graph");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.getContentPane().add(jp);
		frame.pack();
		frame.setVisible(true);

	}

	public final class StoryStateTransformer implements
			Transformer<STRIPSState, Shape> {
		@Override
		public Shape transform(STRIPSState s) {
			Shape ss;
			if (sg.start.equals(s)) {
				ss = rect;
			} else if (sg.ends.contains(s)) {
				ss = triangle;
			} else {
				ss = ellipse;
			}

			if (sg.frequency.get(s) != null) {
				int l = sg.frequency.get(s);
				double c = Math.log(l) / 2.0;
				if (c > 1) {
					ss = AffineTransform.getScaleInstance(c, c)
							.createTransformedShape(ss);
				}
			}
			return ss;
		}
	}

	public final class LinesBetweenSameActionsRenderer extends
			BasicRenderer<STRIPSState, Action> {
		@Override
		public void render(RenderContext<STRIPSState, Action> rc,
				Layout<STRIPSState, Action> layout) {
			try {
				for (Action e : layout.getGraph().getEdges()) {

					renderEdge(rc, layout, e);

					Collection<Action> aa = sg.actionToEdges.get(template
							.apply_template(sg.realActions.get(e)));
					GraphicsDecorator g = rc.getGraphicsContext();
					Color c = g.getColor();
					g.setColor(new Color(Color.LIGHT_GRAY.getRed() + 30,
							Color.LIGHT_GRAY.getGreen() + 30, Color.LIGHT_GRAY
									.getBlue() + 30, 207));

					g.setStroke(new BasicStroke(1));
					if (aa != null) {
						for (Action a : aa) {
							Pair<STRIPSState> p = layout.getGraph()
									.getEndpoints(a);
							if (p == null) {
								continue;
							}
							Pair<STRIPSState> r = layout.getGraph()
									.getEndpoints(e);
							if (r == null) {
								continue;
							}
							if (a.equals(e)) {
								continue;
							}

							STRIPSState v1 = p.getSecond();
							STRIPSState v2 = r.getSecond();
							Point2D p1 = layout.transform(v1);
							Point2D p2 = layout.transform(v2);
							p1 = rc.getMultiLayerTransformer().transform(
									Layer.LAYOUT, p1);
							p2 = rc.getMultiLayerTransformer().transform(
									Layer.LAYOUT, p2);

							g.draw(new Line2D.Float(p1, p2));

						}
					}
					g.setColor(c);

				}

				for (STRIPSState v : layout.getGraph().getVertices()) {

					renderVertex(rc, layout, v);

				}
				for (Action e : layout.getGraph().getEdges()) {

					renderEdgeLabel(rc, layout, e);
				}

				for (STRIPSState v : layout.getGraph().getVertices()) {

					renderVertexLabel(rc, layout, v);
				}
			} catch (ConcurrentModificationException cme) {
				rc.getScreenDevice().repaint();
			}
		}
	}

	public static final class LabelsLastRenderer extends
			BasicRenderer<STRIPSState, Action> {
		@Override
		public void render(RenderContext<STRIPSState, Action> rc,
				Layout<STRIPSState, Action> layout) {
			try {
				for (Action e : layout.getGraph().getEdges()) {

					renderEdge(rc, layout, e);
				}

				for (STRIPSState v : layout.getGraph().getVertices()) {

					renderVertex(rc, layout, v);

				}
				for (Action e : layout.getGraph().getEdges()) {

					renderEdgeLabel(rc, layout, e);
				}

				for (STRIPSState v : layout.getGraph().getVertices()) {

					renderVertexLabel(rc, layout, v);
				}
			} catch (ConcurrentModificationException cme) {
				rc.getScreenDevice().repaint();
			}
		}
	}

	// public void createVisualActionGraph() {
	//
	// // Note that we can use the same nodes and edges in two different
	// // graphs.
	// Graph<String, String> g = new SparseMultigraph<String, String>();
	// int c = 0;
	// for (Solution ac : solutions) {
	// c++;
	// Set<String> vertices = new SetWrapper<String>();
	// Set<String> vertices1 = new SetWrapper<String>();
	//
	// for (int n = 0; n < ac.actions.size() - 1; n++) {
	// Action a1 = ac.actions.get(n);
	// Action a2 = ac.actions.get(n + 1);
	// try {
	// String s1 = template.apply_template(a1);
	// String s2 = template.apply_template(a2);
	//
	// // g.addVertex(s2);
	// if (n == 0)
	// s1 = "Start " + s1;
	// if (n == ac.actions.size() - 1)
	// s1 = "End " + s1;
	// if (n == ac.actions.size() - 2)
	// s2 = "End " + s2;
	//
	// if (vertices.contains(s1))
	// s1 = s1 + "-";
	// if (vertices1.contains(s2))
	// s2 = s2 + "-";
	// if (vertices.contains(s1 + "-"))
	// s1 = s1 + "--";
	// if (vertices1.contains(s2+"-"))
	// s2 = s2 + "--";
	// vertices.add(s1);
	// vertices1.add(s2);
	//
	// g.addEdge((c + 1) + " " + n + " " + s1 + "-" + s2, s1, s2,
	// EdgeType.DIRECTED);
	// // System.out.println(s1 + " = " + s2);
	// } catch (Exception x) {
	// System.out.println(x);
	// }
	// }
	//
	// }
	//
	// KKLayout<String, String> layout = new KKLayout<String, String>( g);
	// layout.setAdjustForGravity(false);
	// layout.setMaxIterations(1000);
	// layout.setLengthFactor(1.6);
	// layout.setExchangeVertices(true);
	// // The BasicVisualizationServer<V,E> is parameterized by the edge types
	// VisualizationViewer<String, String> vv = new VisualizationViewer<String,
	// String>(
	// layout);
	// vv.setPreferredSize(new Dimension(1250, 850)); // Sets the viewing area
	// // size
	// vv.getRenderContext().setEdgeLabelTransformer(
	// new Transformer<String, String>() {
	//
	// @Override
	// public String transform(String s) {
	// int a = Integer.parseInt(s.substring(
	// s.indexOf(' ') + 1,
	// s.indexOf(' ', s.indexOf(' ') + 1)));
	// return "" + a;
	// }
	// });
	// vv.getRenderContext().setVertexLabelTransformer(
	// new ToStringLabeller<String>());
	// vv.getRenderContext().setEdgeDrawPaintTransformer(
	// new Transformer<String, Paint>() {
	//
	// @Override
	// public Paint transform(String s) {
	// int a = Integer.parseInt(s.substring(0, s.indexOf(' ')));
	//
	// return Color.getHSBColor(
	// ((a + 7) * 1337 % 256) / 256.0f,
	// ((a + 7) * 1237 % 256) / 256.0f, 0.7f);
	// }
	// });
	//
	// vv.getRenderContext().setVertexFillPaintTransformer(
	// new Transformer<String, Paint>() {
	//
	// @Override
	// public Paint transform(String s) {
	// if (s.startsWith("Start"))
	// return Color.green;
	// else if (s.startsWith("End"))
	// return Color.blue;
	// return Color.orange;
	// }
	// });
	// vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
	//
	// vv.getRenderContext().setEdgeLabelRenderer(
	// new DefaultEdgeLabelRenderer(Color.gray, false));
	// vv.getRenderContext().setLabelOffset(0);
	//
	// DefaultModalGraphMouse gm = new DefaultModalGraphMouse();
	// gm.setMode(Mode.PICKING);
	// vv.setGraphMouse(gm);
	//
	// JFrame frame = new JFrame("Story action graph");
	// frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	// frame.getContentPane().add(vv);
	// frame.pack();
	// frame.setVisible(true);
	//
	// }
}