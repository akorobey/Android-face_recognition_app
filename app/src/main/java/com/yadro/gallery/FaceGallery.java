package com.yadro.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Pair;
import android.widget.Toast;

import com.yadro.tfmodels.FaceRecognitionModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class FaceGallery {
    String unknownLabel = "Unknown";
    public int unknownId = -1;
    float unknownDistance = 1.0F;
    ArrayList<GalleryObject> identities = null;

    float distanceThreshold = 0.5f;
    int id = 0;

    public FaceGallery(Context context, FaceRecognitionModel recModel) throws IOException, URISyntaxException {
        FileInputStream fin = null;
        String [] faces = context.fileList();
        identities = new ArrayList<GalleryObject>();
        for (int i = 0; i < faces.length; ++i) {
            try {
                fin = context.openFileInput(faces[i]);
                byte[] bytes = new byte[fin.available()];
                fin.read(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                String label = faces[i].substring(0, faces[i].lastIndexOf('.'));
                ArrayList<Float> currentEmbeddings = recModel.run(bitmap);
                identities.add(new GalleryObject(currentEmbeddings, label, id));
                id += 1;
            }
            catch(IOException ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }
            finally{
                try{
                    if(fin!=null)
                        fin.close();
                }
                catch(IOException ex){
                    Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public static float scalarProduct(ArrayList<Float> vec1, ArrayList<Float> vec2) {
        assert(vec1.size() == vec2.size());
        float result = 0;
        for (int ind = 0; ind < vec1.size(); ++ind) {
            result += vec1.get(ind) * vec2.get(ind);
        }
        return result;
    }

    public static float computeReidDistance(ArrayList<Float> descr1, ArrayList<Float> descr2) {
        float xy = scalarProduct(descr1, descr2);
        float xx = scalarProduct(descr1, descr1);
        float yy = scalarProduct(descr2, descr2);
        double norm = Math.sqrt(xx * yy) + 1e-6f;
        return (float) (1.0f - xy / norm);
    }

    public ArrayList<Pair<Integer, Float>> getIDsByEmbeddings(ArrayList<ArrayList<Float>> embeddings) {
        ArrayList<Pair<Integer, Float>> matches = new ArrayList<Pair<Integer, Float>>();
        if (embeddings.isEmpty() || identities.isEmpty()) {
            for (int i = 0; i < embeddings.size(); ++i) {
                matches.add(new Pair<>(unknownId, unknownDistance));
            }
            return matches;
        }

        Float[][] distances = new Float[embeddings.size()][identities.size()];

        for (int i = 0; i < embeddings.size(); i++) {
            for (int k = 0; k < identities.size(); ++k) {
                distances[i][k] = computeReidDistance(embeddings.get(i), identities.get(k).embeddings);
            }
        }
        ArrayList<Integer> busy = new ArrayList<Integer>();

        for (int i = 0; i < embeddings.size(); ++i) {
            float min = distances[i][0];
            int index_min = 0;
            for (int k = 1; k < identities.size(); ++k) {
                if (distances[i][k] < min) {
                    min = distances[i][k];
                    index_min = k;
                }
            }
            if (busy.contains(index_min) || min > distanceThreshold) {
                matches.add(new Pair<>(unknownId, unknownDistance));
            } else {
                matches.add(new Pair<>(index_min, distances[i][index_min]));
                busy.add(index_min);
            }
        }
        
        return matches;
    }

    public int size() {
        return identities.size();
    }

    public String getLabelByID(int id) {
        if (id >= 0 && id < identities.size()) {
            return identities.get(id).label;
        }
        return unknownLabel;
    }

    public boolean labelExists(String label) {
        for (int i = 0; i < identities.size(); ++i) {
            if (identities.get(i).label.equals(label)) {
                return true;
            }
        }
        return false;
    }
}
