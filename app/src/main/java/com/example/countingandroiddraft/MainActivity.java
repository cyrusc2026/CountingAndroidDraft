package com.example.countingandroiddraft;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private TextView output;
    private final ArrayList<String> texts = new ArrayList<>();
    private final String[] modes = {"-- Select Mode -- ", "Total Word Count", "Total Sentence Count", "Unique Word Count", "Top 5 Words", "Top 5 Alphabets","Random Paragraph","Save All"};
    private int currentMode;
    private int currentText;
    private String currentTextType;
    private final HashSet<String> commonWords = new HashSet<>();
    private TextView currentTemperatureValue;
    private int temperature;

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
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, modes);
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
            }
        });

        Spinner textSelector = findViewById(R.id.textSelector);
        ArrayAdapter<String> textAdapter = new ArrayAdapter<>(MainActivity.this,
                android.R.layout.simple_spinner_item, texts);
        textAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        textSelector.setAdapter(textAdapter);
        textSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    currentText = position;
                    currentTextType = texts.get(currentText).substring(texts.get(currentText).length() - 3);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        Button enter = findViewById(R.id.enter);
        enter.setOnClickListener(view -> {
            if (currentText > 0 && currentMode > 0) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        modeTriage();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });


        SeekBar temperatureInput = findViewById(R.id.temperatureInput);
        currentTemperatureValue = findViewById(R.id.currentTemperatureValue);

        temperatureInput.setMax(100);
        temperatureInput.setProgress(0);
        currentTemperatureValue.setText("Value: " + temperatureInput.getProgress());
        temperatureInput.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTemperatureValue.setText("Value: " + progress);
                temperature = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
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
            String[] files = assetManager.list("");
            texts.add("-- Select Text --");
            for (String file : Objects.requireNonNull(files)) {
                if (!file.equals("geoid_map") && !file.equals("images") && !file.equals("webkit") && !file.equals("commonWords.txt") && !file.equals("bibleCommonWords.txt")) {
                    texts.add(file);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @SuppressLint("SetTextI18n")
    private void uniqueWords() throws IOException {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        ArrayList<String> sortedWordsWithCount = commonWords(wordsWithCount);
        output.setText("There are "+sortedWordsWithCount.size()+" unique words in the file.");
    }

    @SuppressLint("SetTextI18n")
    private void topFiveWords() throws IOException{
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        ArrayList<String> sortedWordsWithCount = commonWords(wordsWithCount);
        output.setText("The top 5 words are:\n "
                +"1. "+sortedWordsWithCount.get(0) + " with "+wordsWithCount.get(sortedWordsWithCount.get(0)) + " occurrences\n"
                +"2. "+sortedWordsWithCount.get(1) + " with "+wordsWithCount.get(sortedWordsWithCount.get(1)) + " occurrences\n"
                +"3. "+sortedWordsWithCount.get(2) + " with "+wordsWithCount.get(sortedWordsWithCount.get(2)) + " occurrences\n"
                +"4. "+sortedWordsWithCount.get(3) + " with "+wordsWithCount.get(sortedWordsWithCount.get(3)) + " occurrences\n"
                +"5. "+sortedWordsWithCount.get(4) + " with "+wordsWithCount.get(sortedWordsWithCount.get(4)) + " occurrences");
    }
    @SuppressLint("SetTextI18n")
    private void topFiveAlphabets() throws IOException {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        HashMap<Character,Integer> alphabetsWithCount = countAlphabet(wordsWithCount);
        ArrayList<Character> alphabetSorted = alphabetsWithCount.entrySet().stream().sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
        output.setText("The top 5 alphabets are:\n "
                +"1. "+alphabetSorted.get(0) + " with "+alphabetsWithCount.get(alphabetSorted.get(0)) + " occurrences\n"
                +"2. "+alphabetSorted.get(1) + " with "+alphabetsWithCount.get(alphabetSorted.get(1)) + " occurrences\n"
                +"3. "+alphabetSorted.get(2) + " with "+alphabetsWithCount.get(alphabetSorted.get(2)) + " occurrences\n"
                +"4. "+alphabetSorted.get(3) + " with "+alphabetsWithCount.get(alphabetSorted.get(3)) + " occurrences\n"
                +"5. "+alphabetSorted.get(4) + " with "+alphabetsWithCount.get(alphabetSorted.get(4)) + " occurrences");
    }

    @SuppressLint("SetTextI18n")
    private void totalWordCount() {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        output.setText("The total word count is " + words.size() + ".");
    }

    @SuppressLint("SetTextI18n")
    private void totalSentenceCount() {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        output.setText("The total sentence count is " + sentences.length + ".");
    }

    @SuppressLint("SetTextI18n")
    private void randomParagraph() throws IOException {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        ArrayList<String> sortedWordsWithCount = commonWords(wordsWithCount);
        Random random = new Random();
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < 50; i++){
            String temp = sortedWordsWithCount.get(Math.min((int) (random.nextDouble() * ((sortedWordsWithCount.size() * temperature/100.0))), sortedWordsWithCount.size()-1));
            paragraph.append(temp).append(" ");
        }
        output.setText(paragraph);
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void writePDF() throws IOException {
        String[] sentences = new String[0];
        if (currentTextType.equals("pdf"))
            sentences = pdfToSentenceArray(this, texts.get(currentText));
        if (currentTextType.equals("txt")) sentences = txtToSentenceArray(texts.get(currentText));
        ArrayList<String> words = sentenceToWord(sentences);
        commonWordsInitialisation();
        HashMap<String, Integer> wordsWithCount = cleanWords(words);
        ArrayList<String> sortedWordsWithCount = commonWords(wordsWithCount);
        HashMap<Character,Integer> alphabetsWithCount = countAlphabet(wordsWithCount);
        ArrayList<Character> alphabetSorted = alphabetsWithCount.entrySet().stream().sorted((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue())).map(Map.Entry::getKey).collect(Collectors.toCollection(ArrayList::new));
        Random random = new Random();
        StringBuilder paragraph = new StringBuilder();
        for (int i = 0; i < 50; i++){
            String temp = sortedWordsWithCount.get(Math.min((int) (random.nextDouble() * ((sortedWordsWithCount.size() * temperature/100.0))), sortedWordsWithCount.size()-1));
            paragraph.append(temp).append(" ");
        }

        Document document = new Document();
        try {
            PdfWriter.getInstance(document, Files.newOutputStream(new File(this.getFilesDir(),"fileStatistics.pdf").toPath()));
            document.open();
            document.add(new Paragraph("File name: "+texts.get(currentText)));
            document.add(new Paragraph("The total word count is "+words.size()));
            document.add(new Paragraph("The total sentence count is "+sentences.length));
            document.add(new Paragraph("There are "+ sortedWordsWithCount.size()+" unique words"));
            document.add(new Paragraph("The top 5 words are:\n "
                    +"1. "+sortedWordsWithCount.get(0) + " with "+wordsWithCount.get(sortedWordsWithCount.get(0)) + " occurrences\n"
                    +"2. "+sortedWordsWithCount.get(1) + " with "+wordsWithCount.get(sortedWordsWithCount.get(1)) + " occurrences\n"
                    +"3. "+sortedWordsWithCount.get(2) + " with "+wordsWithCount.get(sortedWordsWithCount.get(2)) + " occurrences\n"
                    +"4. "+sortedWordsWithCount.get(3) + " with "+wordsWithCount.get(sortedWordsWithCount.get(3)) + " occurrences\n"
                    +"5. "+sortedWordsWithCount.get(4) + " with "+wordsWithCount.get(sortedWordsWithCount.get(4)) + " occurrences"));
            document.add(new Paragraph("The top 5 alphabets are:\n "
                    +"1. "+alphabetSorted.get(0) + " with "+alphabetsWithCount.get(alphabetSorted.get(0)) + " occurrences\n"
                    +"2. "+alphabetSorted.get(1) + " with "+alphabetsWithCount.get(alphabetSorted.get(1)) + " occurrences\n"
                    +"3. "+alphabetSorted.get(2) + " with "+alphabetsWithCount.get(alphabetSorted.get(2)) + " occurrences\n"
                    +"4. "+alphabetSorted.get(3) + " with "+alphabetsWithCount.get(alphabetSorted.get(3)) + " occurrences\n"
                    +"5. "+alphabetSorted.get(4) + " with "+alphabetsWithCount.get(alphabetSorted.get(4)) + " occurrences"));
            document.add(new Paragraph("This is a randomly generated paragraph according to the temperature parameter: "+paragraph));
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            Log.e("YourTag", "Error occurred while reading the file", e);
        } finally {
            document.close();
        }
        output.setText("Your file has successfully been created");
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

        return sb.toString().trim().split("[.!?]\\s*");
    }

    @SuppressLint("SdCardPath")
    private String[] pdfToSentenceArray(Context context, String filename) {
        StringBuilder sb = new StringBuilder();
        AssetManager assetManager = context.getAssets();
        PdfReader reader = null;

        try (InputStream inputStream = assetManager.open(filename)) {
            reader = new PdfReader(inputStream);

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(reader, i);
                if (pageText != null && !pageText.trim().isEmpty()) {
                    sb.append(pageText);
                }
            }
        } catch (FileNotFoundException e) {
            Log.e("PDFReader", "File not found: " + filename, e);
            return new String[0];
        } catch (IOException e) {
            Log.e("PDFReader", "Error reading PDF: " + e.getMessage(), e);
            return new String[0];
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        ArrayList<String> splitSentences = new ArrayList<>();
        Scanner scanner = new Scanner(sb.toString());
        scanner.useDelimiter("[.!?]");
        while (scanner.hasNext()) {
            String word = scanner.next();
            splitSentences.add(word);
        }
        return splitSentences.toArray(new String[0]);
    }



    private ArrayList<String> sentenceToWord(String[] sentences){
       ArrayList<String> wordsList = new ArrayList<>();

        for (String sentence : sentences) {
            String[] words = sentence.split("[^a-zA-Z'’]+");
            for (String word : words) {
                if (!word.isEmpty()&&!word.equals("'")&&!word.equals("’")) wordsList.add(word);
            }
        }

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
            //noinspection DataFlowIssue
            wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
        }
        wordCount.keySet().removeAll(commonWords);
        return wordCount;

    }

    public ArrayList<String> commonWords(HashMap<String, Integer> words){
        ArrayList<String> sortedWords = new ArrayList<>(words.keySet());
        sortedWords.sort((a, b) -> Objects.requireNonNull(words.get(b)).compareTo(Objects.requireNonNull(words.get(a))));
        return sortedWords;
    }

    public HashMap<Character, Integer> countAlphabet(HashMap<String, Integer> wordsWithCount){
        HashMap<Character, Integer> alphabetCount = new HashMap<>();
        for (Map.Entry<String, Integer> entry : wordsWithCount.entrySet()) {
            String word = entry.getKey();
            int count = entry.getValue();

            for (char c : word.toCharArray()) {
                c = Character.toLowerCase(c);
                if (Character.isLetter(c)) {
                    //noinspection DataFlowIssue
                    alphabetCount.put(c, alphabetCount.getOrDefault(c, 0) + count);
                }
            }
        }
        return alphabetCount;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void modeTriage() throws IOException {
        if (currentMode==1) totalWordCount();
        if (currentMode==2) totalSentenceCount();
        if (currentMode==3) uniqueWords();
        if (currentMode==4) topFiveWords();
        if (currentMode==5) topFiveAlphabets();
        if (currentMode==6) randomParagraph();
        if (currentMode==7) writePDF();
    }


}