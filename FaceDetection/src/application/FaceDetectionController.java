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
 * ���ø����̼��� ������ view ���� ��Ʈ�ѷ��Դϴ�.
 * The application logic is implemented here. 
 * ���ø����̼��� ������ �̰����� �����˴ϴ�.
 * It handles the button for starting/stopping the camera, 
 * �� ����� ī�޶� �õ�/���� ��ư,
 * the acquired video stream, 
 * ȹ���� ���� ��ũ��, 
 * the relative controls and the face detection/tracking.
 * ��� ���� ��ġ �� �� ����/���� ��ư�� �ٷ�ϴ�.
 * 
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @version 1.1 (2015-11-10)
 * @since 1.0 (2014-01-10)
 * 		
 */
public class FaceDetectionController
{
	// FXML buttons
	// FXML ��ư
	@FXML
	private Button cameraButton;
	// the FXML area for showing the current frame
	// ���� �������� �����ֱ� ���� FXML ����
	@FXML
	private ImageView originalFrame;
	// checkboxes for enabling/disabling a classifier
	// �бޱ� Ȱ��ȭ/��Ȱ��ȭ Ȯ�ζ�
	// ���󿡼��� ������ ������ ������� �̿��� feature(haar Classifier)
	@FXML
	private CheckBox haarClassifier; 
	// ������ ��� �ȼ��� ���� ���Ǵ� �����μ� �� �ȼ��� �ֺ� 3 x 3 ������ ������� ��� ��ȭ�� 2������ �ڵ��� �ε��� ��(LBP Classifier)
	@FXML
	private CheckBox lbpClassifier; 
	
	// a timer for acquiring the video stream
	// ���� ��ũ���� �����ϱ� ���� Ÿ�̸�
	private ScheduledExecutorService timer;
	// the OpenCV object that performs the video capture
	// ���� ĸ�ĸ� �����ϱ� ���� OpenCV ��ü
	private VideoCapture capture;
	// a flag to change the button behavior
	// ��ư ���� ���� �÷���
	private boolean cameraActive;
	
	// face cascade classifier
	// �� cascade �з���
	private CascadeClassifier faceCascade;
	private int absoluteFaceSize;
	
	/**
	 * Init the controller, at start time
	 * ���� �� ��Ʈ�ѷ� �ʱ�ȭ
	 */
	protected void init()
	{
		this.capture = new VideoCapture();
		this.faceCascade = new CascadeClassifier();
		this.absoluteFaceSize = 0;
		
		// set a fixed width for the frame
		// �����ӿ� ���� �� ����
		originalFrame.setFitWidth(600);
		// preserve image ratio
		// �̹��� ���� ����
		originalFrame.setPreserveRatio(true);
	}
	
	/**
	 * The action triggered by pushing the button on the GUI
	 * GUI���� ��ư�� ������ Ʈ���� �� ����
	 */
	@FXML
	protected void startCamera()
	{	
		if (!this.cameraActive)
		{
			// disable setting checkboxes
			// ��� ���ϴ� ���� Ȯ�ζ�
			this.haarClassifier.setDisable(true);
			this.lbpClassifier.setDisable(true);
			
			// start the video capture
			// ���� ĸ�� ����
			this.capture.open(0);
			
			// is the video stream available?
			// ���� ��ũ���� ����� �� �ֳ���?
			if (this.capture.isOpened())
			{
				this.cameraActive = true;
				
				// grab a frame every 33 ms (30 frames/sec)
				// 33ms���� ������ ĸó(30������/��)
				Runnable frameGrabber = new Runnable() {
					
					@Override
					public void run()
					{
						// effectively grab and process a single frame
						// ȿ�������� ���� �������� ��Ƽ� ó��
						Mat frame = grabFrame();
						// convert and show the frame
						// ������ ��ȯ �� ǥ��
						Image imageToShow = Utils.mat2Image(frame);
						updateImageView(originalFrame, imageToShow);
					}
				};
				
				this.timer = Executors.newSingleThreadScheduledExecutor();
				this.timer.scheduleAtFixedRate(frameGrabber, 0, 33, TimeUnit.MILLISECONDS);
				
				// update the button content
				// ��ư ������ ������Ʈ
				this.cameraButton.setText("Stop Camera");
			}
			else
			{
				// log the error
				// ���� ���
				System.err.println("Failed to open the camera connection...");
			}
		}
		else
		{
			// the camera is not active at this point
			// �� ���������� ī�޶� ��Ȱ��ȭ �Ǿ� �ֽ��ϴ�.
			this.cameraActive = false;
			// update again the button content
			// ��ư ������ �������Ʈ
			this.cameraButton.setText("Start Camera");
			// enable classifiers checkboxes
			// �з��� Ȯ�ζ� Ȱ��ȭ
			this.haarClassifier.setDisable(false);
			this.lbpClassifier.setDisable(false);
			
			// stop the timer
			// Ÿ�̸� ����
			this.stopAcquisition();
		}
	}
	
	/**
	 * Get a frame from the opened video stream (if any)
	 * ���� ���� ��Ʈ������ ������ ��������(�ִ� ���)
	 * @return the {@link Image} to show
	 * @return�� @��ũ �̹����� �����ݴϴ�.
	 */
	private Mat grabFrame()
	{
		Mat frame = new Mat();
		
		// check if the capture is open
		// ĸ�İ� �����ִ��� Ȯ��
		if (this.capture.isOpened())
		{
			try
			{
				// read the current frame
				// ���� ������ �б�
				this.capture.read(frame);
				
				// if the frame is not empty, process it
				// �������� ������� ������ ó��
				if (!frame.empty())
				{
					// face detection
					// �� �ν�
					this.detectAndDisplay(frame);
				}
				
			}
			catch (Exception e)
			{
				// log the (full) error
				//(��ü) ���� ���
				System.err.println("Exception during the image elaboration: " + e);
			}
		}
		
		return frame;
	}
	
	/**
	 * Method for face detection and tracking
	 * �ȸ� �ν� ���� �� ���� ���
	 * @param frame
	 *            it looks for faces in this frame
	 * @param frame �ȿ��� ���� ã���ϴ�.
	 */
	private void detectAndDisplay(Mat frame)
	{
		MatOfRect faces = new MatOfRect();
		Mat grayFrame = new Mat();	
		// convert the frame in gray scale
		// �������� ȸ������ ��ȯ
		Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
		// equalize the frame histogram to improve the result
		// ������ ������׷��� �յ�ȭ�Ͽ� ����� ����Ŵ
		Imgproc.equalizeHist(grayFrame, grayFrame);
		
		// compute minimum face size (20% of the frame height, in our case)
		// �ּ� ���̽� ũ�� ���(�� ���, ������ ������ 20%)
		if (this.absoluteFaceSize == 0)
		{
			int height = grayFrame.rows();
			if (Math.round(height * 0.2f) > 0)
			{
				this.absoluteFaceSize = Math.round(height * 0.2f);
			}
		}
		// detect faces
		// �� ����
		this.faceCascade.detectMultiScale(grayFrame, faces, 1.1, 2, 0 | Objdetect.CASCADE_SCALE_IMAGE,
				new Size(this.absoluteFaceSize, this.absoluteFaceSize), new Size());
		// each rectangle in faces is a face: draw them!
		// ���� �ȿ� �ִ� ������ ���簢���� ���̴� : �׷�������!
		Rect[] facesArray = faces.toArray();
		for (int i = 0; i < facesArray.length; i++)
			Imgproc.rectangle(frame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);
			
	}
	
	/**
	 * The action triggered by selecting the Haar Classifier checkbox. 
	 * Haar Classifier üũ �ڽ��� �����ϸ� ����Ǵ� �����Դϴ�.
	 * It loads the trained set to be used for frontal face detection.
	 * ���� ���⿡ ����� ������ ��Ʈ�� �ε��մϴ�.
	 */
	@FXML
	protected void haarSelected(Event event)
	{
		// check whether the lpb checkbox is selected and deselect it
		//  lpb üũ �ڽ��� ���õǾ� �ִ��� Ȯ���ϰ� ������ �����Ͻʽÿ�.
		if (this.lbpClassifier.isSelected())
			this.lbpClassifier.setSelected(false);
		// ���� - ĳ�����̵� ������ ����(Assertion failed (!empty()) ������ �߻��ϰ� cascade�� �ν����� ����.)
		this.checkboxSelection("C:\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml");
		
	}
	
	/**
	 * The action triggered by selecting the LBP Classifier checkbox. 
	 * LBP Classifier üũ �ڽ��� �����ϸ� ����Ǵ� �����Դϴ�.
	 * It loads the trained set to be used for frontal face detection.
	 * ���� ���⿡ ����� ������ ��Ʈ�� �ε��մϴ�.
	 */
	@FXML
	protected void lbpSelected(Event event)
	{
		// check whether the haar checkbox is selected and deselect it
		// haar üũ �ڽ��� ���õǾ� �ִ��� Ȯ���ϰ� ������ �����Ͻʽÿ�.
		if (this.haarClassifier.isSelected())
			this.haarClassifier.setSelected(false);			
		// ���� - ĳ�����̵� ������ ����(Assertion failed (!empty()) ������ �߻��ϰ� cascade�� �ν����� ����.)
		this.checkboxSelection("C:\\opencv\\sources\\data\\lbpcascades\\lbpcascade_frontalface.xml");
	}
	
	/**
	 * Method for loading a classifier trained set from disk
	 * ��ũ���� �бޱ� ������ ���� ��Ʈ�� �ε��ϴ� ���
	 * @param classifierPath
	 *            the path on disk where a classifier trained set is located
	 *            ��ũ �󿡼� �б��ڰ� ������ ���� ��Ʈ�� ��ġ�� ���
	 */
	private void checkboxSelection(String classifierPath)
	{
		// load the classifier(s)
		// �бޱ⸦ �ε��Ͻʽÿ�.
		this.faceCascade.load(classifierPath);
		
		// now the video capture can start
		// ���� ���� ĸ�ĸ� ������ �� �ֽ��ϴ�.
		this.cameraButton.setDisable(false);
	}
	
	/**
	 * Stop the acquisition from the camera and release all the resources
	 * ī�޶󿡼� ������ �����ϰ� ��� ���ҽ��� �����Ͻʽÿ�.
	 */
	private void stopAcquisition()
	{
		if (this.timer!=null && !this.timer.isShutdown())
		{
			try
			{
				// stop the timer
				// Ÿ�̸� ����
				this.timer.shutdown();
				this.timer.awaitTermination(33, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e)
			{
				// log any exception
				// ��� ���� ���
				System.err.println("Exception in stopping the frame capture, trying to release the camera now... " + e);
			}
		}
		
		if (this.capture.isOpened())
		{
			// release the camera
			// ī�޶� �����ϴ�
			this.capture.release();
		}
	}
	
	/**
	 * Update the {@link ImageView} in the JavaFX main thread
	 * JavaFX ���� �������� {@link ImageView} ������Ʈ
	 * @param view
	 *            the {@link ImageView} to update
	 * @param view�� {@link ImageView} ������Ʈ
	 * @param image
	 *            the {@link Image} to show
	 * @param image�� {@link Image} ǥ��
	 */
	private void updateImageView(ImageView view, Image image)
	{
		Utils.onFXThread(view.imageProperty(), image);
	}
	
	/**
	 * On application close, stop the acquisition from the camera
	 * ���ø����̼��� ���� �� ī�޶󿡼� ������ �����մϴ�.
	 */
	protected void setClosed()
	{
		this.stopAcquisition();
	}
	
}