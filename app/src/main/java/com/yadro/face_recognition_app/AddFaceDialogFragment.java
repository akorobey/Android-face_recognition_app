package com.yadro.face_recognition_app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;

public class AddFaceDialogFragment extends DialogFragment {

    ImageView face;

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        byte[] byteArr = getArguments().getByteArray("face_image");
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArr, 0, byteArr.length);
        AlertDialog.Builder builder=new AlertDialog.Builder(getActivity());
        LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = layoutInflater.inflate(R.layout.dialog,null);
        builder.setTitle("Добавление лица в галлерею лиц")
                .setIcon(android.R.drawable.ic_menu_gallery)
                .setMessage("Для закрытия окна нажмите ОК")
                .setView(layout)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null);

        face = (ImageView) layout.findViewById(R.id.faceImage);
        face.setImageBitmap(bitmap);

        return builder.create();
    }
}
