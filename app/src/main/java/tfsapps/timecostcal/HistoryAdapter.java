package tfsapps.timecostcal;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.ArrayAdapter;
import java.util.List;

public class HistoryAdapter extends ArrayAdapter<String> {

    public HistoryAdapter(@NonNull Context context, @NonNull List<String> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_history, parent, false);
        }

        TextView historyTextView = convertView.findViewById(R.id.historyTextView);
        historyTextView.setText(getItem(position));

        return convertView;
    }
}
