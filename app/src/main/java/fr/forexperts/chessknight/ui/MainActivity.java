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

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import butterknife.Bind;
import butterknife.ButterKnife;

import butterknife.OnClick;
import fr.forexperts.chessknight.R;
import fr.forexperts.chessknight.util.PrefUtils;

import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class MainActivity extends Activity {

    private static final String TAG = makeLogTag(MainActivity.class);

    @Bind(R.id.score) TextView mScoreTextView;
    @Bind(R.id.score_value) TextView mCurrentScoreValueTextView;
    @Bind(R.id.best_score) TextView mBestScoreTextView;
    @Bind(R.id.best_score_value) TextView mBestScoreValueTextView;
    @Bind(R.id.game_description) TextView mDescriptionTextView;
    @Bind(R.id.chessboard) ChessboardView mChessboard;

    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Set up Ad banner
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Set up scores
        mBestScoreValue = PrefUtils.getBestScore(this);
        mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        mCurrentScoreValue = PrefUtils.getCurrentScore(this);
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Set up font
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        mScoreTextView.setTypeface(typeface);
        mCurrentScoreValueTextView.setTypeface(typeface);
        mBestScoreTextView.setTypeface(typeface);
        mBestScoreValueTextView.setTypeface(typeface);
        mDescriptionTextView.setTypeface(typeface);
    }

    @OnClick(R.id.new_game_button)
    public void newGame(View v) {
        // Clear the current score
        mCurrentScoreValue = 1;
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Clear the preferences
        PrefUtils.clearCurrentScore(this);
        PrefUtils.clearPosition(this);

        // Clear the chessboard
        mChessboard.newGame();
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
