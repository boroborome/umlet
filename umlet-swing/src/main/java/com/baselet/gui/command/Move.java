package com.baselet.gui.command;

import com.baselet.control.HandlerElementMap;
import com.baselet.control.basics.geom.Point;
import com.baselet.control.basics.geom.PointDouble;
import com.baselet.control.basics.geom.Rectangle;
import com.baselet.control.enums.Direction;
import com.baselet.diagram.CurrentDiagram;
import com.baselet.diagram.DiagramHandler;
import com.baselet.element.interfaces.GridElement;
import com.baselet.element.relation.Relation;
import com.baselet.element.sticking.PointChange;
import com.baselet.element.sticking.PointDoubleIndexed;
import com.baselet.element.sticking.Stickable;
import com.baselet.element.sticking.StickableMap;
import com.baselet.element.sticking.Stickables;
import com.baselet.util.PosSuggestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Move extends Command {

	private static final Logger log = LoggerFactory.getLogger(Move.class);

	private final GridElement entity;

	private final int x, y;

	private final double mouseX;
	private final double mouseY;

	private final boolean isShiftKeyDown;

	private final boolean firstDrag;

	private final boolean useSetLocation;

	private final StickableMap stickables;

	private final Collection<Direction> resizeDirection;

	public GridElement getEntity() {
		return entity;
	}

	private int getX() {
		int zoomedX = x * gridSize();
		log.debug("Zoomed x: " + zoomedX);
		return zoomedX;
	}

	private int gridSize() {
		return HandlerElementMap.getHandlerForElement(entity).getGridSize();
	}

	private int getY() {
		int zoomedY = y * gridSize();
		log.debug("Zoomed y: " + zoomedY);
		return zoomedY;
	}

	public StickableMap getStickables() {
		return stickables;
	}

	public boolean isShiftKeyDown() {
		return isShiftKeyDown;
	}

	private Point getMousePosBeforeDrag() {
		Double zoomedX = mouseX * gridSize();
		Double zoomedY = mouseY * gridSize();
		Point p = new Point((int) Math.round(zoomedX), (int) Math.round(zoomedY));
		log.debug("Zoomed point: " + p);
		return p;
	}

	public Move(Collection<Direction> resizeDirection, boolean absoluteMousePos, GridElement e, int x, int y, Point mousePosBeforeDrag, boolean isShiftKeyDown, boolean firstDrag, boolean useSetLocation, StickableMap stickingStickables) {
		entity = e;
		int gridSize = HandlerElementMap.getHandlerForElement(e).getGridSize();
		this.x = x / gridSize;
		this.y = y / gridSize;
		mouseX = calcRelativePos(absoluteMousePos, mousePosBeforeDrag.getX(), entity.getRectangle().getX(), gridSize);
		mouseY = calcRelativePos(absoluteMousePos, mousePosBeforeDrag.getY(), entity.getRectangle().getY(), gridSize);
		this.isShiftKeyDown = isShiftKeyDown;
		this.firstDrag = firstDrag;
		this.useSetLocation = useSetLocation;
		stickables = stickingStickables;
		this.resizeDirection = resizeDirection;
	}

	public Move(Collection<Direction> resizeDirection, GridElement e, int x, int y, Point mousePosBeforeDrag, boolean isShiftKeyDown, boolean firstDrag, boolean useSetLocation, StickableMap stickingStickables) {
		this(resizeDirection, true, e, x, y, mousePosBeforeDrag, isShiftKeyDown, firstDrag, useSetLocation, stickingStickables);
	}

	/**
	 * Calculates the mouse position
	 * @param absoluteMousePos 	if true then the element location must be subtracted to get a relative position instead of an absolute, otherwise it's already relative
	 * @param mousePos			the absolute mouse position
	 * @param entityLocation	the location of the entity
	 * @param gridSize 			the result is divided by the gridsize because it can be (re)executed on different gridSizes (eg do on 100% zoom, change to 50% zoom and undo/redo)
	 * @return					the mouse position relative to the element, independend from gridSize
	 */
	private double calcRelativePos(boolean absoluteMousePos, int mousePos, int entityLocation, double gridSize) {
		double xCalcBase = mousePos * 1.0;
		if (absoluteMousePos) {
			xCalcBase -= entityLocation;
		}
		return xCalcBase / gridSize;
	}

	@Override
	public void execute(DiagramHandler handler) {
		super.execute(handler);
		if (useSetLocation) {
			entity.setRectangleDifference(getX(), getY(), 0, 0, firstDrag, stickables, true);
		}
		else {
			entity.drag(resizeDirection, getX(), getY(), getMousePosBeforeDrag(), isShiftKeyDown, firstDrag, stickables, true);
		}
		updateStickables(stickables.getStickables(), handler.getDrawPanel().getGridElements());
	}

	private void updateStickables(Set<Stickable> stickables, List<GridElement> gridElements) {
		for (Stickable stickable : stickables) {
			Map<PointDoubleIndexed, Rectangle> pointRectMap = findPointGrids(stickable, gridElements);
			Map<PointDoubleIndexed, PointDouble> suggestPos = calculateSuggestPos(pointRectMap);
			applySuggest(suggestPos, stickable);
		}
	}

	private Map<PointDoubleIndexed, Rectangle> findPointGrids(Stickable stickable, List<GridElement> gridElements) {
		Map<PointDoubleIndexed, Rectangle> pointGridMap = new HashMap<>();
		for (PointDoubleIndexed point : stickable.getStickablePoints()) {
			Rectangle connectedRect = null;
			for (GridElement element : gridElements) {
				if (element instanceof Relation) {
					continue;
				}
				PointDouble realPoint = Stickables.getAbsolutePosition(stickable, point);
				if (isConnected(realPoint, element.getRectangle())) {
					connectedRect = element.getRectangle();
					break;
				}
			}
			if (connectedRect != null) {
				pointGridMap.put(point, connectedRect);
			} else {
				pointGridMap.put(point, new Rectangle(point.getX().intValue(), point.getY().intValue(), 0, 0));
			}
		}
		return pointGridMap;
	}

	private boolean isConnected(PointDouble point, Rectangle rect) {
		int x = point.getX().intValue();
		int y = point.getY().intValue();
		return ((x == rect.x || x == rect.x + rect.width) && y >= rect.y && y <= (rect.y + rect.height))
				|| ((y == rect.y || y == rect.y + rect.height) && x >= rect.x && x <= (rect.x + rect.width));
	}

	private Map<PointDoubleIndexed, PointDouble> calculateSuggestPos(Map<PointDoubleIndexed, Rectangle> pointRectMap) {
		Map<PointDoubleIndexed, PointDouble> suggest = new HashMap<>();
		if (pointRectMap.size() == 2) {
			List<Map.Entry<PointDoubleIndexed, Rectangle>> entries = new ArrayList<>(pointRectMap.entrySet());
			Map.Entry<PointDoubleIndexed, Rectangle> e1 = entries.get(0);
			Map.Entry<PointDoubleIndexed, Rectangle> e2 = entries.get(1);

			PointDoubleIndexed p1 = e1.getKey();
			Rectangle r1 = e1.getValue();

			PointDoubleIndexed p2 = e2.getKey();
			Rectangle r2 = e2.getValue();

			int[] suggestXs = PosSuggestUtil.suggestValue(r1.getX(), r1.getWidth(), r2.getX(), r2.getWidth());
			int[] suggestYs = PosSuggestUtil.suggestValue(r1.getY(), r1.getHeight(), r2.getY(), r2.getHeight());

			suggest.put(p1, new PointDouble(suggestXs[0], suggestYs[0]));
			suggest.put(p2, new PointDouble(suggestXs[1], suggestYs[1]));
		}
		return suggest;
	}


	private void applySuggest(Map<PointDoubleIndexed, PointDouble> suggestPos, Stickable stickable) {
		List<PointChange> changes = new ArrayList<>();
		for (Map.Entry<PointDoubleIndexed, PointDouble> entry : suggestPos.entrySet()) {
			PointDoubleIndexed index = entry.getKey();
			PointDouble org = Stickables.getAbsolutePosition(stickable, index);
			PointDouble suggest = entry.getValue();
			if (org.equals(suggest)) {
				continue;
			}
			changes.add(new PointChange(index.getIndex(),
					(int) (suggest.getX() - org.getX()),
					(int) (suggest.getY() - org.getY())));
		}
		if (!changes.isEmpty()) {
			stickable.movePoints(changes);
		}
	}

	@Override
	public void undo(DiagramHandler handler) {
		super.undo(handler);
		entity.undoDrag();
		entity.updateModelFromText();
		CurrentDiagram.getInstance().getDiagramHandler().getDrawPanel().updatePanelAndScrollbars();
	}

	@Override
	public void redo(DiagramHandler handler) {
		entity.redoDrag();
		entity.updateModelFromText();
		CurrentDiagram.getInstance().getDiagramHandler().getDrawPanel().updatePanelAndScrollbars();
	}

	@Override
	public boolean isMergeableTo(Command c) {
		if (!(c instanceof Move)) {
			return false;
		}
		Move m = (Move) c;
		boolean stickablesEquals = stickables.equalsMap(m.stickables);
		boolean shiftEquals = isShiftKeyDown == m.isShiftKeyDown;
		boolean notBothFirstDrag = !(firstDrag && m.firstDrag);
		return entity == m.entity && useSetLocation == m.useSetLocation && stickablesEquals && shiftEquals && notBothFirstDrag;
	}

	@Override
	public Command mergeTo(Command c) {
		Move m = (Move) c;
		Point mousePosBeforeDrag = firstDrag ? getMousePosBeforeDrag() : m.getMousePosBeforeDrag();
		// Important: absoluteMousePos=false, because the mousePos is already relative from the first constructor call!
		Move ret = new Move(m.resizeDirection, false, entity, getX() + m.getX(), getY() + m.getY(), mousePosBeforeDrag, isShiftKeyDown, firstDrag || m.firstDrag, useSetLocation, stickables);
		entity.mergeUndoDrag();
		return ret;
	}
}
