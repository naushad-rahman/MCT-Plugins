package gov.nasa.arc.mct.scenario.view;

import gov.nasa.arc.mct.components.AbstractComponent;
import gov.nasa.arc.mct.gui.View;
import gov.nasa.arc.mct.scenario.component.ActivityComponent;
import gov.nasa.arc.mct.scenario.component.CostFunctionCapability;
import gov.nasa.arc.mct.scenario.component.CostFunctionComponent;
import gov.nasa.arc.mct.scenario.component.DecisionComponent;
import gov.nasa.arc.mct.scenario.component.DurationCapability;
import gov.nasa.arc.mct.scenario.util.DurationFormatter;
import gov.nasa.arc.mct.scenario.view.TimelineLocalControls.CostOverlay;
import gov.nasa.arc.mct.services.component.ViewInfo;
import gov.nasa.arc.mct.services.component.ViewType;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.SwingUtilities;

/**
 * A View of an Activity object, specifically for use as an embedded view within a 
 * larger timeline. ActivityComponents display as rounded rectangles; Decision 
 * components display as horizontal arrows. 
 * 
 * @author vwoeltje
 */
public class ActivityView extends View implements CostOverlay {
	private static final long serialVersionUID = -3208388859058655187L;
	public static final String VIEW_ROLE_NAME = "Activity";
	public static final ViewInfo VIEW_INFO = 
			new ViewInfo(ActivityView.class, ActivityView.VIEW_ROLE_NAME, ViewType.EMBEDDED);
	private static final BasicStroke SOLID_2PT_LINE_STROKE = new BasicStroke(2f);
	private static final Color DECISION_COLOR = new Color (220,220,220);
	private Color lineColor = new Color(100, 100, 100);
	private Color durationColor = new Color(200,200,200, 100);
	private ActivityBackgroundShape bg = ActivityBackgroundShape.ACTIVITY;
	private DurationCapability durationCapability = null;
	
	public ActivityView(AbstractComponent ac, ViewInfo vi) {
		super(ac,vi);
		if (DecisionComponent.class.isAssignableFrom(ac.getClass())) {
			bg = ActivityBackgroundShape.DECISION;			
		}
		durationCapability = ac.getCapability(DurationCapability.class);
		setOpaque(false);		
		if (ac instanceof ActivityComponent) {
			String type = ((ActivityComponent) ac).getType();
			if (type != null && !type.isEmpty()) {
				durationColor = ScenarioColorPalette.getColorMixed(type, durationColor, 0.75f);
				lineColor = durationColor.darker();
			}
		} else if (ac instanceof DecisionComponent) {
			durationColor = DECISION_COLOR;
		}
	}
	
	@Override
	public void updateMonitoredGUI() {
		revalidate();
		repaint();
		
		// Update parent component views (may include cost graphs)
		Container parent;
		Component child = this;
		while ( (parent = SwingUtilities.getAncestorOfClass(View.class, child)) != null) {
				parent.repaint();
				parent.invalidate();
				parent.validate();
				child = parent;		
		}
	}
	
	public void paintComponent(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			
			RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING,
															RenderingHints.VALUE_ANTIALIAS_ON);
			renderHints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2.setRenderingHints(renderHints);
			
			// Draw activity duration			
			bg.paint(g2, getWidth(), getHeight(), lineColor, durationColor);
			
			// Prepare a tool tip
			AbstractComponent ac = getManifestedComponent();
			String name = ac.getDisplayName();			
			String duration = (durationCapability != null) ?
				DurationFormatter.formatDuration(durationCapability.getEnd() - durationCapability.getStart()) : 
					"";
			
			bg.paintLabels(g2, name, duration, getWidth(), getHeight(), getForeground());

			String type = "";
			if (ac instanceof ActivityComponent) {
				type = ((ActivityComponent) ac).getType();
			} 

			setToolTipText(name + " " + duration + (type.isEmpty() ? "" : (" [" + type + "]")));			
		}
	}
	
	private static enum ActivityBackgroundShape {
		ACTIVITY() {

			@Override
			public void paint(Graphics2D g, int w, int h, Color fg, Color bg) {
				g.setColor(bg);
				g.fillRoundRect(1, 1, w-3, h-3, 3*h/4 , 3*h/4 );
				g.setStroke(SOLID_2PT_LINE_STROKE);
				g.setColor(fg);
				g.drawRoundRect(1, 1, w-3, h-3, 3*h/4 , 3*h/4);	
			}

			@Override
			public void paintLabels(Graphics2D g, String name, String duration,
					int w, int h, Color fg) {
				Font originalFont = g.getFont();
				g.setFont(originalFont.deriveFont(originalFont.getSize2D() - 1.0f));
				FontMetrics metrics = g.getFontMetrics(g.getFont());
				int charHeight = metrics.getHeight();
				int baseline   = metrics.getAscent();
				g.setColor(fg);
				g.drawString(name, 6, baseline + h / 2 - charHeight / 2);
				int durationWidth = metrics.stringWidth(duration);
				if (metrics.stringWidth(name) + durationWidth < w - 12) {					
					g.drawString(duration, w - durationWidth - 6, baseline + h/2 - charHeight/2);
				}
				g.setFont(originalFont);
			}
			
		},
		DECISION() {

			@Override
			public void paint(Graphics2D g, int w, int h, Color fg, Color bg) {
				int x[] = {0, w-h/2,  w-h/2, w, w-h/2, w-h/2, 0 };
				int y[] = {2*h/5, 2*h/5, h/4, h/2, 3*h/4, 3*h/5+1, 3*h/5+1};
				g.setColor(bg);
				g.fillPolygon(x,y,7);				

				// Draw diamond
				int diamondWidth = Math.min(w - h / 2, h * 4);
				
				int dx[] = { w/2 - h/4 - diamondWidth/2, w/2-h/4, w/2 - h/4 + diamondWidth/2, w/2-h/4 };
				int dy[] = { h/2, h/6, h/2, 5*h/6 + 1 };
				g.fillPolygon(dx,dy,4);				
				
//				g.setStroke(SOLID_2PT_LINE_STROKE);
//				g.setColor(fg);
//				g.drawPolygon(x,y,7);
			}

			@Override
			public void paintLabels(Graphics2D g, String name, String duration,
					int w, int h, Color fg) {	
				Font originalFont = g.getFont();
				g.setFont(originalFont.deriveFont(Font.ITALIC, originalFont.getSize2D() - 2.0f));
				int charHeight = g.getFontMetrics(g.getFont()).getHeight();
				int baseline   = g.getFontMetrics(g.getFont()).getAscent();
				int charsWidth = g.getFontMetrics(g.getFont()).charsWidth(name.toCharArray(), 0, name.length());
				
				// only draw if this fits
				if (w - h >= charsWidth) {
					g.setColor(fg);
					g.drawString(name, w/2 - h/4 - charsWidth/2, baseline + h / 2 - charHeight / 2);
				}
				
				g.setFont(originalFont);
			}
			
		};
		
		public abstract void paint(Graphics2D g, int w, int h, Color fg, Color bg);
		public abstract void paintLabels(Graphics2D g, String name, String duration, int w, int h, Color fg);
	}

	@Override
	public List<CostFunctionCapability> getCostFunctions() {
		AbstractComponent comp = getManifestedComponent();
		if (comp instanceof CostFunctionComponent) {
			// TODO: Maybe InternalCostFunction should be capability?
			return ((CostFunctionComponent) comp).getInternalCostFunctions();
		}
		return getManifestedComponent().getCapabilities(CostFunctionCapability.class);
	}
}
