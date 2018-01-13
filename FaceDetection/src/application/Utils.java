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
 * OpenCV-JavaFX ������ ��ȯ�� ó���ϱ����� ���� �޼ҵ带 �����մϴ�.
 * Moreover, expose some "low level" methods for matching few JavaFX behavior.
 * ���� JavaFX ������ ���� ��ġ��Ű�� �ʴ� "������"�޼ҵ带 �����մϴ�.
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
	 * �ش� Image for JavaFX���� Mat ��ü (OpenCV)�� ��ȯ�մϴ�.
	 *
	 * @param frame
	 *            the {@link Mat} representing the current frame
	 * @param frame��  ���� �������� ��Ÿ���� {@link Mat}
	 * @return the {@link Image} to show
	 * @return�� {@link Image} ǥ��
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
	 * *JavaFX �����忡�� �� JavaFX �����忡 ���� ���� ��Ҹ� �ְ� UI�� �����ϰ� ������Ʈ�ϴ� �Ϲ����� ���
	 * 
	 * @param property
	 *            a {@link ObjectProperty}
	 *  @param property�� {@��ũ ��ü �Ӽ�}   
	 * @param value
	 *            the value to set for the given {@link ObjectProperty}
	 * @param value�� ������ ObjectProperty�� �����ϴ� �� {@link ObjectProperty}
	 */
	public static <T> void onFXThread(final ObjectProperty<T> property, final T value)
	{
		Platform.runLater(() -> {
			property.set(value);
		});
	}
	
	/**
	 * Support for the {@link mat2image()} method
	 * {@link mat2image ()} �޼ҵ� ����
	 * 
	 * @param original
	 *            the {@link Mat} object in BGR or grayscale
	 * @param original�� BGR �Ǵ� �׷��� �������� Matrix ������Ʈ {@link Mat}
	 * @return the corresponding {@link BufferedImage}
	 * @return�� �����ϴ� {@link BufferedImage}
	 */
	private static BufferedImage matToBufferedImage(Mat original)
	{
		// init
		// �ʱ�ȭ
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