package com.example.countingandroiddraft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private TextView output;
    private final ArrayList<String> texts = new ArrayList<>();
    private final String[] modes = {"-- Select Mode -- ", "Total Word Count", "Total Sentence Count","List of Unique Words", "Top 5 words", "Random Paragraph"};
    private int currentMode;
    private int currentText;
    private String currentTextType;
    private final HashSet<String> commonWords = new HashSet<>();
    private final HashMap<String, Integer> wordCount = new HashMap<>();
    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        output = findViewById(R.id.output);
        output.setText("Please choose an option");

        textsSpinnerSetUp(this);
        Spinner modeSelector = findViewById(R.id.modeSelector);
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item,modes);
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSelector.setAdapter(modeAdapter);
        modeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentMode = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle when nothing is selected for Spinner 1
            }
        });

        Spinner textSelector = findViewById(R.id.textSelector);
        ArrayAdapter<String> textAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_spinner_item,texts);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSelector.setAdapter(textAdapter);
        textSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentText = position;
                    currentTextType = texts.get(currentText).substring(texts.get(currentText).length()-3);
                    output.setText((CharSequence) texts);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle when nothing is selected for Spinner 1
            }
        });



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

    }

    private void textsSpinnerSetUp(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            // List all files in the assets folder
            String[] files = assetManager.list("");
            //gets all the files' names
            texts.add("-- Select Text --");
            //default option
            for (String file : Objects.requireNonNull(files)) {
                //requireNonNull checks to make sure that the files are null
                if (!file.equals("geoid_map") && !file.equals("images") && !file.equals("webkit") &&!file.equals("commonWords.txt")&&!file.equals("bibleCommonWords.txt")) {
                    //removes irrelevant files in assets file
                    texts.add(file);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void listOfUniqueWords() throws IOException {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf")) sentences = pdfToSentenceArray(this, String.valueOf(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(String.valueOf(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        ArrayList<String> sortedWordsWithCount = commonWords(wordsWithCount);
    }

    private String[] txtToSentenceArray(String filename) {
        AssetManager asset = this.getAssets();
        StringBuilder sb = new StringBuilder();

        try (InputStream inputStream = asset.open(filename);
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line.toLowerCase()).append(" ");
            }

        } catch (IOException e) {
            Log.e("YourTag", "Error occurred while reading the file", e);
        }

// Use StringBuilder to change the file type and split
        return sb.toString().split("\\s+");
    }

    private String[] pdfToSentenceArray(Context context,String filename){
        AssetManager asset = this.getAssets();
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = context.getAssets().open(filename);
             PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            sb.append(text);
        } catch (IOException e) {
            Log.e("PDFUtils", "Error reading PDF file", e);
        }

        return sb.toString().split("\\s+");
    }

    private ArrayList<String> sentenceToWord(String[] sentences){
       ArrayList<String> wordsList = new ArrayList<>();

        // Split each sentence into words
        for (String sentence : sentences) {
            String[] words = sentence.split("[^'’a-zA-Z]+");
            for (String word : words) {
                wordsList.add(word);
            }
        }

        // Convert List to Array
        return wordsList;
    }

    private void commonWordsInitialisation() throws IOException {
        AssetManager asset = this.getAssets();
            InputStream inputStreamC = asset.open("commonWords.txt");
            InputStreamReader inputStreamReaderC = new InputStreamReader(inputStreamC);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReaderC);
        String commonWord = "";
        while (true) {
            try {
                if ((commonWord = bufferedReader.readLine()) == null) break;
            } catch (IOException e) {
                Log.e("YourTag", "Error occurred while reading the file", e);
            }
            commonWords.add(commonWord.toLowerCase());
        }
        try {
            bufferedReader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HashMap<String, Integer> cleanWords(ArrayList<String> cleanWords){
        HashMap<String, Integer> wordCount = new HashMap<>();
        for (String word : cleanWords) {
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        wordCount.keySet().removeAll( commonWords);
        return wordCount;

    }

    public ArrayList<String> commonWords(HashMap<String, Integer> words){
        ArrayList<String> sortedWords = new ArrayList<>(words.keySet());
        sortedWords.sort((a, b) -> Objects.requireNonNull(words.get(b)).compareTo(Objects.requireNonNull(words.get(a))));
        return sortedWords;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void modeTriage(){

    }
}