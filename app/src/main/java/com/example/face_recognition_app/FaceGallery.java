package com.example.face_recognition_app;

import static com.example.face_recognition_app.MainActivity.TAG;

import android.content.Context;
import android.util.Pair;

import org.checkerframework.checker.units.qual.A;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

public class FaceGallery {
    String unknownLabel = "Unknown";
    int unknownId = -1;
    float unknownDistance = 1.0F;
    ArrayList<GalleryObject> identities = null;

    float distanceThreshold = 0.7f;
    int id = 0;

    public FaceGallery(Context context, FaceRecognitionModel recModel) throws IOException, URISyntaxException {
        System.out.println(context.getAssets().toString());
        String file = Common.getResourcePath(context.getAssets().open("Korobeynikov Aleksei.jpg"), "Korobeynikov Aleksei", "jpg");
        System.out.println(file);
        ArrayList<Float> currentEmbeddings = recModel.run(BitmapUtils.getBitmap(file));

        String label = "Korobeynikov Aleksei";
        identities = new ArrayList<GalleryObject>();
        identities.add(new GalleryObject(currentEmbeddings, label, id));
        id += 1;
        //System.out.println(Common.getDirectory(context.getAssets().open("gallery"), "gallery"));
//        String[] files = context.getAssets().list("gallery");
//        Stream<String> input = Arrays.stream(files);
//        input.forEach(x -> System.out.println(x));
//            try (DirectoryStream<Path> stream = Files.newDirectoryStream(
//                    Common.getDirectory(context.getAssets().open("gallery"), "gallery"), "*.{jpg,png,jpeg}")) {
//                for (Path file : stream) {
//                    String label = file.getFileName().toString();
//                    System.out.println(label);
//                    System.out.println(file.toString());
//                    //ArrayList<Float> currentEmbeddings = recModel.run(BitmapUtils.getBitmap(file.toString()));
//                    //identities.add(new GalleryObject(currentEmbeddings, label, id));
//                    //id += 1;
//                }
//            } catch (DirectoryIteratorException ex) {
//                // I/O error encounted during the iteration, the cause is an IOException
//                throw ex.getCause();
//            }
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
                System.out.println("Embeddings from gallery : " + identities.get(k).embeddings);
                System.out.println("Embeddings from camera : " + embeddings.get(i));
                distances[i][k] = computeReidDistance(embeddings.get(i), identities.get(k).embeddings);
                System.out.println(distances[i][k]);
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
