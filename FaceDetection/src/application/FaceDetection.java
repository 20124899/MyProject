package application;
	
import org.opencv.core.Core;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.fxml.FXMLLoader;

/**
 * The main class for a JavaFX application. It creates and handle the main
 * JavaFX 응용 프로그램의 기본 클래스 입니다. 메인을 생성하고 처리합니다.
 * window with its resources (style, graphics, etc.).
 * 윈도우의 리소스(스타일, 그래픽스, 기타 등등..)
 * This application handles a video stream and try to find any possible human
 * 이 애플리케이션은 비디오 스트림을 처리하며, 가능한 모든 사람을 찾습니다.
 * face in a frame. It can use the Haar or the LBP classifier.
 * 프레임 안에서 얼굴은 Haar나 LBP분급기를 사용할 수 있습니다.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 2.0 (2017-03-10)
 * @since 1.0 (2014-01-10)
 * 
 */
public class FaceDetection extends Application
{
	@Override
	public void start(Stage primaryStage)
	{
		try
		{
			// load the FXML resource
			// FXML 리소스를 로드합니다.
			FXMLLoader loader = new FXMLLoader(getClass().getResource("FaceDetection.fxml"));
			BorderPane root = (BorderPane) loader.load();
			// set a whitesmoke background
			// 배경을 화이트 스모크 (color HEX값 : #F5F5F5)값으로 설정합니다.(설정 초기화)
			root.setStyle("-fx-background-color: whitesmoke;");
			// create and style a scene
			// Scene 스타일을 생성합니다. (SceneBuilder용)
			Scene scene = new Scene(root, 800, 600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			// create the stage with the given title and the previously created
			// 주어진 타이틀과 이전에 생성된 요소들로 stage를 생성합니다.
			// scene
			primaryStage.setTitle("Face Detection and Tracking");
			primaryStage.setScene(scene);
			// show the GUI
			// GUI를 보여줍니다.
			primaryStage.show();
			
			// init the controller
			// 컨트롤러를 초기화합니다.
			FaceDetectionController controller = loader.getController();
			controller.init();
			
			// set the proper behavior on closing the application
			// 애플리케이션을 종료할 때, 올바른 동작을 설정합니다.
			primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
				public void handle(WindowEvent we)
				{
					controller.setClosed();
				}
			}));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		// load the native OpenCV library
		// 네이리브(특정 운용 프로그램에 내재된) OpenCV 라이브러리 로드
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		launch(args);
	}
}