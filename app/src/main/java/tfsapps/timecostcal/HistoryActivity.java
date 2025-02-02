package tfsapps.timecostcal;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final String HISTORY_KEY = "calculation_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        TextView totalTimeView = findViewById(R.id.totalTimeView);
        TextView totalPurchaseView = findViewById(R.id.totalPurchaseView);
        ListView historyListView = findViewById(R.id.historyListView);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String historyJson = prefs.getString(HISTORY_KEY, "[]");

        try {
            JSONArray historyArray = new JSONArray(historyJson);
            ArrayList<String> historyList = new ArrayList<>();
            double totalTime = 0;
            double totalPurchase = 0;

            for (int i = 0; i < historyArray.length(); i++) {
                JSONObject entry = historyArray.getJSONObject(i);

                // 各履歴データを取得
                String result = entry.getString("result");
                double purchaseAmount = entry.getDouble("purchaseAmount");
                double laborTime = entry.getDouble("laborTime");

                // 合計を計算
                totalTime += laborTime;
                totalPurchase += purchaseAmount;

                // 表示用テキストに変換
                String formattedEntry = String.format("購入金額: %,d円\n労働時間: %.2f時間\n時給: %.2f円",
                        (int) purchaseAmount, laborTime, purchaseAmount / laborTime);
                historyList.add(formattedEntry);
            }

            // 合計値を表示
            totalTimeView.setText(String.format("総労働時間: %.2f時間", totalTime));
            totalPurchaseView.setText(String.format("総購入金額: %,d円", (int) totalPurchase));

            // 履歴リストを表示
            HistoryAdapter adapter = new HistoryAdapter(this, historyList);
            historyListView.setAdapter(adapter);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "履歴の読み込みに失敗しました。", Toast.LENGTH_SHORT).show();
        }
    }
}

