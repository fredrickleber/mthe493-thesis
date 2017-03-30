package thesisGUI;

import java.io.File;

// imports for javafx
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class GUI extends Application {
    
	public static String filePath; // global variable for file path of image
	
    @Override
    public void start(Stage primaryStage) {
    	// Creating border pane for project
    		BorderPane layout = new BorderPane();
    		layout.setPadding((new Insets(20,20,20,20)));
    	    		
        	    		
    		VBox imageDrop = addImageDrop();
    		layout.setTop(imageDrop);
    		imageDrop.setMinHeight(100);
    		imageDrop.setMinWidth(300);
    		
    		
    		BorderPane functionPanel = addFunctionPanel();
    		layout.setCenter(functionPanel);
    		    	        
    	Scene scene = new Scene(layout, 400, 300);
        primaryStage.setTitle("JSCC Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    

	private VBox addImageDrop() {
    	VBox imageDrop = new VBox();
    	
    	Label label = new Label("Drag an image here!");
    	label.setMaxWidth(Double.MAX_VALUE);
    	label.setAlignment(Pos.CENTER);
    	imageDrop.getChildren().addAll(label);
    		
    	// Dragging behavior
		imageDrop.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getGestureSource() != imageDrop
                        && event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY);
                }
                event.consume();
            } 
		});
		
	// Dropping behavior
        imageDrop.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                    success = true;
                    for (File file:db.getFiles()) {
                        filePath = file.getAbsolutePath();
                        // SEND TO GLOBAL VARIABLE, TO USE IN FUTURE FOR ENCODING STEP                        
                        System.out.println(filePath);
                        // DISPLAY IN OUTPUT SCREEN
                    }
                }
                event.setDropCompleted(success);
                event.consume();
            }
        });
    	return imageDrop;
    }
    
	private BorderPane addFunctionPanel() {
    	BorderPane functionPanel = new BorderPane();
 	   	
    	// Channel Property Fields
    	VBox channel = new VBox();
    	
    	Text channelText = new Text("Channel Properties");
    	channelText.setFont(Font.font(null,FontWeight.BOLD,16));
    	
    	Label epsText = new Label("Bit Error Rate: ");
    	ChoiceBox epsChoice = new ChoiceBox(FXCollections.observableArrayList(
    			"0.005", "0.01","0.1")
    			);
    	epsChoice.setMinWidth(100);
    	
    	Label deltaText = new Label("Correlation: ");
    	ChoiceBox deltaChoice = new ChoiceBox(FXCollections.observableArrayList(
    			"Memoryless","5","10")
    			);
    	deltaChoice.setMinWidth(100);
    	
    	channel.getChildren().addAll(channelText,epsText,epsChoice,deltaText,deltaChoice);
    	functionPanel.setLeft(channel);
    	  	
    	// Encoding and sending actions
    	VBox buttons = new VBox();
        Text operateText = new Text("Operate Channel");
        operateText.setFont(Font.font(null,FontWeight.BOLD,16));
        
    	/* CHANNEL BUTTON */
    	Button channelBtn = new Button("send");
    	channelBtn.setMinWidth(100);
    	
    	channelBtn.setOnAction((ActionEvent e) -> {
	    	
    		//ENTER REQUESTED ACTION HERE FOR SEND BUTTON (e.g. operate channel using vector created from encode step)
    		System.out.println(deltaChoice.getValue());
    		System.out.println(epsChoice.getValue());
    		System.out.println(filePath);
    			
    	});
    		/* OUTPUT BUTTON */
    	Button outputBtn = new Button("output");
    	outputBtn.setMinWidth(100);
    	buttons.getChildren().addAll(operateText,channelBtn,outputBtn);
    	
    	outputBtn.setOnAction((ActionEvent e) -> {
	    	
    		//ENTER REQUESTED ACTION HERE FOR OUTPUT BUTTON (e.g. display/compare outputs of picture)
    		System.out.println(deltaChoice.getValue());
    			
    	});
    	

    	buttons.setSpacing(15);
    	
    	
    	
    	functionPanel.setRight(buttons);
    	    	
    	return functionPanel;
    }
    
       public static void main(String[] args) {
        launch(args);
    }
}
