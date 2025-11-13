package adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

public class HintAdapter extends ArrayAdapter<String> {

    public HintAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
    }

    @Override
    public boolean isEnabled(int position) {
        // Desabilita o primeiro item (o "hint")
        return position != 0;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        TextView tv = (TextView) view;
        if (position == 0) {
            // Define a cor do texto do hint como cinza
            tv.setTextColor(Color.GRAY);
        } else {
            // Define a cor do texto dos itens normais como preto
            tv.setTextColor(Color.BLACK);
        }
        return view;
    }
}
