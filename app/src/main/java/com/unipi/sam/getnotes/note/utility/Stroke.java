package com.unipi.sam.getnotes.note.utility;

import android.graphics.Path;

import java.io.Serializable;
import java.util.ArrayList;

public class Stroke extends Path implements Serializable {
    private final int color;
    private final int strokeWidth;
    private ArrayList<Action> actions = new ArrayList<>();

    public Stroke(int color, int strokeWidth) {
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    public void refresh() {
        if(actions.isEmpty()) return;
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
}
