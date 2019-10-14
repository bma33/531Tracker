package com.a531tracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.a531tracker.Database.DatabaseHelper;
import com.a531tracker.LiftBuilders.AsManyRepsAsPossible;
import com.a531tracker.LiftBuilders.CompoundLifts;

import java.util.ArrayList;
import java.util.List;

public class HomeScreen extends AppCompatActivity {
    static final int TRAINING_MAX_CODE = 42534;

    public static String[] compoundLifts = new String[]{"Bench", "Overhand Press", "Squat", "Deadlift"};
    private List<CompoundLifts> liftsArray = new ArrayList<>();
    private List<Integer> liftValues = new ArrayList<>();

    private DatabaseHelper db;
    private Context mContext;
    private Integer cycleValue;
    private String liftMissingAMRAP;

    private Button accessoryButton;
    private Button benchNumbers;
    private Button deadliftNumbers;
    private Button pressNumbers;
    private Button squatNumbers;
    private Button tmUpdate;
    private Button trainingMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        mContext = this;

        setUpButtons();
        startCycle();
        checkForLiftValues();
    }

    // TODO Create listener/Change activity for set total maxes so lifts are reset
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Got_result", "OK!");
        if(requestCode == 1 && resultCode == RESULT_OK){
            Log.d("Got_result", "OK doing work!");
            String i = data.getStringExtra("Submitted");
            Log.d("Submitted value", i);
            checkForLiftValues();
        }
    }


    private void checkForLiftValues() {
        try{
            addLiftsToArray();
        } catch (NullPointerException e){
            Log.d("Compound_lifts", "No lifts found, force user to make lifts");
        } catch (Exception e){
            alertBuilder(getResources().getString(R.string.no_lifts_found), getResources().getString(R.string.no_lifts_message), "", false, true);
        }
    }


    public void setUpButtons(){
        accessoryButton = findViewById(R.id.addAccessoriesButton);
        benchNumbers = findViewById(R.id.bench_numbers);
        deadliftNumbers = findViewById(R.id.deadlift_numbers);
        pressNumbers = findViewById(R.id.press_numbers);
        squatNumbers = findViewById(R.id.squat_numbers);
        tmUpdate = findViewById(R.id.increase_training_max);
        trainingMax = findViewById(R.id.setTrainingMaxButton);

        benchButton();
        deadliftButton();
        pressButton();
        squatButton();
        updateTrainingMax();
        trainingMaxButton();
        //accessoriesButton();
        accessoriesCheck();
        testBBB();
    }


    public void openCompoundWeek(String compound){
        Intent intent = new Intent(getApplicationContext(), Week.class);
        intent.putExtra("Compound", compound);
        intent.putExtra("Cycle", cycleValue);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }


    public void alertBuilder(String title, String message, String extraMessage, boolean cancelable, final boolean noLiftsFound){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        dialog.setTitle(title)
                .setMessage(message)
                .setCancelable(cancelable)
                .setPositiveButton(R.string.no_lifts_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(noLiftsFound) {
                            Intent intent = new Intent(mContext, SetMaxes.class);
                            intent.putExtra("Has_Lifts", true);
                            startActivityForResult(intent, 1);
                        }
                    }
                });
        if(!noLiftsFound){
            TextView tv = new TextView(this);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(ContextCompat.getColor(mContext, R.color.colorRed));
            tv.setText(extraMessage);
            tv.setTextSize(getResources().getDimension(R.dimen.alert_extra_message_text));
            dialog.setView(tv);
        } else {
            dialog.setMessage(message);
        }
        AlertDialog alertDialog = dialog.create();
        alertDialog.show();
    }


    public class emptyCheck implements isItEmpty {
        @Override
        public boolean emptyChecker(boolean val) {
            return val;
        }
    }


    private void startCycle(){
        try{
            if(db.startCycle()){
                cycleValue = db.getCycle();
                Log.d("Cycle_Value", cycleValue+" from if");
            } else {
                Log.d("Cycle_Started", "Cycle has already started");
                Log.d("Cycle_Val", cycleValue+" from else");
            }
        } catch (Exception e){
            cycleValue = 1;
            Log.d("Cycle_Value", "Default from catch");
            e.printStackTrace();
        }
    }


    private void addToTotalMax(){
        ArrayList<CompoundLifts> newLifts = new ArrayList<>();
        for(String lifts: compoundLifts)
            newLifts.add(db.getLifts(lifts));

        for(int i =0; i < compoundLifts.length; i++) {
            CompoundLifts lifts = new CompoundLifts();
            lifts.setCompound_movement(compoundLifts[i]);

            int addedValue;
            if(lifts.getCompound_movement().equals("Deadlift") || lifts.getCompound_movement().equals("Squat"))
                addedValue = 10;
            else
                addedValue = 5;

            lifts.setTraining_max(newLifts.get(i).getTraining_max()+addedValue);
            Log.d("Training_max", lifts.getTraining_max()+"");
            db.updateCompoundStats(lifts);
        }
    }


    private void addLiftsToArray(){
        for(String lifts : compoundLifts){
            liftsArray.add(db.getLifts(lifts));
            liftValues.add(db.getLifts(lifts).getTraining_max());
        }
    }


    private void resetLifValuesArray(){
        liftsArray.clear();
        liftValues.clear();
    }


    public void trainingMaxButton(){
        trainingMax.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(new emptyCheck().emptyChecker(liftsArray.isEmpty())){
                    Intent intent = new Intent(getApplicationContext(), SetMaxes.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(getApplicationContext(), SetMaxes.class);
                    intent.putIntegerArrayListExtra("LIFT_VALUES", (ArrayList<Integer>) liftValues);
                    startActivity(intent);
                }
            }
        });
    }


    public void accessoriesCheck(){
        if(getResources().getBoolean(R.bool.have_accessories)) {
            accessoriesButton();
            accessoryButton.setVisibility(View.VISIBLE);
        }
    }

    //Buttons
    private void benchButton() {
        benchNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompoundWeek("Bench");
            }
        });
    }


    private void deadliftButton(){
        deadliftNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompoundWeek("Deadlift");
            }
        });
    }


    private void pressButton(){
        pressNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompoundWeek("Overhand Press");
            }
        });
    }


    private void squatButton(){
        squatNumbers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openCompoundWeek("Squat");
            }
        });
    }


    private void testBBB(){
        Button testBBB = findViewById(R.id.bbb_test);
        testBBB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(String lifts : compoundLifts){
                    CompoundLifts newLifts = new CompoundLifts();
                    newLifts.setBig_but_boring_weight(0.65f);
                    newLifts.setCompound_movement(lifts);
                    int i = db.updateBBBWeight(newLifts);
                    if(i == 1){
                        Log.d("BBB Update", "Successfully updated BBB weights!");
                    }
                }
            }
        });
    }


    private void updateTrainingMax(){
        tmUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    ArrayList<AsManyRepsAsPossible> newAmrapLifts = new ArrayList<>();
                    //This loop will fail if there's a missing AMRAP value and therefore use the catch
                    for(String liftName : compoundLifts){
                        liftMissingAMRAP = liftName;
                        Log.d("liftMissingAMRAP", liftMissingAMRAP);
                        newAmrapLifts.add(db.getAMRAPValues(liftName, cycleValue));
                    }
                    if(newAmrapLifts.size() == 4) {
                        try {
                            db.updateCycle(cycleValue);
                            cycleValue = db.getCycle();
                            for (String lifts : compoundLifts) {
                                db.createAMRAPTable(cycleValue, lifts);
                            }
                            addToTotalMax();
                            resetLifValuesArray();
                            addLiftsToArray();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Log.d("Error_For_Cycle", "From else");
                        String message = getResources().getString(R.string.amrap_error_message);
                        alertBuilder(getResources().getString(R.string.amrap_error_title), message, liftMissingAMRAP, true, false);
                    }
                } catch (Exception e){
                    Log.d("Error_For_Cycle", "From catch");
                    String message = getResources().getString(R.string.amrap_error_message);
                    alertBuilder(getResources().getString(R.string.amrap_error_title), message, liftMissingAMRAP, true, false);
                }
            }
        });
    }


    public void accessoriesButton (){
        accessoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }
}
