package com.droidtools.rubiksolver.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.test.InstrumentationTestCase;
import android.util.Log;

import com.droidtools.rubiksolver.ColorDecoder;
import com.droidtools.rubiksolver.HColor;

public class ColorDecoderTest extends InstrumentationTestCase {
	
	private final String TAG = ColorDecoderTest.class.getName();
    private Context testContext;
    private Context targetContext;
	
	public ColorDecoderTest() {
		super();
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.testContext = getInstrumentation().getContext();
		this.targetContext = getInstrumentation().getTargetContext();
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
	
	// TODO: Currently failing because we incorrectly decode the cubelet face with the Rubik's
	// cube logo.
	public void testImageBasedSixColorWithLogoDecode() {
		byte[][] colorIndex = new byte[][] {
				{1, 4, 2}, // [0][0], [0][1], [0][2]
				{1, 3, 3}, // [1][0], [1][1], [1][2]
				{5, 1, 0}  // [2][0], [2][1], [2][2]
		};
		assertImageBasedCubeDecode(colorIndex, "cube_face_600x450_6colors.png");
	}
	
	/**
	 * Tests an image captured cube with the decode method making sure the right number of
	 * colors and the correct colors are decoded.
	 */
	private void assertImageBasedCubeDecode(byte[][] colorIndex, String fileName) {
		Set<Byte> uniqueInputIndex = new TreeSet<Byte>();
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
			    uniqueInputIndex.add(colorIndex[row][col]);
			}
		}
		
		ColorDecoder decoder = new ColorDecoder(targetContext.getCacheDir().getAbsolutePath(),
				targetContext.getExternalFilesDir(null).getAbsolutePath());
		byte[] ids = decoder.decode(readTestImage(fileName));
		
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
		
		ColorDecoder decoder = new ColorDecoder(targetContext.getCacheDir().getAbsolutePath(),
				targetContext.getExternalFilesDir(null).getAbsolutePath());
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
		saveToExternalFile(testCubeImage, "test_face.png");
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
	 * <externalDir>/<fileName>
	 * This can be found in the emulator or hardware device when debugging.
	 * @param image to save
	 * @param fileName name of the file
	 */
	private void saveToExternalFile(Bitmap image, String fileName) {
	    try {
	        File file = new File(testContext.getExternalFilesDir(null), fileName);
	        FileOutputStream fos = new FileOutputStream(file);
	        image.compress(Bitmap.CompressFormat.PNG, 90, fos);
	        fos.close();
	    } catch (FileNotFoundException e) {
	    	Log.e(TAG, String.format("Failed to save %s to external media", fileName), e);
	    } catch (IOException e) {
	    	Log.e(TAG, String.format("Failed to save %s to external media", fileName), e);
	    }
	}
	
	/**
	 * Read a test image from the assets folder in the test.
	 * @param fileName of the test image
	 * @return a bitmap of the loaded image
	 */
	private Bitmap readTestImage(String fileName) {
		try {
			InputStream is = testContext.getAssets().open("images/" + fileName);
			return BitmapFactory.decodeStream(is);
		} catch (IOException e) {
			Log.e(TAG, String.format("Failed to open %s from assets", fileName), e);
		}
		return null;
	}
}
