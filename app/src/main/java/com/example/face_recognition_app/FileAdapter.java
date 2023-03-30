package com.example.face_recognition_app;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private ArrayList<String> faces;
    private File parentDir;
    private Context context;

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageButton editButton;
        ImageButton deleteButton;
        EditText filename;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            editButton = (ImageButton) view.findViewById(R.id.edit_button);
            deleteButton = (ImageButton) view.findViewById(R.id.delete_button);
            filename = (EditText) view.findViewById(R.id.filename);
            filename.setEnabled(false);
        }
    }

    /**
     * Initialize the dataset of the Adapter
     *
     * @param dataSet String[] containing the data to populate views to be used
     * by RecyclerView
     */
    public FileAdapter(File dir, String[] dataSet, Context _context) {
        parentDir = dir;
        faces = new ArrayList<>();
        for (int i = 0; i < dataSet.length; ++i) {
            faces.add(getFilename(dataSet[i]));
        }
        context = _context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.file_item, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(FileAdapter.ViewHolder viewHolder, int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.filename.setText(faces.get(position));

        // Add listeners for buttons
        viewHolder.deleteButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                File file = new File(parentDir, viewHolder.filename.getText().toString() + ".png");
                boolean deleted = file.delete();
                System.out.println(file.toString() + " " + deleted);
                deleteItemByPosition(position);
            }
        });

        viewHolder.editButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v)
            {
                if (viewHolder.filename.isEnabled()) {
                    viewHolder.filename.setEnabled(false);
                    File olf_file = new File(parentDir, faces.get(position) + ".png");
                    File new_file = new File(parentDir, viewHolder.filename.getText().toString() + ".png");
                    olf_file.renameTo(new_file);
                    updateItemByPosition(viewHolder.filename.getText().toString(), position);
                } else {
                    viewHolder.filename.setEnabled(true);
                }
            }
        });
    }

    // Functions for delete and adding faces
    public String getFilename(String fullFilename) {
        return fullFilename.substring(0, fullFilename.lastIndexOf('.'));
    }
    public void deleteItemByPosition(int position){
        faces.remove(position);
        notifyDataSetChanged();
    }

    public void updateItemByPosition(String newName, int position){
        faces.set(position, newName);
        notifyDataSetChanged();
    }

    public void deleteItem(String filename){
        faces.remove(filename);
        notifyDataSetChanged();
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return faces.size();
    }
}
