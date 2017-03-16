import java.io.File;

// imports for javafx
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import javafx.geometry.Insets;

public class GUI extends Application {
    
	public static String filePath; // global variable for file path of image
	
    @Override
    public void start(Stage primaryStage) {
    	// Creating border pane for project
    		BorderPane layout = new BorderPane();
    		layout.setPadding((new Insets(20,20,20,20)));
    	    		
        	    		
    		VBox imageDrop = addImageDrop();
    		layout.setTop(imageDrop);
    		imageDrop.setMinHeight(300);
    		
    		
    		BorderPane functionPanel = addFunctionPanel();
    		layout.setCenter(functionPanel);
    		    	        
    	Scene scene = new Scene(layout, 400, 450);
        primaryStage.setTitle("JSCC Application");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    

	private VBox addImageDrop() {
    	VBox imageDrop = new VBox();
    	
    	Label label = new Label("Drag a greyscale image here!");
    	Label dropped = new Label("");
    	imageDrop.getChildren().addAll(label,dropped);
    		
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
    	Label outputMsg = new Label();
 	   	
    	// Channel Property Fields
    	VBox channel = new VBox();
    	
    	Text channelText = new Text("Channel Properties");
    	
    	Label epsText = new Label("Bit Error Rate: ");
    	TextField epsField = new TextField ();
    	   	
    	Label deltaText = new Label("Burstyness: ");
    	TextField deltaField = new TextField ();
    	
    	channel.getChildren().addAll(channelText,epsText,epsField,deltaText,deltaField);
    	functionPanel.setLeft(channel);
    	  	
    	// Encoding and sending actions
    	VBox buttons = new VBox();
    		/* ENCODING BUTTON */
    	Button encoderBtn = new Button("encode");
    	encoderBtn.setMinWidth(100);
    	encoderBtn.setOnAction((ActionEvent e) -> {
      
     		//ENTER REQUESTED ACTION HERE FOR ENCODE BUTTON (e.g. encode using channel params & file path)
    		System.out.println(epsField.getText());
    		if (filePath==null) {
    			outputMsg.setText("Encoding error: incorrect input!");
    		} else {
    			// ENCODING CALLS HERE
    			outputMsg.setText("Encoding...");	
    		}
    		functionPanel.setBottom(outputMsg);
    	});
        	/* CHANNEL BUTTON */
    	Button channelBtn = new Button("send");
    	channelBtn.setMinWidth(100);
    	channelBtn.setOnAction((ActionEvent e) -> {
	    	
    		//ENTER REQUESTED ACTION HERE FOR SEND BUTTON (e.g. operate channel using vector created from encode step)
    		System.out.println(deltaField.getText());
    			
    	});
    		/* OUTPUT BUTTON */
    	Button outputBtn = new Button("output");
    	outputBtn.setMinWidth(100);
    	buttons.getChildren().addAll(encoderBtn,channelBtn,outputBtn);
    	outputBtn.setOnAction((ActionEvent e) -> {
	    	
    		//ENTER REQUESTED ACTION HERE FOR OUTPUT BUTTON (e.g. display/compare outputs of picture)
    		System.out.println(deltaField.getText());
    			
    	});
    	
    	functionPanel.setRight(buttons);
    	    	
    	return functionPanel;
    }
 
    public static void main(String[] args) {
        launch(args);
    }
}
