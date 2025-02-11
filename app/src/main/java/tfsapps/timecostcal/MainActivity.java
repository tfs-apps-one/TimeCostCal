package tfsapps.timecostcal;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private EditText purchaseAmountInput;
    private RadioGroup salaryTypeGroup;
    private EditText hourlyWageInput;
    private EditText monthlySalaryInput;
    private EditText annualSalaryInput;
    private EditText monthlyWorkHoursInput;
    private EditText annualWorkHoursInput;
    private TextView resultView;

    private int _exec_func_code = 0;     //確認ダイアログの実行処理コード
    private int LISTMAX = 10;

    // Constants
    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final String HISTORY_KEY = "calculation_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        purchaseAmountInput = findViewById(R.id.purchaseAmountInput);
        salaryTypeGroup = findViewById(R.id.salaryTypeGroup);
        hourlyWageInput = findViewById(R.id.hourlyWageInput);
        monthlySalaryInput = findViewById(R.id.monthlySalaryInput);
        annualSalaryInput = findViewById(R.id.annualSalaryInput);
        monthlyWorkHoursInput = findViewById(R.id.monthlyWorkHoursInput);
        annualWorkHoursInput = findViewById(R.id.annualWorkHoursInput);
        resultView = findViewById(R.id.resultView);

        Button calculateButton = findViewById(R.id.calculateButton);
        Button saveButton = findViewById(R.id.saveButton);
        Button historyButton = findViewById(R.id.historyButton);
        Button TipsButton = findViewById(R.id.TipsButton);
        Button clearButton = findViewById(R.id.clearButton);


        addCommaFormatting(purchaseAmountInput);
        addCommaFormatting(hourlyWageInput);
        addCommaFormatting(monthlySalaryInput);
        addCommaFormatting(annualSalaryInput);

        salaryTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            findViewById(R.id.hourlyWageLayout).setVisibility(View.GONE);
            findViewById(R.id.monthlySalaryLayout).setVisibility(View.GONE);
            findViewById(R.id.annualSalaryLayout).setVisibility(View.GONE);

            if (checkedId == R.id.hourlyWageOption) {
                findViewById(R.id.hourlyWageLayout).setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.monthlySalaryOption) {
                findViewById(R.id.monthlySalaryLayout).setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.annualSalaryOption) {
                findViewById(R.id.annualSalaryLayout).setVisibility(View.VISIBLE);
            }
        });

        calculateButton.setOnClickListener(v -> calculateTimeCost());
        clearButton.setOnClickListener(v -> clearInputs());

        saveButton.setOnClickListener(v -> saveCalculationResult());
        historyButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });

        TipsButton.setOnClickListener(v -> Tips());
    }

    private void saveCalculationResult() {
        String resultText = resultView.getText().toString();

        if (resultText.isEmpty() || !resultText.contains("購入金額")) {
            Toast.makeText(this, "有効な計算結果がありません。", Toast.LENGTH_SHORT).show();
            return;
        }

        ShowConfirmPopup(1);
    }
    private void saveCalculationResultExec() {

        String resultText = resultView.getText().toString();

        try {
            // 購入金額と労働時間を抽出
            String[] lines = resultText.split("\n");
            double purchaseAmount = 0;
            double laborTime = 0;

            for (String line : lines) {
                if (line.contains("購入金額")) {
//                    purchaseAmount = parseInputToDouble(line.replaceAll("[^\\d.]", ""));
                    purchaseAmount = parseInputToDouble(line.replaceAll("[^\\d]", ""));
//                    purchaseAmount = safeParseDouble(line);
                } else if (line.contains("労働時間")) {
//                    laborTime = Double.parseDouble(line.replaceAll("[^\\d.]", ""));
                    laborTime = parseInputToDouble(line.replaceAll("[^\\d.]", ""));
//                    laborTime = safeParseDouble(line);
                }
            }

            if (purchaseAmount == 0 || laborTime == 0) {
                Toast.makeText(this, "保存するデータが正しくありません。", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String historyJson = prefs.getString(HISTORY_KEY, "[]");

            JSONArray historyArray = new JSONArray(historyJson);

            // 最大100件を保持
            if (historyArray.length() >= LISTMAX) {
                historyArray.remove(0);
            }

            JSONObject newEntry = new JSONObject();
            newEntry.put("purchaseAmount", purchaseAmount);
            newEntry.put("laborTime", laborTime);
            newEntry.put("result", resultText);

            historyArray.put(newEntry);

            prefs.edit().putString(HISTORY_KEY, historyArray.toString()).apply();
            Toast.makeText(this, "保存しました。", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存中にエラーが発生しました。", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateTimeCost() {

        try {
            double purchaseAmount = parseInputToDouble(purchaseAmountInput.getText().toString());
            int selectedSalaryType = salaryTypeGroup.getCheckedRadioButtonId();
            double hourlyWage = 0;
            String hourlyWageText = "";

            if (selectedSalaryType == R.id.hourlyWageOption) {
                hourlyWage = parseInputToDouble(hourlyWageInput.getText().toString());
                hourlyWageText = new DecimalFormat("#,##0").format(hourlyWage);
            } else if (selectedSalaryType == R.id.monthlySalaryOption) {
                double monthlySalary = parseInputToDouble(monthlySalaryInput.getText().toString());
                double monthlyWorkHours = parseInputToDouble(monthlyWorkHoursInput.getText().toString());
                if (monthlyWorkHours <= 0){
                    ShowErrorPopup(1);
                    return; //計算できない
                }
                hourlyWage = monthlySalary / monthlyWorkHours;
                hourlyWageText = new DecimalFormat("#,##0").format(hourlyWage);
            } else if (selectedSalaryType == R.id.annualSalaryOption) {
                double annualSalary = parseInputToDouble(annualSalaryInput.getText().toString());
                double annualWorkHours = parseInputToDouble(annualWorkHoursInput.getText().toString());
                if (annualWorkHours <= 0){
                    ShowErrorPopup(1);
                    return; //計算できない
                }
                hourlyWage = annualSalary / annualWorkHours;
                hourlyWageText = new DecimalFormat("#,##0").format(hourlyWage);
            }

            double timeCost = purchaseAmount / hourlyWage;
            double timeCostDay = timeCost / 8;

            /* 計算できない場合、実施しない */
            if (purchaseAmount <= 0 || hourlyWage <= 0){
                ShowErrorPopup(2);
                return;
            }

            DecimalFormat formatter = new DecimalFormat("#,##0");
            String formattedAmount = formatter.format(purchaseAmount);
            String result = String.format(
                    "購入金額　　 ▶︎ %s円\n時給　　　　 ▶︎ %s円\n必要労働時間 ▶︎ %.2f時間\n　(1日8時間労働  %.2f日)",
                    formattedAmount, hourlyWageText, timeCost, timeCostDay);

//            String result = String.format(
//                    "購入金額:\n　▶︎ %s円\n時給:\n　▶︎ %s円\n購入に必要な労働時間:\n　▶︎ %.2f時間",
//                    formattedAmount, hourlyWageText, timeCost);
            resultView.setText(result);
        } catch (NumberFormatException e) {
            resultView.setText("すべてのフィールドに正しい数値を入力してください。");
        }
    }

    private void clearInputs() {
        purchaseAmountInput.setText("");
        hourlyWageInput.setText("");
        monthlySalaryInput.setText("");
        annualSalaryInput.setText("");
        monthlyWorkHoursInput.setText("");
        annualWorkHoursInput.setText("");
        resultView.setText("↓↓ 結果はこちらに表示 ↓↓");
        Toast.makeText(this, "画面をクリアしました。", Toast.LENGTH_SHORT).show();
    }

    private void Tips(){

        String ttl = "";
        String mess = "";

        ttl = "！労働時間(参考値)";
        mess = "\n\n【労働時間】について確認下さい！\n" +
                "\n"+
                "\n【法定労働時間】"+
                "\n労働基準法によって「1日8時間・週40時間」と定められた労働時間の上限となります。"+
                "\n" +
                "\n\n【週当たり】"+
                "\n　1日 8時間 × 5日  = 40 時間\n" +
                "\n\n【月当たり】 ※31日の場合"+
                "\n　31日 ÷ 7日(1週間)   = 4.42 週" +
                "\n　4.42週 × 40時間(週)  = 177 時間 \n" +
                "\n\n【年当たり】"+
                "\n　365日 ÷ 7日(1週間)   = 52.14 週 " +
                "\n　52.14週 × 40時間(週) = 2085 時間 " +
                "\n"+
                "\n　年休105日の場合"+
                "\n　　365日 - 105日 = 260日 " +
                "\n　　260日 × 8時間 = 2080 時間" +
                "\n"+
                "\n　年休120日の場合" +
                "\n　　365日 - 120日 = 245日 " +
                "\n　　245日 × 8時間 = 1960 時間" +
                "\n\n"+
                "\n\n";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(ttl);
        builder.setMessage(mess);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void addCommaFormatting(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    editText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll(",", "");
                    try {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = new DecimalFormat("#,##0").format(parsed);
                        current = formatted;
                        editText.setText(formatted);
                        editText.setSelection(formatted.length());
                    } catch (NumberFormatException e) {
                        current = "";
                    }

                    editText.addTextChangedListener(this);
                }
            }
        });
    }

    private double parseInputToDouble(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(input.replaceAll(",", ""));
    }

    /**
     * 文字列から安全に数値を抽出するメソッド
     */
    private double safeParseDouble(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 正規表現で数値部分のみ抽出
        String numStr = text.replaceAll("[^0-9.]", "");

        try {
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return 0; // パースに失敗した場合は 0 を返す
        }
    }

    private void ShowConfirmPopup(int code) {

        String ttl = "";
        String mess = "";

        switch (code) {
            case 1:
                ttl = "！保存確認";
                mess = "\n\n【計算結果】を履歴データとして保存しますか？\n" +
                        "\n" +
                        "\n\n" +
                        "\n\n\n";
                _exec_func_code = code;
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(ttl);
        builder.setMessage(mess);
        builder.setPositiveButton("はい", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (_exec_func_code == 1){
                    saveCalculationResultExec();
                }
            }
        });
        builder.setNegativeButton("いいえ", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void ShowErrorPopup(int errcode) {

        String ttl = "";
        String mess = "";

        switch (errcode) {
            case 1:
                ttl = "！入力確認";
                mess = "\n\n【労働時間】を確認してください\n" +
                        "\n" +
                        "\n\n" +
                        "\n\n\n";
                break;
            case 2:
                ttl = "！入力確認";
                mess = "\n\n【購入金額】や【給与:時給/月給/年収】を確認してください\n" +
                        "\n" +
                        "\n\n" +
                        "\n\n\n";
                break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(ttl);
        builder.setMessage(mess);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
//        builder.setNegativeButton("　後で　", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
        builder.setCancelable(false);
        builder.show();
    }

}
