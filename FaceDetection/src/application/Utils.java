package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

/**
 * Provide general purpose methods for handling OpenCV-JavaFX data conversion.
 * OpenCV-JavaFX 데이터 변환을 처리하기위한 범용 메소드를 제공합니다.
 * Moreover, expose some "low level" methods for matching few JavaFX behavior.
 * 또한 JavaFX 동작을 거의 일치시키지 않는 "저수준"메소드를 노출합니다.
 *
 * @author <a href="mailto:luigi.derussis@polito.it">Luigi De Russis</a>
 * @author <a href="http://max-z.de">Maximilian Zuleger</a>
 * @version 1.0 (2016-09-17)
 * @since 1.0
 * 
 */
public final class Utils
{
	/**
	 * Convert a Mat object (OpenCV) in the corresponding Image for JavaFX
	 * 해당 Image for JavaFX에서 Mat 객체 (OpenCV)를 변환합니다.
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @param frame은  현재 프레임을 나타내는 {@link Mat}
	 * @return the {@link Image} to show
	 * @return은 {@link Image} 표시
	 */
	public static Image mat2Image(Mat frame)
	{
		try
		{
			return SwingFXUtils.toFXImage(matToBufferedImage(frame), null);
		}
		catch (Exception e)
		{
			System.err.println("Cannot convert the Mat object: " + e);
			return null;
		}
	}
	
	/**
	 * Generic method for putting element running on a non-JavaFX thread on the
	 * JavaFX thread, to properly update the UI
	 * *JavaFX 스레드에서 비 JavaFX 스레드에 실행 중인 요소를 넣고 UI를 적절하게 업데이트하는 일반적인 방법
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 *  @param property는 {@링크 객체 속성}   
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 * @param value는 지정된 ObjectProperty에 설정하는 값 {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	/**
	 * Support for the {@link mat2image()} method
	 * {@link mat2image ()} 메소드 지원
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @param original은 BGR 또는 그레이 스케일의 Matrix 오브젝트 {@link Mat}
	 * @return the corresponding {@link BufferedImage}
	 * @return은 대응하는 {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		// 초기화
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
}