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

package fr.forexperts.cavalier.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import fr.forexperts.cavalier.R;
import fr.forexperts.cavalier.util.PrefUtils;

import static fr.forexperts.cavalier.util.LogUtils.LOGD;
import static fr.forexperts.cavalier.util.LogUtils.makeLogTag;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = makeLogTag(MainActivity.class);

    private static Button mNewGameButton;
    private static TextView mScoreTextView;
    private static TextView mCurrentScoreValueTextView;
    private static TextView mBestScoreTextView;
    private static TextView mBestScoreValueTextView;
    private static TextView mDescriptionTextView;

    private static ChessboardView mChessboard;

    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        mNewGameButton = (Button) findViewById(R.id.new_game_button);
        mChessboard = (ChessboardView) findViewById(R.id.chessboard);
        mScoreTextView = (TextView) findViewById(R.id.score);
        mCurrentScoreValueTextView = (TextView) findViewById(R.id.score_value);
        mBestScoreTextView = (TextView) findViewById(R.id.best_score);
        mBestScoreValueTextView = (TextView) findViewById(R.id.best_score_value);
        mDescriptionTextView = (TextView) findViewById(R.id.game_description);

        mBestScoreValue = PrefUtils.getBestScore(this);
        mCurrentScoreValue = PrefUtils.getCurrentScore(this);

        mNewGameButton.setOnClickListener(this);

        mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        mScoreTextView.setTypeface(typeface);
        mCurrentScoreValueTextView.setTypeface(typeface);
        mBestScoreTextView.setTypeface(typeface);
        mBestScoreValueTextView.setTypeface(typeface);
        mDescriptionTextView.setTypeface(typeface);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_game_button:
                // Clear the current score
                mCurrentScoreValue = 1;
                mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

                // Clear the preferences
                PrefUtils.clearCurrentScore(this);
                PrefUtils.clearPosition(this);

                // Clear the chessboard
                mChessboard.newGame();
                break;
            default:
                LOGD(TAG, "onClick: view id unknown: " + v.getId());
        }
    }

    public void updateScore() {
        mCurrentScoreValue++;
        PrefUtils.setCurrentScore(this, mCurrentScoreValue);
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Check if the current score is the best score
        if (mCurrentScoreValue > mBestScoreValue) {
            mBestScoreValue = mCurrentScoreValue;
            PrefUtils.setBestScore(this, mBestScoreValue);
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        }
    }
}
