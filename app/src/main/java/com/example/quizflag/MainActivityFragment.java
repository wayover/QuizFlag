package com.example.quizflag;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.AnimatedImageDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivityFragment extends Fragment {

    /*Znacznik używany przy zapisie błędów wdzienniku Log */
    private static final String TAG = "QuizWithFlags Activity";

    /*Liczba flag biorących udział w quizie */
    private static final int FLAG_IN_QUIZ =10;


    /*Nazwy pików z obrazami flag */
    private List<String> fileNameList;

    /*Lita plików z obrazami flag biorących udział w bieżącym quizie */
    private List<String> quizCounteriesList;

    /*Wybranie obszary biorące udział w quizie */
    private Set<String> regionSet;

    /*Poprawna nazwa kraju przypisana do bieżącej flagi*/
    private String correctAnswer;

    /*Całkowita liczba odpowiedzi */
    private int totalGuesses;

    /*Liczba poprawnych odpoweiedzi */
    private int correctAnswers;

    /* Liczba wierszy przycisków odpoweidzi wyświetlanych na ekranie */
    private int guessRows;

    /*Obiekt służąct do losowanie */
    private SecureRandom random;

    /*Obiekt używany podczas opóźnienia procesu ładowania kolejnej flagi w quizie */
    private Handler handler;

    /*Animacja błędnej odpowiedzi */
    private Animation shakeAnimation;

    /* Główny rozkład aplikacji */
    private LinearLayout quizLinearLayout;

    /* Widok wyświetlający numer bieżącego pytania quizu */
    private TextView questionNumberTextView;

    /* Widok wyświetlający bieżącą flagę */
    private ImageView flagImageView;

    /* Tablica zawierająca wiersze przycisków odpowiedzi */
    private LinearLayout[] guessLinearLayouts;

    /* Widok wyświetlający poprawną odpowiedź w quizie */
    private TextView answerTextView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        /*Zainicjowanie graficznego interfejsu użytkownika dla fragmentu */
        super.onCreateView(inflater, container, savedInstanceState);

        /*Pobranie rozkładu dla fragmentu */
        View view = inflater.inflate(R.layout.fragment_main,container,false);

        /*Inicjalizacja wybranych pól */
        fileNameList = new ArrayList<>();
        quizCounteriesList = new ArrayList<>();
        random=new SecureRandom();
        handler=new Handler();

        /*Inicjalizacja animacji */
        shakeAnimation= AnimationUtils.loadAnimation(getActivity(),R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);
        /*Inicjalizacja komonmentów graficznego interfejsu użytkownika */
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayaut);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayaut);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayaut);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayaut);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);


        /*Konfiguracja nasłuchiwania zdarzeń w przyciskach odpowiedzi */
        for(LinearLayout row: guessLinearLayouts){
            for(int column =0;column<row.getChildCount();column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(quessButtonListener);
            }
        }

        /*Wyświetlenie formatowanego tekstu w widoku TextView */
        questionNumberTextView.setText(getString(R.string.question,1,FLAG_IN_QUIZ));

        /*Zwróć widok fragmentu do wyświetlenia */
        return view;

    }


    public void updateGuessRows(SharedPreferences sharedPreferences){
        /*Pobranie informacji o ilości przycisków odpoweidzi do wyświetlenia */
        String choices= sharedPreferences.getString(MainActivity.CHOICES,null);

        /*Liczba wierszy z przyciskami odpoweidzi do wyświetlenia */
        guessRows= Integer.parseInt(choices)/2;

        /*Ukrycie wszystkich wierszy z przyciskami */
        for(LinearLayout layout:guessLinearLayouts){
            layout.setVisibility(View.GONE);
        }

        /*Wyświetlenie określonej liczby wierszy z przyciskami odpoweidzi */
        for(int row = 0;row<guessRows;row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }

    }


    public void updateRegions(SharedPreferences sharedPreferences){
        /*Pobranie informacji na temat wyranych przez użytkownika obszarów */
        regionSet=sharedPreferences.getStringSet(MainActivity.REGIONS,null);
    }


    public void resetQuiz() {

        /* Uzyskaj dostęp do folderu assets */
        AssetManager assets = getActivity().getAssets();

        /* Wyczyść listę z nazwami flag */
        fileNameList.clear();

        /* Pobierz nazwy plików obrazów flag z wybranych przez użytkownika obszarów */
        try {
            /* Pętla przechodząca przez każdy obszar - czyli przez każdy folder w folderze assets */
            for (String region : regionSet) {

                /* Pobranie nazw wszystkich plików znajdujących się w folderze danego obszaru */
                String[] paths = assets.list(region);

                /* Usunięcie z nazw plików ich rozszerzenia formatu */
                for (String path : paths) {
                    fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Bład podczas ładowania plików z obrazami flag", ex);
        }


        /*Zresetowanie liczby poprawnych i wszystkich udzielonych odpowiedzi */
        correctAnswers=0;
        totalGuesses=0;

        /*Wyczyszzenie liczby krajów */
        quizCounteriesList.clear();

        /*Inicjalizacja zmioennych qykorzyntywanych przy losowaniu flag */
        int flagCounter=1;
        int numberOfFlags=fileNameList.size();

        /*Losowanie flag */
        while(flagCounter <= FLAG_IN_QUIZ){

            /* Wybierz losową wartość z zakresu od "0" do "liczby flag" biorących udział w quizie */
            int randomIndex = random.nextInt(numberOfFlags);

            /* Pobierz nazwę pliku o wylosowanym indeksie */
            String fileName = fileNameList.get(randomIndex);

            /* Jeżeli plik o tej nazwie nie został jeszcze wylosowany, to dodaj go do listy wybranych krajów */
            if(!quizCounteriesList.contains(fileName)){
                quizCounteriesList.add(fileName);
                ++flagCounter;
            }
        }


        /*Załaduj flagę */
        loadNextFlag();

    }

}
