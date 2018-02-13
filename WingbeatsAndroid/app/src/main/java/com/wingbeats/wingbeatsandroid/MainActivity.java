package com.wingbeats.wingbeatsandroid;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class MainActivity extends AppCompatActivity {

    int SAMPLE_RATE = 8000;
    int SAMPLE_DURATION_MS = 625;
    int SAMPLE_LENGTH = (int) (SAMPLE_RATE * SAMPLE_DURATION_MS / 1000);
    String INPUT_DATA_NAME = "input_1:0";
    String OUTPUT_SCORES_NAME = "output_node:0";
    String MODEL_FILENAME = "file:///android_asset/basic_cnn_1d.pb";

    float[] float_buffer = new float[SAMPLE_LENGTH];

    String[] labels = new String[] {"Ae. aegypti", "Ae. albopictus", "An. gambiae", "An. arabiensis", "C. pipiens", "C. quinquefasciatus"};
    Integer[] labels_indices = new Integer[]{0, 1, 2, 3, 4, 5};

    String[] output_scores_names = new String[] {OUTPUT_SCORES_NAME};
    float[] output_scores = new float[labels.length];

    String test_corpus_path = "test_corpus";
    String user_path = "Wingbeats_user";

    TensorFlowInferenceInterface tensorflow_inference;
    MediaPlayer player = new MediaPlayer();

    TextView text_view;

    Button switch_button;

    int mode = 0;

    List<String> final_list;

    String final_wav_file_path;

    String final_monitor = "";

    DecimalFormat df = new DecimalFormat("0.00");
    DecimalFormatSymbols dfs = new DecimalFormatSymbols();

    int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    final int target_API = 26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tensorflow_inference = new TensorFlowInferenceInterface(getAssets(), MODEL_FILENAME);

        dfs.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(dfs);

        text_view = findViewById(R.id.textView);

        switch_button = findViewById(R.id.button);
        switch_button.setEnabled(false);
        switch_button.setVisibility(View.GONE);

        File test_corpus_path_dir = new File(getFilesDir() + "/" + test_corpus_path);
        if(test_corpus_path_dir.isDirectory()) {
            Log.i(">>>>>", test_corpus_path + " already exists");
        }
        else{
            Log.i(">>>>>", "creating " + test_corpus_path + " path and copying files from assets");
            copy_assets(test_corpus_path);
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
            }
            else{
                //GRANTED
                init_app();
            }
        }
        else{
            //GRANTED NO M
            init_app();
        }
    }

    View.OnClickListener connectListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mode == 0){
                mode = 1;
            }
            else if (mode == 1){
                mode = 0;
            }
            start_app(mode);
        }
    };

    private void init_app(){
        switch_button.setEnabled(true);
        switch_button.setVisibility(View.VISIBLE);
        switch_button.setOnClickListener(connectListener);
        text_view.setText("Choose a sample to begin...");
        make_user_path();
        start_app(0);
    }

    private void start_app(int mode) {
        if (mode == 0) {
            String[] assets_list;
            try {
                assets_list = getAssets().list(test_corpus_path);
                if (assets_list.length > 0) {
                    final_list = Arrays.asList(assets_list);
                    Collections.shuffle(final_list);
                    Log.i(">>>>>", "found " + String.valueOf(final_list.size()) + " files in assets");
                }
            } catch (IOException e) {
                Log.i(">>>>>", "no files in assets");
            }
        }
        else if (mode == 1){
            File[] user_files;
            File user_path_dir = new File(Environment.getExternalStorageDirectory() + "/" + user_path);
            user_files = user_path_dir.listFiles();

            if (user_files != null) {
                String[] user_list = new String[user_files.length];

                for (int i=0; i<user_list.length; i++){
                    user_list[i] = user_files[i].getName();
                }

                final_list = Arrays.asList(user_list);
                Collections.shuffle(final_list);
                Log.i(">>>>>", "found " + String.valueOf(final_list.size()) + " files in user path");
            }
            else{
                Log.i(">>>>>", "no files in user path");
            }
        }
        ListView mainListView = findViewById(R.id.list);
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(this, R.layout.simplerow);

        for (int i = 0; i < final_list.size(); i++) {
            listAdapter.insert(final_list.get(i), 0);
        }

        mainListView.setAdapter(listAdapter);
        registerForContextMenu(mainListView);

        if (mode == 0){
            switch_button.setText("Switch to your samples");
        }
        else if (mode == 1){
            switch_button.setText("Switch to pre-installed samples");
        }

        mainListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3)
            {
                Animation animation = new AlphaAnimation(0.3f, 1.0f);
                animation.setDuration(625);
                v.startAnimation(animation);

                String value = (String)adapter.getItemAtPosition(position);

                inference(value);
            }
        });
    }

    private void inference(String value){
        long start_time = System.currentTimeMillis();

        String[] splited = value.split("\\s+");

        final_monitor = "";

        if (mode == 0) {
            Log.i(">>>>>", "Classifying class: " + splited[0] + " " + splited[1] + " " + splited[2]);
            final_monitor += "Classifying class: " + splited[0] + " " + splited[1] + "\n" + splited[2];
        }
        else if (mode == 1){
            Log.i(">>>>>", "Classifying class: Unknown" + " " + value);
            final_monitor += "Classifying class: Unknown" + "\n" + value;
        }
        text_view.setText(final_monitor);

        player.release();
        player = new MediaPlayer();

        if (mode == 0) {
            try {
                AssetFileDescriptor afd = getAssets().openFd(test_corpus_path + "/" + value);
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                player.prepare();
            } catch (Exception e) {
                Log.e(">>>>>", "no such file");
            }
        }
        else if (mode == 1){
            try {
                player.setDataSource(Environment.getExternalStorageDirectory() + "/" + user_path + "/" + value);
                player.prepare();
            } catch (Exception e) {
                Log.e(">>>>>", "no such file");
            }
        }

        player.start();

        if (mode == 0) {
            final_wav_file_path = getFilesDir() + "/" + test_corpus_path + "/" + value;
        }
        else if (mode == 1){
            final_wav_file_path = Environment.getExternalStorageDirectory() + "/" + user_path + "/" + value;
        }

        try
        {
            // Open the wav file
            WavFile wavFile = WavFile.openWavFile(new File(final_wav_file_path));

            // Display information about the wav file
            wavFile.display();

            // Get the number of audio channels in the wav file
            int numChannels = wavFile.getNumChannels();

            // Create a buffer of 100 frames
            double[] buffer = new double[100 * numChannels];

            int framesRead;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            int index = 0;

            do
            {
                // Read frames into buffer
                framesRead = wavFile.readFrames(buffer, 100);

                // Loop through frames and look for minimum and maximum value
                for (int s=0 ; s<framesRead * numChannels ; s++)
                {
                    if (buffer[s] > max) max = buffer[s];
                    if (buffer[s] < min) min = buffer[s];
                    float_buffer[index] = (float)buffer[s];
                    index++;
                }
            }
            while (framesRead != 0);

            // Close the wavFile
            wavFile.close();

            // Output the minimum and maximum value
            System.out.printf("Min: %f, Max: %f\n", min, max);

            // Divide by max
            for (int i = 0; i < float_buffer.length; i++) {
                float_buffer[i] = float_buffer[i] / (float)max;
            }
        }
        catch (Exception e)
        {
            System.err.println(e);
        }

        // Tensorflow inference
        tensorflow_inference.feed(INPUT_DATA_NAME, float_buffer, 1, SAMPLE_LENGTH, 1);
        tensorflow_inference.run(output_scores_names);
        tensorflow_inference.fetch(OUTPUT_SCORES_NAME, output_scores);

        Arrays.sort(labels_indices, new Comparator<Integer>() {
            @Override public int compare(final Integer o1, final Integer o2) {
                return Float.compare(output_scores[o1], output_scores[o2]);
            }
        });

        final_monitor = "";

        if (mode == 0) {
            Log.i(">>>>>", "Results for class: " + splited[0] + " " + splited[1] + " " + splited[2]);
            final_monitor += "Results for class: " + splited[0] + " " + splited[1] + "\n" + splited[2] + "\n\n";
        }
        else if (mode == 1){
            Log.i(">>>>>", "Results for class: Unknown" + " " + value);
            final_monitor += "Results for class: Unknown" + "\n" + value + "\n\n";
        }

        int ii = 0;
        for (int i = output_scores.length - 1; i >= 0; i--) {
            ii ++;
            Log.i(">>>>>", String.valueOf(ii + ") " + df.format(output_scores[labels_indices[i]])) + " " + String.valueOf(labels[labels_indices[i]]));
            final_monitor += String.valueOf(ii + ")   " + df.format(output_scores[labels_indices[i]])) + "   " + String.valueOf(labels[labels_indices[i]] + "\n");
        }

        long end_time = System.currentTimeMillis();

        Log.i(">>>>>", "Time (s): " + String.valueOf((end_time - start_time) / 1000.0f));
        final_monitor += String.valueOf("\nTime (s): " + String.valueOf((end_time - start_time) / 1000.0f));

        text_view.setText(final_monitor);
    }

    private void copy_assets(String path) {
        AssetManager manager = getAssets();
        try {
            String[] contents = manager.list(path);

            if (contents == null || contents.length == 0)
                throw new IOException();

            File dir = new File(getFilesDir() + "/" + path);
            dir.mkdirs();

            for (String entry : contents) {
                copy_assets(path + "/" + entry);
            }
        } catch (IOException e) {
            copy_assets_file(path);
        }
    }

    private void copy_assets_file(String path) {
        File file = new File(getFilesDir() + "/" + path);
        try {
            InputStream in = getAssets().open(path);
            OutputStream out = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read = in.read(buffer);
            while (read != -1) {
                out.write(buffer, 0, read);
                read = in.read(buffer);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Log.e(">>>>>", String.valueOf(e));
        }
    }

    private void make_user_path(){
        File user_path_dir = new File(Environment.getExternalStorageDirectory() + "/" + user_path);
        if (user_path_dir.isDirectory()) {
            Log.i(">>>>>", user_path + " already exists");
        } else {
            Log.i(">>>>>", "creating " + user_path + " path");
            user_path_dir.mkdirs();
        }
    }

    @TargetApi(target_API)
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            //GRANTED RESULT
            init_app();
        }
        else {
            if (requestCode == MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE) {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = shouldShowRequestPermissionRationale(permission);
                        if (!showRationale) {
                            //NEVER
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_HOME);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            System.exit(0);

                        } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                            //DENY
                            Intent intent = new Intent(Intent.ACTION_MAIN);
                            intent.addCategory(Intent.CATEGORY_HOME);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            System.exit(0);
                        }
                    }
                }
            }
        }
    }
}
