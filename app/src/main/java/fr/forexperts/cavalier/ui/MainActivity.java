package fr.forexperts.cavalier.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import fr.forexperts.cavalier.R;
import fr.forexperts.cavalier.util.PrefUtils;

import static fr.forexperts.cavalier.util.LogUtils.makeLogTag;

public class MainActivity extends Activity implements View.OnClickListener {

    private static final String TAG = makeLogTag(MainActivity.class);

    private static Button mNewGameButton;
    private static ChessboardView mChessboard;
    private static TextView mBestScoreTextView;
    private static TextView mCurrentScoreTextView;

    private static int mBestScoreValue = 1;
    private static int mCurrentScoreValue = 1;

    private static String CHESSBOARD_VIEW_KEY = "chessboardView";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mNewGameButton = (Button) findViewById(R.id.new_game_button);
        mChessboard = (ChessboardView) findViewById(R.id.chessboard);
        mBestScoreTextView = (TextView) findViewById(R.id.best_value);
        mCurrentScoreTextView = (TextView) findViewById(R.id.score_value);

        mBestScoreValue = PrefUtils.getBestScore(this);
        mCurrentScoreValue = PrefUtils.getCurrentScore(this);

        mNewGameButton.setOnClickListener(this);

        mBestScoreTextView.setText(Integer.toString(mBestScoreValue));
        mCurrentScoreTextView.setText(Integer.toString(mCurrentScoreValue));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.new_game_button:
                // Clear the current score
                mCurrentScoreValue = 0;
                mCurrentScoreTextView.setText(Integer.toString(mCurrentScoreValue));

                // Clear the preferences
                PrefUtils.clearCurrentScore(this);
                PrefUtils.clearPosition(this);

                // Clear the chessboard
                mChessboard.newGame();
        }
    }

    public void updateScore() {
        mCurrentScoreValue++;
        PrefUtils.setCurrentScore(this, mCurrentScoreValue);
        mCurrentScoreTextView.setText(Integer.toString(mCurrentScoreValue));

        // Check if the current score is the best score
        if (mCurrentScoreValue > mBestScoreValue) {
            mBestScoreValue = mCurrentScoreValue;
            PrefUtils.setBestScore(this, mBestScoreValue);
            mBestScoreTextView.setText(Integer.toString(mBestScoreValue));
        }
    }
}
