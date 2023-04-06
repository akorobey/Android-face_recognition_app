package com.yadro.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.yadro.face_recognition_app.R;

import org.w3c.dom.Text;

public class SettingAdapter extends RecyclerView.Adapter<SettingAdapter.ViewHolder> {

    private String[] localSettings;
    private Context context;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button button;
        private final TextView current;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            button = (Button) view.findViewById(R.id.button);
            current = (TextView) view.findViewById(R.id.current_state);
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
        SharedPreferences settings = context.getSharedPreferences("Settings", context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = settings.edit();
        if (position == 0) {
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newWindow = new Intent(context, FacesList.class);
                    context.startActivity(newWindow);
                }
            });
        }
        if (position == 1) {
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent newWindow = new Intent(context, Monitors.class);
                    context.startActivity(newWindow);
                }
            });
        }
        if (position == 2) {
            viewHolder.current.setText(settings.getString("Device", "Not defined"));
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.inflate(R.menu.device_menu);

                    popupMenu
                            .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.id.cpu:
                                            viewHolder.current.setText("CPU");
                                            prefEditor.putString("Device", "CPU");
                                            prefEditor.apply();
                                            return true;
                                        case R.id.gpu:
                                            viewHolder.current.setText("GPU");
                                            prefEditor.putString("Device", "GPU");
                                            prefEditor.apply();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                    popupMenu.show();
                }
            });
        }
        if (position == 3) {
            viewHolder.current.setText(String.valueOf(settings.getInt("Threads", 0)));
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.inflate(R.menu.threads_menu);

                    popupMenu
                            .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.id._1:
                                            viewHolder.current.setText("1");
                                            prefEditor.putInt("Threads", 1);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._2:
                                            viewHolder.current.setText("2");
                                            prefEditor.putInt("Threads", 2);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._3:
                                            viewHolder.current.setText("3");
                                            prefEditor.putInt("Threads", 3);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._4:
                                            viewHolder.current.setText("4");
                                            prefEditor.putInt("Threads", 4);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._5:
                                            viewHolder.current.setText("5");
                                            prefEditor.putInt("Threads", 5);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._6:
                                            viewHolder.current.setText("6");
                                            prefEditor.putInt("Threads", 6);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._7:
                                            viewHolder.current.setText("7");
                                            prefEditor.putInt("Threads", 7);
                                            prefEditor.apply();
                                            return true;
                                        case R.id._8:
                                            viewHolder.current.setText("8");
                                            prefEditor.putInt("Threads", 8);
                                            prefEditor.apply();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                    popupMenu.show();
                }
            });
        }
        if (position == 4) {
            viewHolder.current.setText(settings.getString("Algorithm", "Not defined"));
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.inflate(R.menu.algorithm_menu);

                    popupMenu
                            .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.id.kit:
                                            viewHolder.current.setText("MLKit");
                                            prefEditor.putString("Algorithm", "MLKit");
                                            prefEditor.apply();
                                            return true;
                                        case R.id.yadro:
                                            viewHolder.current.setText("YADRO");
                                            prefEditor.putString("Algorithm", "YADRO");
                                            prefEditor.apply();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                    popupMenu.show();
                }
            });
        }
        if (position == 5) {
            if (settings.getBoolean("ShowFPS", true)) {
                viewHolder.current.setText("ON");
            } else {
                viewHolder.current.setText("OFF");
            }
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    prefEditor.putBoolean("ShowFPS", !settings.getBoolean("ShowFPS", true));
                    prefEditor.apply();
                    viewHolder.current.setText(viewHolder.current.getText().toString().equals("OFF") ? "ON" : "OFF");
                }
            });
        }
        if (position == 6) {
            viewHolder.current.setText(settings.getString("Resolution", "Not defined"));
            viewHolder.getButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(context, v);
                    popupMenu.inflate(R.menu.resolution_menu);

                    popupMenu
                            .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    switch (item.getItemId()) {
                                        case R.id.low:
                                            viewHolder.current.setText("640x480");
                                            prefEditor.putString("Resolution", "640x480");
                                            prefEditor.apply();
                                            return true;
                                        case R.id.middle:
                                            viewHolder.current.setText("1280x720");
                                            prefEditor.putString("Resolution", "1280x720");
                                            prefEditor.apply();
                                            return true;
                                        default:
                                            return false;
                                    }
                                }
                            });
                    popupMenu.show();
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
