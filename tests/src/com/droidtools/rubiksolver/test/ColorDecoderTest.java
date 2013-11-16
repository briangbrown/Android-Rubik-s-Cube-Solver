package com.droidtools.rubiksolver.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.test.AndroidTestCase;

import com.droidtools.rubiksolver.ColorDecoder;
import com.droidtools.rubiksolver.HColor;

public class ColorDecoderTest extends AndroidTestCase {

	public ColorDecoderTest() {
		super();
	}
	
	/**
	 * Tests that the HColor Average method works correctly.
	 * TODO: move this into HColor.
	 */
	public void testAvg() {
		List<HColor> colors = new ArrayList<HColor>();
		colors.add(new HColor(255, 0, 0));
		colors.add(new HColor(0, 255, 0));
		colors.add(new HColor(0, 0, 255));
		
		HColor avgColor = ColorDecoder.avg(colors);
		assertEquals(85, avgColor.r);
		assertEquals(85, avgColor.g);
		assertEquals(85, avgColor.b);
	}
	
	
	public void testSyntheticRowThreeColorDecode() {
		byte[][] colorIndex = new byte[][] {
				{0, 0, 0}, // [0][0], [0][1], [0][2]
				{1, 1, 1}, // [1][0], [1][1], [1][2]
				{2, 2, 2}  // [2][0], [2][1], [2][2]
		};
		assertSyntheticCubeDecode(colorIndex);
	}
	
	public void testSyntheticColumnThreeColorDecode() {
		byte[][] colorIndex = new byte[][] {
				{0, 1, 2}, // [0][0], [0][1], [0][2]
				{0, 1, 2}, // [1][0], [1][1], [1][2]
				{0, 1, 2}  // [2][0], [2][1], [2][2]
		};
		assertSyntheticCubeDecode(colorIndex);
	}
	
	public void testSyntheticSixColorDecode() {
		byte[][] colorIndex = new byte[][] {
				{0, 1, 2}, // [0][0], [0][1], [0][2]
				{3, 4, 5}, // [1][0], [1][1], [1][2]
				{2, 1, 0}  // [2][0], [2][1], [2][2]
		};
		assertSyntheticCubeDecode(colorIndex);
	}

	/**
	 * Tests a synthetically created cube with the decode method making sure the right number of
	 * colors and the correct colors are decoded.
	 */
	private void assertSyntheticCubeDecode(byte[][] colorIndex) {
		Set<Byte> uniqueInputIndex = new TreeSet<Byte>();
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
			    uniqueInputIndex.add(colorIndex[row][col]);
			}
		}
		
		ColorDecoder decoder = new ColorDecoder(getContext().getCacheDir().getAbsolutePath());
		byte[] ids = decoder.decode(getTestCubeBitmap(colorIndex));
		
		// There should always be 9 cubelets.
		assertEquals(9, ids.length);
		
		Set<Byte> uniqueIds = new TreeSet<Byte>();
		for (int i = 0; i < ids.length; i++) {
			uniqueIds.add(ids[i]);
		}
		
		// Number of different colors decoded.
		assertEquals(uniqueInputIndex.size(), uniqueIds.size());
		
		// Verify each color
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				// Note that ids is in Col-Major order starting in the bottom left.
				// 2 5 8
				// 1 4 7
				// 0 3 6
				HColor decodedColor = decoder.getColor(ids[(col + 1) * 3 - (row + 1)]);
				HColor expectedColor = new HColor(getColorFromIndex(colorIndex[row][col]));
				assertEquals(0, expectedColor.distance(decodedColor), 0.0000001);
			}	
		}
	}
	

	/**
	 * Creates a bitmap with a Rubik's Cube face drawn at the center using the specified color
	 * index.
	 *                   width
	 *  /--------------------------------------\
	 *  |     /--------------------------\     |
	 *  |     |        |        |        |     |
	 *  |     | [0][0] | [0][1] | [0][2] |     |
	 *  |     |        |        |        |     |
	 *  |     |--------------------------|     |
	 *  |     |        |        |        |     |
	 *  |     | [1][0] | [1][1] | [1][2] |     | height
	 *  |     |        |        |        |     |
	 *  |     |--------------------------|     |
	 *  |     |        |        |        |     |
	 *  |     | [2][0] | [2][1] | [2][2] |     |
	 *  |     |        |        |        |     |
	 *  |     \--------------------------/     |
	 *  \--------------------------------------/
	 *  
	 * @param colorIndex array of color indicies indexed by row then column; 0 - 5 index values 
	 * @return bitmap of a Rubik's Cube face
	 */
	private Bitmap getTestCubeBitmap(byte[][] colorIndex) {
		final int width = 200;
		final int height = 120;
		Bitmap testCubeImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(testCubeImage);
		Paint paint = new Paint();
		
		// From the decode method
		int margin = (int) (Math.min(width, height) * .1);
		int sideLength = Math.min(width, height) - margin;
		float leftCube = (width - sideLength) / 2.0f;
		float rightCube = leftCube + sideLength;
		float topCube = (height - sideLength) / 2.0f;
		float bottomCube = topCube + sideLength;
		float cubeletLength = sideLength / 3.0f;
		float cubeletMargin = cubeletLength * .05f;
		
		assertFalse("Cube too small to get at least one pixel of cubelet margin.",
				cubeletMargin < 1.0f);
		
		// Draw the background
		testCubeImage.eraseColor(Color.WHITE);
		
		// Draw base of cube as black
		paint.setColor(Color.BLACK);
		canvas.drawRect(leftCube, topCube, rightCube, bottomCube, paint);
		
		float leftCubelet;
		float rightCubelet;
		float topCubelet;
		float bottomCubelet;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				leftCubelet = leftCube + cubeletMargin + (col * cubeletLength);
				rightCubelet = leftCube - cubeletMargin + ((col + 1) * cubeletLength);
				topCubelet = topCube + cubeletMargin + (row * cubeletLength);
				bottomCubelet = topCube - cubeletMargin + ((row + 1) * cubeletLength);
				
				paint.setColor(getColorFromIndex(colorIndex[row][col]));
				
				canvas.drawRect(leftCubelet, topCubelet, rightCubelet, bottomCubelet, paint);
			}	
		}
		
		// Save out the cube face image for visual inspection.
		saveToInternalFile(testCubeImage, "test_face.png");
		return testCubeImage;
	}
	
	/**
	 * Gets a color int from an index of 6 cubelet colors.
	 * @param index of a cubelet color between 0 and 5
	 * @return a cublet color int
	 */
	private int getColorFromIndex(byte index) {
		final int[] cubletColors = new int[] {
		    Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.WHITE, Color.MAGENTA
		};
		
		assertTrue(index < 6);
		assertTrue(index >= 0);
		
		return cubletColors[index];
	}
	
	/**
	 * Saves an image as a png with the given fileName. The file will be saved to
	 * /data/data/com.droidtools.rubiksolver/files/<fileName>
	 * This can be found in the emulator or hardware device when debugging.
	 * @param image to save
	 * @param fileName name of the file
	 */
	private void saveToInternalFile(Bitmap image, String fileName) {
	    try {
	        File file = new File(getContext().getFilesDir(), fileName);
	        FileOutputStream fos = new FileOutputStream(file);
	        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
	        fos.close();
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
}
