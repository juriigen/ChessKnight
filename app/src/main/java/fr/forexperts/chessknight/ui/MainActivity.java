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
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameUtils;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import java.util.HashMap;

import fr.forexperts.chessknight.R;
import fr.forexperts.chessknight.util.PrefUtils;

import static fr.forexperts.chessknight.util.LogUtils.makeLogTag;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = makeLogTag(MainActivity.class);

    @Bind(R.id.score) TextView mScoreTextView;
    @Bind(R.id.score_value) TextView mCurrentScoreValueTextView;
    @Bind(R.id.best_score) TextView mBestScoreTextView;
    @Bind(R.id.best_score_value) TextView mBestScoreValueTextView;
    @Bind(R.id.game_description) TextView mDescriptionTextView;
    @Bind(R.id.chessboard) ChessboardView mChessboard;

    private static HashMap<Integer, Integer> mBestScore;
    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;
    private static int mGameNumberCounter = 1;

    private static InterstitialAd mInterstitialAd;

    private static GoogleApiClient mGoogleApiClient;

    private static int RC_SIGN_IN = 9001;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;

    private static int REQUEST_LEADERBOARD = 1;

    private static HashMap<Integer, String> mAchievementID;
    private static HashMap<Integer, String> mLeaderboardID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Create the Google Api Client with access to the Play Games services
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        // Fill Google Play Services data array
        fillAchievementIDArray();
        fillLeaderboardIDArray();

        // Set up Ad banner
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Set up Interstitial
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-7370519346258326/7481993699");
        requestNewInterstitial();

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                newGame();
            }
        });

        // Set up scores
        mBestScore = PrefUtils.getBestScore(this);
        mBestScoreValue = mBestScore.get(PrefUtils.getColumnsNumber(this));
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

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (mResolvingConnectionFailure) {
            // Already resolving
            return;
        }

        // If the sign-in button was clicked or if auto sign-in is enabled,
        // launch the sign-in flow
        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = true;

            // Attempt to resolve the connection failure using BaseGameUtils.
            if (!BaseGameUtils.resolveConnectionFailure(this,
                    mGoogleApiClient, connectionResult,
                    RC_SIGN_IN, getString(R.string.signin_other_error))) {
                mResolvingConnectionFailure = false;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {
        // Attempt to reconnect
        mGoogleApiClient.connect();
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mSignInClicked = false;
            mResolvingConnectionFailure = false;
            if (resultCode == RESULT_OK) {
                mGoogleApiClient.connect();
            } else {
                // Bring up an error dialog to alert the user that sign-in failed.
                BaseGameUtils.showActivityResultError(this,
                        requestCode, resultCode, R.string.signin_failure);
            }
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder().build();
        mInterstitialAd.loadAd(adRequest);
    }

    @OnClick(R.id.new_game_button)
    public void newGame() {
        if (mInterstitialAd.isLoaded() && mGameNumberCounter % 3 == 0) {
            mInterstitialAd.show();
        } else {
            // Clear the current score
            mCurrentScoreValue = 1;
            mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

            // Load the corresponding best score
            mBestScoreValue = mBestScore.get(PrefUtils.getColumnsNumber(this));
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));

            // Clear the preferences
            PrefUtils.clearForbiddenSquare(this);
            PrefUtils.clearCurrentScore(this);
            PrefUtils.clearPosition(this);

            // Clear the chessboard
            mChessboard.newGame();

            // Increment game number counter
            mGameNumberCounter++;
        }
    }

    public void newRound() {
        PrefUtils.clearPosition(this);

        // Clear the chessboard
        mChessboard.startNewRound();
    }

    @OnClick(R.id.undo_button)
    public void undo() {
        mChessboard.undoLastMove();
    }

    @OnClick(R.id.leaderboard_button)
    public void getLeaderboard() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(mGoogleApiClient),
                    REQUEST_LEADERBOARD);
        } else {
            showSignInDialog();
        }
    }

    @OnClick(R.id.change_size_button)
    public void changeChessBoardSize() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_change_size);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        final TextView chessboardSize = (TextView) dialog.findViewById(R.id.chessboard_size);
        chessboardSize.setTypeface(typeface);
        int columnsNumber = PrefUtils.getColumnsNumber(this);
        chessboardSize.setText(columnsNumber + "x" + columnsNumber);

        SeekBar chessboardSizeControl = (SeekBar) dialog.findViewById(R.id.chessboard_size_control);
        chessboardSizeControl.setProgress(columnsNumber - 4);
        chessboardSizeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + 4;
                chessboardSize.setText(progress + "x" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PrefUtils.setColumnsNumber(MainActivity.this, seekBar.getProgress() + 4);
            }
        });

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    private void showSignInDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sign_in);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView pleaseSignInLabel = (TextView) dialog.findViewById(R.id.not_signed_label);
        pleaseSignInLabel.setTypeface(typeface);

        Button signInButton = (Button) dialog.findViewById(R.id.sign_in_button);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGoogleApiClient.connect();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    public void updateScore(boolean undo) {
        if (!undo) {
            mCurrentScoreValue++;
        } else {
            mCurrentScoreValue--;
        }
        PrefUtils.setCurrentScore(this, mCurrentScoreValue);
        mCurrentScoreValueTextView.setText(Integer.toString(mCurrentScoreValue));

        // Check if the current score is the best score
        if (mCurrentScoreValue > mBestScoreValue) {
            mBestScoreValue = mCurrentScoreValue;
            mBestScore.put(PrefUtils.getColumnsNumber(this), mBestScoreValue);
            PrefUtils.saveBestScore(this, mBestScore);
            mBestScoreValueTextView.setText(Integer.toString(mBestScoreValue));
        }
    }

    public void endGame() {
        int columnsNumber = PrefUtils.getColumnsNumber(this);
        if (mCurrentScoreValue == columnsNumber * columnsNumber) {
            showWinDialog();
        } else {
            showGameOverDialog();
        }
    }

    private void showGameOverDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_game_over);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView gameOverLabel = (TextView) dialog.findViewById(R.id.game_over);
        gameOverLabel.setTypeface(typeface);

        TextView scoreValue = (TextView) dialog.findViewById(R.id.score_value);
        scoreValue.setText(Integer.toString(mCurrentScoreValue));
        scoreValue.setTypeface(typeface);

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Check if an achievement "Play X games in a row" is completed
            if (mGameNumberCounter == 10) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(1));
            } else if (mGameNumberCounter == 20) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(2));
            } else if (mGameNumberCounter == 50) {
                Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(3));
            }

            // Check if the best score had been beat
            if (mCurrentScoreValue == mBestScoreValue) {
                int columnsNumber = PrefUtils.getColumnsNumber(this);
                Games.Leaderboards.submitScore(mGoogleApiClient, mLeaderboardID.get(columnsNumber),
                        mBestScoreValue);
            }
        }
    }

    private void showWinDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_win);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        TextView winLabel = (TextView) dialog.findViewById(R.id.win);
        winLabel.setTypeface(typeface);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            int columnsNumber = PrefUtils.getColumnsNumber(this);
            Games.Achievements.unlock(mGoogleApiClient, mAchievementID.get(columnsNumber));
        }

        TextView scoreValue = (TextView) dialog.findViewById(R.id.score_value);
        scoreValue.setText(Integer.toString(mCurrentScoreValue));
        scoreValue.setTypeface(typeface);

        Button newGameButton = (Button) dialog.findViewById(R.id.new_game_button);
        newGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newGame();
                dialog.dismiss();
            }
        });

        Button continueButton = (Button) dialog.findViewById(R.id.continue_button);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newRound();
                dialog.dismiss();
            }
        });

        dialog.show();

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = dialog.getWindow();
        lp.copyFrom(window.getAttributes());
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
    }

    private void fillAchievementIDArray() {
        mAchievementID = new HashMap<>();
        mAchievementID.put(1, "CgkIytW_os0bEAIQAw");
        mAchievementID.put(2, "CgkIytW_os0bEAIQBA");
        mAchievementID.put(3, "CgkIytW_os0bEAIQFg");
        mAchievementID.put(4, "CgkIytW_os0bEAIQBQ");
        mAchievementID.put(5, "CgkIytW_os0bEAIQBg");
        mAchievementID.put(6, "CgkIytW_os0bEAIQCg");
        mAchievementID.put(7, "CgkIytW_os0bEAIQCw");
        mAchievementID.put(8, "CgkIytW_os0bEAIQDA");
        mAchievementID.put(9, "CgkIytW_os0bEAIQDQ");
        mAchievementID.put(10, "CgkIytW_os0bEAIQDg");
        mAchievementID.put(11, "CgkIytW_os0bEAIQDw");
        mAchievementID.put(12, "CgkIytW_os0bEAIQEA");
        mAchievementID.put(13, "CgkIytW_os0bEAIQEg");
        mAchievementID.put(14, "CgkIytW_os0bEAIQEw");
        mAchievementID.put(15, "CgkIytW_os0bEAIQFA");
        mAchievementID.put(16, "CgkIytW_os0bEAIQFQ");
    }

    private void fillLeaderboardIDArray() {
        mLeaderboardID = new HashMap<>();
        mLeaderboardID.put(4, "CgkIytW_os0bEAIQFw");
        mLeaderboardID.put(5, "CgkIytW_os0bEAIQGA");
        mLeaderboardID.put(6, "CgkIytW_os0bEAIQGQ");
        mLeaderboardID.put(7, "CgkIytW_os0bEAIQGg");
        mLeaderboardID.put(8, "CgkIytW_os0bEAIQGw");
        mLeaderboardID.put(9, "CgkIytW_os0bEAIQHA");
        mLeaderboardID.put(10, "CgkIytW_os0bEAIQHQ");
        mLeaderboardID.put(11, "CgkIytW_os0bEAIQHg");
        mLeaderboardID.put(12, "CgkIytW_os0bEAIQHw");
        mLeaderboardID.put(13, "CgkIytW_os0bEAIQIA");
        mLeaderboardID.put(14, "CgkIytW_os0bEAIQIQ");
        mLeaderboardID.put(15, "CgkIytW_os0bEAIQIg");
        mLeaderboardID.put(16, "CgkIytW_os0bEAIQIw");
    }
}
