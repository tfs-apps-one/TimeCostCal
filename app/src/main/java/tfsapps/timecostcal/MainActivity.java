package tfsapps.timecostcal;

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

    // Constants
    private static final String PREFS_NAME = "TimeCostPrefs";
    private static final String HISTORY_KEY = "calculation_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Button settingsButton = findViewById(R.id.settingsButton);
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

    }

    private void saveCalculationResult() {
        String resultText = resultView.getText().toString();
        if (resultText.isEmpty() || !resultText.contains("購入金額")) {
            Toast.makeText(this, "有効な計算結果がありません。", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 購入金額と労働時間を抽出
            String[] lines = resultText.split("\n");
            double purchaseAmount = 0;
            double laborTime = 0;

            for (String line : lines) {
                if (line.contains("購入金額")) {
                    purchaseAmount = parseInputToDouble(line.replaceAll("[^\\d.]", ""));
                } else if (line.contains("労働時間")) {
                    laborTime = Double.parseDouble(line.replaceAll("[^\\d.]", ""));
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
            if (historyArray.length() >= 10) {
                historyArray.remove(0);
            }
            // 最大100件を保持
//            if (historyArray.length() >= 100) {
//                historyArray.remove(0);
//            }

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
                    return; //計算できない
                }
                hourlyWage = monthlySalary / monthlyWorkHours;
                hourlyWageText = new DecimalFormat("#,##0").format(hourlyWage);
            } else if (selectedSalaryType == R.id.annualSalaryOption) {
                double annualSalary = parseInputToDouble(annualSalaryInput.getText().toString());
                double annualWorkHours = parseInputToDouble(annualWorkHoursInput.getText().toString());
                if (annualWorkHours <= 0){
                    return; //計算できない
                }
                hourlyWage = annualSalary / annualWorkHours;
                hourlyWageText = new DecimalFormat("#,##0").format(hourlyWage);
            }

            double timeCost = purchaseAmount / hourlyWage;

            /* 計算できない場合、実施しない */
            if (purchaseAmount <= 0 || hourlyWage <= 0){
                return;
            }

            DecimalFormat formatter = new DecimalFormat("#,##0");
            String formattedAmount = formatter.format(purchaseAmount);
            String result = String.format(
                    "購入金額:\n　▶︎ %s円\n時給:\n　▶︎ %s円\n購入に必要な労働時間:\n　▶︎ %.2f時間",
                    formattedAmount, hourlyWageText, timeCost);
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
}
