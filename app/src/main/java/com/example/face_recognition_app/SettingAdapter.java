package com.example.face_recognition_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.ViewHolder> {

    private String[] localSettings;
    private Context context;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button button;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            button = (Button) view.findViewById(R.id.button);
        }

        public Button getButton() {
            return button;
        }
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     */
    public SettingAdapter(String[] dataSet, Context _context) {
        localSettings = dataSet;
        context = _context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public SettingAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.setting_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SettingAdapter.ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getButton().setText(localSettings[position]);
        if (position == 0) {
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newWindow = new Intent(context, FacesList.class);
                    context.startActivity(newWindow);
                }
            });
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return localSettings.length;
    }
}
