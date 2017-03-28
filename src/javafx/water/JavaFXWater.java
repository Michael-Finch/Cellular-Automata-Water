/*
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
|
|   Project: JavaFX Water
|   Name: Michael Finch
|   Date: 1/31/17
|   Description: Water Animation using cellular automata
|
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 */
package javafx.water;

import java.util.ArrayList;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class JavaFXWater extends Application {

    public static void main(String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        //Allow the combobox to work on systems with touch screens
        System.setProperty("glass.accessible.force", "false");
        //Pane that contains an array representing water
        WaterPane paneWater = new WaterPane(false);
        paneWater.displayState();
        //HBox to hold start stop and clear buttons
        HBox paneButtons = new HBox();
        //HBox to hold combobox and textfield
        HBox paneSelection = new HBox();
        //Button to start everything
        Button buttonStart = new Button("Start");
        //Button to stop everything
        Button buttonStop = new Button("Stop");
        //Button to clear the pane
        Button buttonClear = new Button("Clear");
        //Combobox for selecting what to place
        ObservableList<String> options
                = FXCollections.observableArrayList(
                        "Water Source",
                        "Stone",
                        "Eraser"
                );
        final ComboBox comboBoxSelection = new ComboBox(options);
        comboBoxSelection.setValue("Water Source");
        paneSelection.getChildren().add(comboBoxSelection);
        paneButtons.getChildren().addAll(buttonStart, buttonStop, buttonClear);
        //VBox to hold the water pane and a button to start everything
        VBox containerPane = new VBox(paneWater, paneButtons, paneSelection);
        Scene scene = new Scene(containerPane);
        stage.setScene(scene);
        stage.show();
        

        //Task to animate the display
        Task updateDisplayTask = new Task<Void>() {
            @Override
            public Void call() throws Exception {
                while (true) {
                    
                        Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            paneWater.calculateNextState(paneWater.waterArray);
                            paneWater.displayState();
                        }
                    });
                    Thread.sleep(50);
                    
                }
            }
        };
        //Define a thread for the updateDisplayTask
        Thread th = new Thread(updateDisplayTask);
        th.setDaemon(true);
        //Set what the start button does
        buttonStart.setOnAction(e -> {
            if(!th.isAlive()){
                th.start();
            }
            th.resume();
        });
        //Set what the stop button does
        buttonStop.setOnAction(e -> {
            th.suspend();
        });
        //Set what the clear button does
        buttonClear.setOnAction(e ->{
            paneWater.clear();
        });
        //Allow placement of cells by clicking and dragging
        paneWater.setOnMouseDragged(e ->{
            int[] clickLocation = paneWater.getMouseClick(e);
            System.out.println(comboBoxSelection.getValue());
            if(comboBoxSelection.getValue() == "Water Source"){
                paneWater.placeWaterSource(clickLocation[0], clickLocation[1]);
            }
            else if(comboBoxSelection.getValue() == "Stone"){
                paneWater.placeRock(clickLocation[0], clickLocation[1]);
            }
            else if(comboBoxSelection.getValue() == "Eraser"){
                paneWater.eraseCell(clickLocation[0], clickLocation[1]);
            }
        });
        paneWater.setOnMouseClicked(e ->{
            int[] clickLocation = paneWater.getMouseClick(e);
            System.out.println(comboBoxSelection.getValue());
            if(comboBoxSelection.getValue() == "Water Source"){
                paneWater.placeWaterSource(clickLocation[0], clickLocation[1]);
            }
            else if(comboBoxSelection.getValue() == "Stone"){
                paneWater.placeRock(clickLocation[0], clickLocation[1]);
            }
            else if(comboBoxSelection.getValue() == "Eraser"){
                paneWater.eraseCell(clickLocation[0], clickLocation[1]);
            }
        });
    }
}

//Class for water pane
class WaterPane extends Pane {
    int[][] waterArray;
    ArrayList<Integer> updatedRows = new ArrayList<>();
    ArrayList<Integer> updatedColumns = new ArrayList<>();
    static final int STONE_BLOCK = -1;
    static final int WATER_SOURCE = Integer.MAX_VALUE;
    final int ROWS = 50;
    final int COLUMNS = 50;
    final int CELL_SIZE = 15;
    final int PRESSURE_TO_FLOW_UP = 10;
    final int HIGHEST_ALLOWED_PRESSURE = 10;
    final int NUMBER_OF_WATER_SOURCES = 10;
    final int NUMBER_OF_ROCKS =  50;
    
    final Color paintWaterBase = new Color(0, 0, 1, 1);
    final Color paintBackground = new Color(1, 1, 1, 1);
    final Color paintRock = new Color(.5, .5, .5, 1);
    
    final boolean SEIZURE_MODE = false;

    //Constructor for water pane
    WaterPane(boolean emptyArray) {
        if(emptyArray){
            this.waterArray = getArray(ROWS, COLUMNS, 0, 0);
        }
        else{
            this.waterArray = getArray(ROWS, COLUMNS, NUMBER_OF_WATER_SOURCES, NUMBER_OF_ROCKS);
        }
        
    }

    //Method to get an initial array with a little bit of water
    static public int[][] getArray(int rows, int columns, int numberOfWaterSources, int numberOfRocks) {
        int[][] waterArray = new int[rows][columns];
        //Place the water sources
        while(numberOfWaterSources > 0){
            int randomRow = (int)(Math.random() * rows);
            int randomColumn = (int)(Math.random() * columns);
            if(waterArray[randomRow][randomColumn] != WATER_SOURCE && waterArray[randomRow][randomColumn] != STONE_BLOCK){
                waterArray[randomRow][randomColumn] = WATER_SOURCE;
                numberOfWaterSources--;
            }
        }
        //Place the rocks
        while(numberOfRocks > 0){
            int randomRow = (int)(Math.random() * rows);
            int randomColumn = (int)(Math.random() * columns);
            if(waterArray[randomRow][randomColumn] != STONE_BLOCK && waterArray[randomRow][randomColumn] != WATER_SOURCE){
                waterArray[randomRow][randomColumn] = STONE_BLOCK;
                numberOfRocks--;
            }
        }
        for(int i = 0; i < rows; i++){
            for(int j = 0; j < columns; j++){
                double rand = Math.random();
                if(waterArray[i][j] != WATER_SOURCE && waterArray[i][j] != STONE_BLOCK){
                    waterArray[i][j] = 0;
                }
            }
        }
        return waterArray;
    }

    //Display the state of the array
    public void displayState() {
        //Clear the currently displayed state
        this.getChildren().clear();
        for (int row = 0; row < waterArray.length; row++) {
            for (int column = 0; column < waterArray[0].length; column++) {
                Rectangle rectangle = new Rectangle(column * CELL_SIZE, row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                if (waterArray[row][column] > 0) {
                    if (waterArray[row][column] >= HIGHEST_ALLOWED_PRESSURE) {
                        if(!SEIZURE_MODE){
                            rectangle.setFill(paintWaterBase);
                        }
                        else{
                            rectangle.setFill(new Color(Math.random(), Math.random(), Math.random(), 1));
                        }
                    }
                    else{
                        if(!SEIZURE_MODE){
                            rectangle.setFill(new Color(0, 0, 1, (double) waterArray[row][column] / HIGHEST_ALLOWED_PRESSURE));
                        }
                        else{
                            rectangle.setFill(new Color(Math.random(), Math.random(), Math.random(), (double) waterArray[row][column] / HIGHEST_ALLOWED_PRESSURE));
                        }
                        
                    }
                }
                else if (waterArray[row][column] == STONE_BLOCK) {
                    rectangle.setFill(paintRock);
                }
                else {
                    rectangle.setFill(paintBackground);
                    
                }
                this.getChildren().add(rectangle);
            }
        }
    }

    //Method to calculate the next state of the array
    public void calculateNextState(int[][] givenArray) {
        //Create a duplicate array
        int[][] newArray = new int[givenArray.length][givenArray[0].length];
        for (int i = 0; i < givenArray.length; i++) {
            for (int j = 0; j < givenArray[i].length; j++) {
                newArray[i][j] = givenArray[i][j];
            }
        }

        //Calculate and apply the changes to the new array
        for (int row = 0; row < givenArray.length; row++) {
            for (int column = 0; column < givenArray[0].length; column++) {
                if (givenArray[row][column] > 0) {
                    //Flow down if able
                    if (canFlowDown(row, column, givenArray)) {
                        flowDown(row, column, newArray);
                    } //If can not flow down, see if it can flow to the left or right
                    //Check to see if it can flow to the left or right, flow randomly if it can
                    else if (canFlowLeftOrRight(row, column, givenArray)) {
                        flowLeftOrRight(row, column, newArray);
                    } //Check to see if it can flow left, flow left if it can
                    else if (canFlowLeft(row, column, givenArray)) {
                        flowLeft(row, column, newArray);
                    } //Check to see if it can flow Right, flow right if it can
                    else if (canFlowRight(row, column, givenArray)) {
                        flowRight(row, column, newArray);
                    } //If it can't do any of this, flow up if the pressure is high enough
                    else if (canFlowUp(row, column, givenArray)) {
                        flowUp(row, column, newArray);
                    }
                }
            }
        }
        //Update the pane's array
        this.waterArray = newArray;
    }

    //Method to print the state of the array in the console
    public void printState() {
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < waterArray.length; j++) {
                for (int k = 0; k < waterArray[0].length; k++) {
                    System.out.print(waterArray[j][k] + ", ");
                }
                System.out.println();
            }
            System.out.println();
            System.out.println();
        }
    }
    
    //Method to determine if water can flow downwards
    public boolean canFlowDown(int row, int column, int[][] givenArray){
        if(row < givenArray.length - 1){
            if((givenArray[row + 1][column] < HIGHEST_ALLOWED_PRESSURE) && (givenArray[row][column] > 0) && (givenArray[row + 1][column] != STONE_BLOCK)){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }
    
    //Method to determine if water can flow left
    public boolean canFlowLeft(int row, int column, int[][] givenArray){
        if(!isOnLeftEdge(row, column, givenArray)){
            if(givenArray[row][column - 1] < givenArray[row][column] && (givenArray[row][column - 1] != STONE_BLOCK)){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }
    
    //Method to determine if water can flow left
    public boolean canFlowRight(int row, int column, int[][] givenArray){
        if(!isOnRightEdge(row, column, givenArray)){
            if(givenArray[row][column + 1] < givenArray[row][column] && (givenArray[row][column + 1] != STONE_BLOCK)){
                return true;
            }
            else{
                return false;
            }
        }
        else{
            return false;
        }
    }
    
    //Method to determine if water can flow left or right
    public boolean canFlowLeftOrRight(int row, int column, int[][] givenArray){
        if(canFlowLeft(row, column, givenArray) && canFlowRight(row, column, givenArray)){
            return true;
        }
        else{
            return false;
        }
    }
    
    //Method to determine if water can flow up
    public boolean canFlowUp(int row, int column, int[][] givenArray) {
        if (row > 0) {
            if (givenArray[row][column] >= PRESSURE_TO_FLOW_UP) {
                if ((givenArray[row - 1][column] < HIGHEST_ALLOWED_PRESSURE) && (givenArray[row][column] > 0) && (givenArray[row - 1][column] != STONE_BLOCK)) {
                    return true;
                }
                else {
                    return false;
                }
            }
            else{
                return false;
            }

        }
        else {
            return false;
        }
    }
    
    //Method to determine if the water is on a right edge
    public boolean isOnRightEdge(int row, int column, int[][] givenArray){
        if(column == givenArray[0].length - 1){
            return true;
        }
        else{
            return false;
        }
    }
    
    //Method to determine if the water is on a left edge{
    public boolean isOnLeftEdge(int row, int column, int[][] givenArray){
        if(column == 0){
            return true;
        }
        else{
            return false;
        }
    }
    
    //Method for flowing down
    public void flowDown(int row, int column, int[][] newArray){
        newArray[row + 1][column] += 1;
        newArray[row][column] -= 1;
    }
    
    //Method for flowing left
    public void flowLeft(int row, int column, int[][] newArray){
        newArray[row][column - 1] += 1;
        newArray[row][column] -= 1;
    }
    
    //Method for flowing right
    public void flowRight(int row, int column, int[][] newArray){
        newArray[row][column + 1] += 1;
        newArray[row][column] -= 1;
    }
    
    //Method for flowing up
    public void flowUp(int row, int column, int[][] newArray){
        newArray[row - 1][column] += 1;
        newArray[row][column] -= 1;
    }
    
    //Method for randomly flowing left or right
    public void flowLeftOrRight(int row, int column, int[][] newArray){
        double rand = Math.random();
        if(rand >= 0.5){
            flowLeft(row, column, newArray);
        }
        else{
            flowRight(row, column, newArray);
        }
    }
    
    //Method for getting the location of a mouse click
    public int[] getMouseClick(MouseEvent e){
        //Get the row and column clicked on
        int[] clickLocation = new int[2];
        clickLocation[0] = (int)Math.round(e.getY() / CELL_SIZE - 0.5);
        clickLocation[1] = (int)Math.round(e.getX() / CELL_SIZE - 0.5);
        System.out.println("Click at: " + clickLocation[0] + ", " + clickLocation[1]);
        return clickLocation;
    }
    
    //Method for placing water sources
    public void placeWaterSource(int row, int column){
        waterArray[row][column] = WATER_SOURCE;
        displayState();
    }
    
    //Method for placing rocks
    public void placeRock(int row, int column){
        waterArray[row][column] = STONE_BLOCK;
        displayState();
    }
    
    //Method for erasing cells
    public void eraseCell(int row, int column){
        waterArray[row][column] = 0;
        displayState();
    }
    
    //Method for clearing the pane
    public void clear(){
        this.waterArray = getArray(ROWS, COLUMNS, 0, 0);
        this.displayState();
    }
}