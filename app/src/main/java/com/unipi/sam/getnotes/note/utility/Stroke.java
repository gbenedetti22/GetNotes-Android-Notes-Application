package com.unipi.sam.getnotes.note.utility;

import android.graphics.Path;
import android.graphics.RectF;

import androidx.annotation.NonNull;

import com.google.firebase.database.Exclude;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;


public class Stroke extends Path implements Serializable {
    private final int color;
    private int strokeWidth;
    private ArrayList<Action> actions = new ArrayList<>();
    private LinkedList<Point> capturedPoints = new LinkedList<>();
    private boolean erase;
    private static final int MAX_RECORD_POINTS = 20;

    public Stroke(int color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public void refresh() {
        if (actions.isEmpty()) return;
        reset();

        for (Action a : actions) {
            switch (a.action) {
                case Action.LINE_TO: {
                    super.lineTo(a.x, a.y);
                    break;
                }
                case Action.MOVE_TO: {
                    super.moveTo(a.x, a.y);
                    break;
                }
            }
        }
    }

    public int getColor() {
        return color;
    }

    @Override
    public void lineTo(float x, float y) {
        actions.add(new Action(x, y, Action.LINE_TO));
        super.lineTo(x, y);
    }

    @Override
    public void moveTo(float x, float y) {
        actions.add(new Action(x, y, Action.MOVE_TO));
        super.moveTo(x, y);
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setErase(boolean erase, int size) {
        this.erase = erase;
        this.strokeWidth = size;
    }

    public boolean isBlank() {
        return erase;
    }

    public void generatePoints() {
        int numPoints = actions.size() / MAX_RECORD_POINTS;
        numPoints = Math.max(numPoints, 1);

        for (int i = 0; i < actions.size(); i += numPoints) {
            capturedPoints.add(new Point(actions.get(i).x, actions.get(i).y));
        }
    }

    private boolean belongstoLine(Point p1, Point p2, RectF r) {
        ArrayList<RectF> bounds = getBounds(p1, p2);
        for (RectF bound : bounds) {
            if (r.intersect(bound))
                return true;
        }
        return false;
    }

    @NonNull
    private ArrayList<RectF> getBounds(Point p1, Point p2) {
        int numSquares = MAX_RECORD_POINTS;
        ArrayList<RectF> rectFS = new ArrayList<>(numSquares);

        float startX = p1.x;
        float startY = p1.y;

        for (int i = 0; i <= numSquares; i++) {
            Point p = getPoint(p1, p2, i, numSquares);

            Path tempPath = new Path();
            tempPath.moveTo(startX, startY);
            tempPath.lineTo(p.x, p.y);
            RectF bounds = new RectF();
            tempPath.computeBounds(bounds, true);
            rectFS.add(bounds);

            startX = p.x;
            startY = p.y;
        }

        return rectFS;
    }

    private Point getPoint(Point startPoint, Point endPoint, int segment, int totalSegments) {
        Point p1 = new Point();

        float px = (startPoint.x + (int) ((double) (endPoint.x - startPoint.x) / (double) totalSegments) * segment);
        float py = (startPoint.y + (int) ((double) (endPoint.y - startPoint.y) / (double) totalSegments) * segment);

        p1.set(px, py);

        return p1;
    }

    public boolean contains(RectF rectF) {
        if (rectF.isEmpty()) return false;

        for (int i = 0; i < capturedPoints.size() - 1; i++) {
            int x = Math.round(rectF.centerX());
            int y = Math.round(rectF.centerY());

            if (belongstoLine(capturedPoints.get(i), capturedPoints.get(i + 1), rectF)) {
                return true;
            }
        }

        return false;
    }

    public LinkedList<Point> getPoints() {
        return capturedPoints;
    }

    @NonNull
    @Override
    public String toString() {
        return capturedPoints.toString();
    }

    public static class Action implements Serializable {
        public static final int LINE_TO = 0;
        public static final int MOVE_TO = 1;
        private final float x;
        private final float y;
        private final int action;

        public Action(float x, float y, int action) {
            this.x = x;
            this.y = y;
            this.action = action;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public int getAction() {
            return action;
        }
    }

    public static class Point implements Serializable {
        private float x;
        private float y;

        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public Point() {
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public void setX(float x) {
            this.x = x;
        }

        public void setY(float y) {
            this.y = y;
        }

        @Exclude
        public void set(float x, float y) {
            setX(x);
            setY(y);
        }

        @NonNull
        @Override
        public String toString() {
            return "(" + x + ", " + y + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Point point = (Point) o;
            return x == point.x && y == point.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }
}
