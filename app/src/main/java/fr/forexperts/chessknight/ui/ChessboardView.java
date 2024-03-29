/**
 * Copyright 2015 Baptiste Robert
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.forexperts.chessknight.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

import fr.forexperts.chessknight.util.PrefUtils;

import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class ChessboardView extends View {
    private static final String TAG = makeLogTag(ChessboardView.class);

    private int x0, y0, sqSize;
    private int selectedSquare;
    private static int columnsNumber = 3;
    private ArrayList<Integer> position;
    private ArrayList<Integer> forbiddenSquare;
    private ArrayList<Integer> validMoves;

    private Paint darkPaint;
    private Paint brightPaint;
    private Paint redPaint;
    private Paint greenPaint;
    private Paint whitePiecePaint;

    public ChessboardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        x0 = y0 = sqSize = 0;

        validMoves = new ArrayList<>();

        // Load last chessboard size
        columnsNumber = PrefUtils.getColumnsNumber(getContext());

        // If the last game was not finished, get the last knight position;
        int lastKnightPosition = PrefUtils.getPositionKnight(getContext());
        if (lastKnightPosition != -1) {
            selectedSquare = lastKnightPosition;
        } else {
            selectedSquare = randInt(0, columnsNumber * columnsNumber - 1);
        }

        // If the last game was not finished, get the last position of the last game.
        ArrayList<Integer> lastPosition = PrefUtils.getPosition(getContext());
        if (lastPosition.size() != 0) {
            position = lastPosition;
        } else {
            position = new ArrayList<>();
        }

        // If the last game was not finished, get the forbidden square of the last game.
        ArrayList<Integer> lastForbiddenSquare = PrefUtils.getForbiddenSquare(getContext());
        if (lastForbiddenSquare.size() != 0) {
            forbiddenSquare = lastForbiddenSquare;
        } else {
            forbiddenSquare = new ArrayList<>();
        }

        darkPaint = new Paint();
        darkPaint.setARGB(255, 0, 0, 0);

        brightPaint = new Paint();
        brightPaint.setARGB(255, 192, 192, 192);

        redPaint = new Paint();
        redPaint.setARGB(255, 200, 0, 0);

        greenPaint = new Paint();
        greenPaint.setARGB(255, 48, 142, 106);

        whitePiecePaint = new Paint();
        whitePiecePaint.setARGB(255, 255, 255, 255);
        whitePiecePaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int minSize = Math.min(width, height);
        setMeasuredDimension(minSize, minSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();
        sqSize = (Math.min(width, height) - (columnsNumber >> 1)) / columnsNumber;
        x0 = (width - sqSize * columnsNumber) / 2;
        y0 = (height - sqSize * columnsNumber) / 2;

        updateValidMoves(selectedSquare);

        for (int x = 0; x < columnsNumber; x++) {
            for (int y = 0; y < columnsNumber; y++) {
                // Draw the square
                final int xCrd = getXCrd(x);
                final int yCrd = getYCrd(y);
                Paint paint = darkSquare(x, y) ? darkPaint : brightPaint;
                canvas.drawRect(xCrd, yCrd, xCrd + sqSize, yCrd + sqSize, paint);

                // Draw a red square if the knight had been already in this square
                int sq = getSquare(x, y);

                if (forbiddenSquare.contains(sq)) {
                    drawForbiddenSquare(canvas, xCrd + sqSize / 2, yCrd + sqSize / 2);
                } else {
                    if (position.contains(sq)) {
                        canvas.drawRect(xCrd, yCrd, xCrd + sqSize, yCrd + sqSize, redPaint);
                    }

                    // Draw the piece into the selected square
                    if (sq == selectedSquare) {
                        drawPiece(canvas, xCrd + sqSize / 2, yCrd + sqSize / 2);
                    }
                }

                // Draw green square if it's a valid moves square
                int currentSquare = getSquare(x, y);
                if (validMoves.contains(currentSquare)) {
                    canvas.drawRect(xCrd, yCrd, xCrd + sqSize, yCrd + sqSize, greenPaint);
                }
            }
        }

        if (isGameFinished(selectedSquare)) {
            ((MainActivity) getContext()).endGame();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        setSelection(eventToSquare(event), false);
        return true;
    }

    /**
     * Set/clear the selected square.
     * @param square The square to select, or -1 to clear selection.
     */
    final public void setSelection(int square, boolean undo) {
        if (square != selectedSquare) {
            if (validMove(getX(selectedSquare), getY(selectedSquare),
                    getX(square), getY(square))) {
                // Update the score
                ((MainActivity) getContext()).updateScore(undo);

                // Refresh position
                if (!undo) {
                    position.add(selectedSquare);
                }
                PrefUtils.savePosition(getContext(), position);

                // Refresh knight position
                PrefUtils.setKnightPosition(getContext(), square);

                // Change the current square
                selectedSquare = square;

                // Refresh the chessboard
                invalidate();
            }
        }
    }

    /**
     * Compute the square corresponding to the coordinates of a mouse event.
     * @param evt Details about the mouse event.
     * @return The square corresponding to the mouse event, or -1 if outside board.
     */
    final int eventToSquare(MotionEvent evt) {
        int xCrd = (int) evt.getX();
        int yCrd = (int) evt.getY();

        int sq = -1;
        if ((xCrd >= x0) && (yCrd >= y0) && (sqSize > 0)) {
            int x = (xCrd - x0) / sqSize;
            int y = columnsNumber - 1 - (yCrd - y0) / sqSize;
            if ((x >= 0) && (x < columnsNumber) && (y >= 0) && (y < columnsNumber)) {
                sq = getSquare(x, y);
            }
        }
        return sq;
    }

    /**
     * Return index in squares[] vector corresponding to (x,y).
     */
    public static int getSquare(int x, int y) {
        return y * columnsNumber + x;
    }

    /**
     * Return x position (file) corresponding to a square.
     */
    public static int getX(int square) {
        return square % columnsNumber;
    }

    /**
     * Return y position (rank) corresponding to a square.
     */
    public static int getY(int square) {
        return square / columnsNumber;
    }

    private void drawPiece(Canvas canvas, int xCrd, int yCrd) {
        String ps = "m"; // White Knight
        if (ps.length() > 0) {
            Paint paint = whitePiecePaint;
            paint.setTextSize(sqSize);
            Typeface casefont = Typeface.createFromAsset(getContext().getAssets(),
                    "fonts/casefont.ttf");
            paint.setTypeface(casefont);
            Rect bounds = new Rect();
            paint.getTextBounds(ps, 0, ps.length(), bounds);
            int xCent = bounds.centerX();
            int yCent = bounds.centerY();
            canvas.drawText(ps, xCrd - xCent, yCrd - yCent, paint);
        }
    }

    private void drawForbiddenSquare(Canvas canvas, int xCrd, int yCrd) {
        String ps = "x"; // Cross
        if (ps.length() > 0) {
            Paint paint = whitePiecePaint;
            paint.setTextSize(sqSize);
            Typeface casefont = Typeface.createFromAsset(getContext().getAssets(),
                    "fonts/casefont.ttf");
            paint.setTypeface(casefont);
            Rect bounds = new Rect();
            paint.getTextBounds(ps, 0, ps.length(), bounds);
            int xCent = bounds.centerX();
            int yCent = bounds.centerY();
            canvas.drawText(ps, xCrd - xCent, yCrd - yCent, paint);
        }
    }

    private boolean validMove(int xfrom, int yfrom, int xto, int yto) {
        return (((Math.abs(xfrom - xto) == 1 && Math.abs(yfrom - yto) == 2) ||
                (Math.abs(yfrom - yto) == 1 && Math.abs(xfrom - xto) == 2)) &&
                !position.contains(getSquare(xto, yto)) &&
                !forbiddenSquare.contains(getSquare(xto, yto)));
    }

    private boolean isGameFinished(int square) {
        for (int i = 0; i < columnsNumber; i++) {
            for (int j = 0; j < columnsNumber; j++) {
                if (validMove(getX(square), getY(square), i, j)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void updateValidMoves(int square) {
        validMoves.clear();
        for (int i = 0; i < columnsNumber; i++) {
            for (int j = 0; j < columnsNumber; j++) {
                if (validMove(getX(square), getY(square), i, j)) {
                    validMoves.add(getSquare(i, j));
                }
            }
        }
    }

    private int getXCrd(int x) {
        return x0 + sqSize * x;
    }
    private int getYCrd(int y) {
        return y0 + sqSize * (columnsNumber - 1 - y);
    }

    /**
     * Return true if (x,y) is a dark square.
     */
    private static boolean darkSquare(int x, int y) {
        return (x & 1) == (y & 1);
    }

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public void newGame() {
        columnsNumber = PrefUtils.getColumnsNumber(getContext());
        selectedSquare = randInt(0, columnsNumber * columnsNumber - 1);
        x0 = y0 = sqSize = 0;
        position = new ArrayList<>();
        forbiddenSquare = new ArrayList<>();
        invalidate();
    }

    public void startNewRound() {
        x0 = y0 = sqSize = 0;
        position = new ArrayList<>();

        // TODO: To optimize can be long at the end
        if (forbiddenSquare.size() < columnsNumber * columnsNumber - 1) {
            int newForbiddenSquare = randInt(0, columnsNumber * columnsNumber - 1);
            while (newForbiddenSquare == selectedSquare
                    || forbiddenSquare.contains(newForbiddenSquare)) {
                newForbiddenSquare = randInt(0, columnsNumber * columnsNumber - 1);
            }
            forbiddenSquare.add(newForbiddenSquare);
        }
        PrefUtils.saveForbiddenSquare(getContext(), forbiddenSquare);

        // Update the score
        ((MainActivity) getContext()).updateScore(false);

        invalidate();
    }

    public boolean undoLastMove() {
        if (position.size() > 1) {
            int lastMove = position.get(position.size() - 1);
            position.remove(position.indexOf(lastMove));
            setSelection(lastMove, true);
            return true;
        }
        return false;
    }
}