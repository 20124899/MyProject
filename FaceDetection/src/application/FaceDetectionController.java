package application;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;
import org.opencv.videoio.VideoCapture;

import application.Utils;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * The controller associated with the only view of our application. 
 * 애플리케이션의 유일한 view 연결 컨트롤러입니다.
 * The application logic is implemented here. 
 * 애플리케이션의 로직이 이곳에서 구현됩니다.
 * It handles the button for starting/stopping the camera, 
 * 이 기능은 카메라 시동/정지 버튼,
 * the acquired video stream, 
 * 획득한 비디오 스크림, 
 * the relative controls and the face detection/tracking.
 * 상대 제어 장치 및 얼굴 검출/추적 버튼을 다룹니다.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.1 (2015-11-10)
 * @since 1.0 (2014-01-10)
 * 		
 */
public class FaceDetectionController
{
	// FXML buttons
	// FXML 버튼
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	// 현재 프레임을 보여주기 위한 FXML 영역
	@FXML
	private ImageView originalFrame;
	// checkboxes for enabling/disabling a classifier
	// 분급기 활성화/비활성화 확인란
	// 영상에서의 영역과 영역의 밝기차를 이용한 feature(haar Classifier)
	@FXML
	private CheckBox haarClassifier; 
	// 영상의 모든 픽셀에 대해 계산되는 값으로서 각 픽셀의 주변 3 x 3 영역의 상대적인 밝기 변화를 2진수로 코딩한 인덱스 값(LBP Classifier)
	@FXML
	private CheckBox lbpClassifier; 
	
	// a timer for acquiring the video stream
	// 비디오 스크림을 수집하기 위한 타이머
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	// 비디오 캡쳐를 수행하기 위한 OpenCV 객체
	private VideoCapture capture;
	// a flag to change the button behavior
	// 버튼 동작 변경 플래그
	private boolean cameraActive;
	
	// face cascade classifier
	// 얼굴 cascade 분류기
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	
	/**
	 * Init the controller, at start time
	 * 시작 시 컨트롤러 초기화
	 */
	protected void init()
	{
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;
		
		// set a fixed width for the frame
		// 프레임에 고정 폭 설정
		originalFrame.setFitWidth(600);
		// preserve image ratio
		// 이미지 비율 유지
		originalFrame.setPreserveRatio(true);
	}
	
	/**
	 * The action triggered by pushing the button on the GUI
	 * GUI에서 버튼을 눌러서 트리거 된 동작
	 */
	@FXML
	protected void startCamera()
	{	
		if (!this.cameraActive)
		{
			// disable setting checkboxes
			// 사용 안하는 설정 확인란
			this.haarClassifier.setDisable(true);
			this.lbpClassifier.setDisable(true);
			
			// start the video capture
			// 비디오 캡쳐 시작
			this.capture.open(0);
			
			// is the video stream available?
			// 비디오 스크림을 사용할 수 있나요?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				// 33ms마다 프레임 캡처(30프레임/초)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						// 효과적으로 단일 프레임을 잡아서 처리
						Mat frame = grabFrame();
						// convert and show the frame
						// 프레임 변환 및 표시
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				// 버튼 컨텐츠 업데이트
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				// 오류 기록
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			// 이 시점에서는 카메라가 비활성화 되어 있습니다.
			this.cameraActive = false;
			// update again the button content
			// 버튼 컨텐츠 재업데이트
			this.cameraButton.setText("Start Camera");
			// enable classifiers checkboxes
			// 분류기 확인란 활성화
			this.haarClassifier.setDisable(false);
			this.lbpClassifier.setDisable(false);
			
			// stop the timer
			// 타이머 정지
			this.stopAcquisition();
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * 열린 비디오 스트림에서 프레임 가져오기(있는 경우)
	 * @return the {@link Image} to show
	 * @return는 @링크 이미지를 보여줍니다.
	 */
	private Mat grabFrame()
	{
		Mat frame = new Mat();
		
		// check if the capture is open
		// 캡쳐가 열려있는지 확인
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				// 현재 프레임 읽기
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				// 프레임이 비어있지 않으면 처리
				if (!frame.empty())
				{
					// face detection
					// 얼굴 인식
					this.detectAndDisplay(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				//(전체) 오류 기록
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * Method for face detection and tracking
	 * 안면 인식 검출 및 추적 방법
	 * @param frame
	 *            it looks for faces in this frame
	 * @param frame 안에서 얼굴을 찾습니다.
	 */
	private void detectAndDisplay(Mat frame)
	{
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();	
		// convert the frame in gray scale
		// 프레임을 회색으로 변환
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		// 프레임 히스토그램을 균등화하여 결과를 향상시킴
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		// compute minimum face size (20% of the frame height, in our case)
		// 최소 페이스 크기 계산(이 경우, 프레임 높이의 20%)
		if (this.absoluteFaceSize == 0)
		{
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0)
			{
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		// detect faces
		// 얼굴 감지
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
		// each rectangle in faces is a face: draw them!
		// 사진 안에 있는 각각의 직사각형은 얼굴이다 : 그려보세요!
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
			
	}
	
	/**
	 * The action triggered by selecting the Haar Classifier checkbox. 
	 * Haar Classifier 체크 박스를 선택하면 실행되는 동작입니다.
	 * It loads the trained set to be used for frontal face detection.
	 * 전면 검출에 사용할 교육용 세트를 로드합니다.
	 */
	@FXML
	protected void haarSelected(Event event)
	{
		// check whether the lpb checkbox is selected and deselect it
		//  lpb 체크 박스가 선택되어 있는지 확인하고 선택을 해제하십시오.
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);
		// 버그 - 캐스케이드 절대경로 지정(Assertion failed (!empty()) 오류가 발생하고 cascade를 인식하지 못함.)
		this.checkboxSelection("C:\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml");
		
	}
	
	/**
	 * The action triggered by selecting the LBP Classifier checkbox. 
	 * LBP Classifier 체크 박스를 선택하면 실행되는 동작입니다.
	 * It loads the trained set to be used for frontal face detection.
	 * 전면 검출에 사용할 교육용 세트를 로드합니다.
	 */
	@FXML
	protected void lbpSelected(Event event)
	{
		// check whether the haar checkbox is selected and deselect it
		// haar 체크 박스가 선택되어 있는지 확인하고 선택을 해제하십시오.
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);			
		// 버그 - 캐스케이드 절대경로 지정(Assertion failed (!empty()) 오류가 발생하고 cascade를 인식하지 못함.)
		this.checkboxSelection("C:\\opencv\\sources\\data\\lbpcascades\\lbpcascade_frontalface.xml");
	}
	
	/**
	 * Method for loading a classifier trained set from disk
	 * 디스크에서 분급기 교육을 받은 세트를 로딩하는 방법
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 *            디스크 상에서 분급자가 교육을 받은 세트가 위치한 경로
	 */
	private void checkboxSelection(String classifierPath)
	{
		// load the classifier(s)
		// 분급기를 로드하십시오.
		this.faceCascade.load(classifierPath);
		
		// now the video capture can start
		// 이제 비디오 캡쳐를 시작할 수 있습니다.
		this.cameraButton.setDisable(false);
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 * 카메라에서 포착을 중지하고 모든 리소스를 해제하십시오.
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				// 타이머 정지
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				// 모든 예외 기록
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			// 카메라를 해제하다
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * JavaFX 메인 스레드의 {@link ImageView} 업데이트
	 * @param view
	 *            the {@link ImageView} to update
	 * @param view는 {@link ImageView} 업데이트
	 * @param image
	 *            the {@link Image} to show
	 * @param image는 {@link Image} 표시
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 * 애플리케이션을 닫을 때 카메라에서 포착을 중지합니다.
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}