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

package fr.forexperts.chessknight.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;

import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class PrefUtils {
    private static final String TAG = makeLogTag(PrefUtils.class);

    /**
     * Integer preference indicates the score of the current game.
     */
    public static final String PREF_CURRENT_SCORE = "pref_current_score";

    /**
     * Integer preference indicates the position of the knight.
     */
    public static final String PREF_POSITION_KNIGHT = "pref_position_knight";

    /**
     * Integer preference indicates the position of the knight.
     */
    public static final String PREF_COLUMNS_NUMBER = "pref_columns_number";

    public static void setCurrentScore(final Context context, final int score) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PREF_CURRENT_SCORE, score).apply();
    }

    public static void setKnightPosition(final Context context, final int positionKnight) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PREF_POSITION_KNIGHT, positionKnight).apply();
    }

    public static void setColumnsNumber(final Context context, final int columnsNumber) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(PREF_COLUMNS_NUMBER, columnsNumber).apply();
    }

    public static void savePosition(final Context context, final ArrayList<Integer> position) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor mEdit = sp.edit();

        mEdit.putInt("position_size", position.size());
        for (int i = 0; i < position.size(); i++) {
            mEdit.putInt("position_" + i, position.get(i));
        }

        mEdit.apply();
    }

    public static void saveBestScore(final Context context,
                                     final HashMap<Integer, Integer> bestScore) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor mEdit = sp.edit();

        for (int i = 4; i <= 16; i++) {
            if (bestScore.containsKey(i)) {
                mEdit.putInt("bestscore_" + i, bestScore.get(i));
            } else {
                mEdit.putInt("bestscore_" + i, 1);
            }
        }

        mEdit.apply();
    }

    public static HashMap<Integer, Integer> getBestScore(final Context context) {
        HashMap<Integer, Integer> bestScore = new HashMap<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        for (int i = 4; i <= 16; i++) {
            bestScore.put(i, sp.getInt("bestscore_" + i, 1));
        }

        return bestScore;
    }

    public static int getCurrentScore(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_CURRENT_SCORE, 1);
    }

    public static int getPositionKnight(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_POSITION_KNIGHT, -1);
    }

    public static int getColumnsNumber(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(PREF_COLUMNS_NUMBER, 6);
    }

    public static ArrayList<Integer> getPosition(final Context context) {
        ArrayList<Integer> position = new ArrayList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int size = sp.getInt("position_size", 0);

        for (int i = 0; i < size; i++) {
            position.add(sp.getInt("position_" + i, -1));
        }

        return position;
    }

    public static void clearPosition(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int size = sp.getInt("position_size", 0);

        for (int i = 0; i < size; i++) {
            sp.edit().remove("position_" + i).apply();
        }
        sp.edit().remove("position_size").apply();
    }

    public static void clearCurrentScore(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().remove(PREF_CURRENT_SCORE).apply();
    }
}
